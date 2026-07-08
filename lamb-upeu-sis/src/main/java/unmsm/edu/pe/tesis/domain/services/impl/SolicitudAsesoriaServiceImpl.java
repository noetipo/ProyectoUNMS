package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.CrearSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.DocenteComboItem;
import unmsm.edu.pe.tesis.application.dto.ResponderSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.SolicitudBandejaItem;
import unmsm.edu.pe.tesis.application.dto.SolicitudMiaItem;
import unmsm.edu.pe.tesis.application.dto.SolicitudResponse;
import unmsm.edu.pe.tesis.application.mapper.SolicitudAsesoriaMapper;
import unmsm.edu.pe.tesis.domain.services.SolicitudAsesoriaService;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.enums.DecisionSolicitud;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SolicitudAsesoriaServiceImpl implements SolicitudAsesoriaService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject LineaInvestigacionRepository lineaInvestigacionRepository;
    @Inject DocenteLineaInvestigacionRepository docenteLineaRepository;
    @Inject SolicitudAsesoriaRepository solicitudRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject SolicitudAsesoriaMapper mapper;
    @Inject unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService resolverPlantilla;
    @Inject com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public SolicitudResponse crear(CrearSolicitudRequest request) {
        Estudiante estudiante = estudianteActual();

        Docente docente = docenteRepository.findByPersonaId(request.getDocenteId())
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .orElseThrow(() -> new BusinessException("Docente no encontrado o inactivo: " + request.getDocenteId()));

        LineaInvestigacion linea = lineaInvestigacionRepository.buscarPorId(request.getLineaInvestigacionId())
                .filter(l -> Boolean.TRUE.equals(l.getActive()))
                .orElseThrow(() -> new BusinessException("Línea de investigación no encontrada o inactiva"));

        if (!docenteLineaRepository.existsByDocenteAndLinea(docente.getPersonaId(), linea.getId())) {
            throw new BusinessException("El docente no tiene registrada esa línea de investigación");
        }

        if (solicitudRepository.existePendientePorEstudiante(estudiante.getPersonaId())) {
            throw new BusinessException("Ya tienes una solicitud pendiente; espera la respuesta o cancélala antes de solicitar a otro docente");
        }

        SolicitudAsesoria solicitud = SolicitudAsesoria.builder()
                .estudiante(estudiante)
                .docente(docente)
                .lineaInvestigacion(linea)
                .tituloTentativo(trimToNull(request.getTituloTentativo()))
                .mensaje(trimToNull(request.getMensaje()))
                .tipo(request.getTipo() != null ? request.getTipo() : TipoAsesoria.ASESOR)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        // Snapshot de la Solicitud (marcadores resueltos con la config vigente) → evidencia inmutable.
        solicitud.setDatosSolicitud(snapshot(resolverPlantilla.resolverSolicitud(solicitud, LocalDate.now())));

        return mapper.toResponse(solicitudRepository.save(solicitud));
    }

    /** Serializa el mapa de marcadores a JSON para persistirlo como snapshot. */
    private String snapshot(java.util.Map<String, String> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception ex) {
            throw new BusinessException("No se pudo generar el documento de asesoría: " + ex.getMessage());
        }
    }

    @Override
    public PageResponse<SolicitudBandejaItem> bandeja(String estado, int page, int size) {
        Docente docente = docenteActual();
        EstadoSolicitud est = parseEstado(estado != null && !estado.isBlank() ? estado : "PENDIENTE");
        List<SolicitudBandejaItem> content = solicitudRepository
                .listarPorDocente(docente.getPersonaId(), est, page, size).stream()
                .map(mapper::toBandejaItem).collect(Collectors.toList());
        long total = solicitudRepository.contarPorDocente(docente.getPersonaId(), est);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public SolicitudResponse responder(UUID id, ResponderSolicitudRequest request) {
        Docente docente = docenteActual();
        SolicitudAsesoria solicitud = solicitudRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada: " + id));

        if (solicitud.getDocente() == null
                || !docente.getPersonaId().equals(solicitud.getDocente().getPersonaId())) {
            throw new BusinessException("No puedes responder una solicitud que no te corresponde");
        }
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("La solicitud ya fue respondida (estado: " + solicitud.getEstado() + ")");
        }

        if (request.getDecision() == DecisionSolicitud.RECHAZAR) {
            if (request.getMotivo() == null || request.getMotivo().isBlank()) {
                throw new ValidationException("El motivo es obligatorio para rechazar la solicitud");
            }
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitud.setMotivoRespuesta(request.getMotivo().trim());
        } else {
            // ACEPTAR: el docente queda como asesor. Se materializa la asesoría formal
            // (fila en asesorias tipo ASESOR) sobre la tesis activa del estudiante, de modo
            // que el estado derivado pase de SIN_ASESOR a "aceptado".
            solicitud.setEstado(EstadoSolicitud.ACEPTADA);
            materializarAsesoria(solicitud);
            // Snapshot de la Carta de aceptación (evidencia inmutable con la config vigente).
            solicitud.setDatosCarta(snapshot(resolverPlantilla.resolverCarta(solicitud, LocalDate.now())));
        }
        solicitud.setFechaRespuesta(LocalDateTime.now());

        return mapper.toResponse(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional
    public SolicitudResponse cancelar(UUID id) {
        Estudiante estudiante = estudianteActual();
        SolicitudAsesoria solicitud = solicitudRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada: " + id));

        if (solicitud.getEstudiante() == null
                || !estudiante.getPersonaId().equals(solicitud.getEstudiante().getPersonaId())) {
            throw new BusinessException("No puedes cancelar una solicitud que no es tuya");
        }
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BusinessException("Solo puedes cancelar una solicitud PENDIENTE");
        }
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());

        return mapper.toResponse(solicitudRepository.save(solicitud));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Crea la asesoría formal (tipo ASESOR) sobre la tesis activa del estudiante, si aún no tiene asesor. */
    private void materializarAsesoria(SolicitudAsesoria solicitud) {
        UUID estudianteId = solicitud.getEstudiante().getPersonaId();
        UUID tesisId = tesisAutorRepository.tesisActivaId(estudianteId);
        if (tesisId == null) {
            // Sin tema registrado aún: no hay tesis a la cual anclar la asesoría; solo queda ACEPTADA.
            return;
        }
        solicitud.setTesisId(tesisId);
        if (!asesoriaRepository.existeAsesorParaTesis(tesisId)) {
            asesoriaRepository.save(Asesoria.builder()
                    .tesisId(tesisId)
                    .docenteId(solicitud.getDocente().getPersonaId())
                    .tipo("ASESOR")
                    .build());
        }
    }

    private Persona personaActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        return personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
    }

    private Estudiante estudianteActual() {
        return estudianteRepository.findByPersonaId(personaActual().getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de estudiante"));
    }

    private Docente docenteActual() {
        return docenteRepository.findByPersonaId(personaActual().getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de docente"));
    }

    private EstadoSolicitud parseEstado(String estado) {
        try {
            return EstadoSolicitud.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Estado inválido: " + estado);
        }
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
