package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.util.ProyectoDefinicion;
import unmsm.edu.pe.tesis.application.dto.ObservarItemRequest;
import unmsm.edu.pe.tesis.application.dto.ProyectoBandejaItem;
import unmsm.edu.pe.tesis.application.dto.ProyectoEditorResponse;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevision;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisionEvento;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;
import unmsm.edu.pe.tesis.domain.enums.EstadoProyecto;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.AsesorProyectoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AsesorProyectoServiceImpl implements AsesorProyectoService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoRevisionRepository revisionRepository;
    @Inject ProyectoRevisionEventoRepository eventoRepository;
    @Inject ProyectoEditorAssembler assembler;
    @Inject AsesorDesignadoService asesorDesignado;

    private static final java.util.Set<EstadoItemRevision> PENDIENTES = java.util.Set.of(
            EstadoItemRevision.OBSERVADO, EstadoItemRevision.EN_CORRECCION, EstadoItemRevision.CORREGIDO);

    @Override
    @Transactional
    public PageResponse<ProyectoBandejaItem> bandeja(String estado, String buscar, int page, int size) {
        Docente asesor = docenteActual();
        List<ProyectoBandejaItem> content = proyectoRepository
                .bandejaDeAsesor(asesor.getPersonaId(), estado, buscar, page, size)
                .stream().map(this::toBandejaItem).collect(Collectors.toList());
        long total = proyectoRepository.contarBandejaDeAsesor(asesor.getPersonaId(), estado, buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public ProyectoEditorResponse detalle(UUID tesisId) {
        Docente asesor = docenteActual();
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("El proyecto aún no ha sido iniciado por el estudiante"));
        // El co-asesor también abre el detalle, pero en modo consulta.
        boolean coasesor = esCoasesor(tesisId, asesor);
        if (!coasesor) {
            verificarAsesor(tesisId, asesor);
            asesorDesignado.sincronizar(p);
        }
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        ProyectoEditorResponse editor =
                assembler.armar(p, tesis, estudianteDe(tesisId), nombre(asesor.getPersona()), "ASESOR");
        editor.setSoloLectura(coasesor);
        return editor;
    }

    @Override
    @Transactional
    public void observarItem(UUID tesisId, ObservarItemRequest req) {
        Docente asesor = docenteActual();
        ProyectoTesis p = proyecto(tesisId, asesor);
        verificarRevisionAbierta(p);
        ProyectoRevision rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), req.getCampo())
                .orElseGet(() -> ProyectoRevision.builder().proyectoId(p.getId()).campo(req.getCampo()).build());
        rev.setEstado(EstadoItemRevision.OBSERVADO);
        rev = revisionRepository.save(rev);
        registrarEvento(rev.getId(), "OBSERVACIÓN", nombre(asesor.getPersona()), "ASESOR", req.getTexto().trim());
        // Un proyecto observado por el asesor ya fue enviado a revisión: se reafirma el hito
        // para que el estudiante pueda reenviarlo tras corregir (invariante observado ⟹ enviado).
        p.setListoRevision(true);
        if (p.getFechaListoRevision() == null) {
            p.setFechaListoRevision(LocalDate.now());
        }
        p.setEstado(EstadoProyecto.OBSERVADO);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void darConformidad(UUID tesisId, String campo) {
        Docente asesor = docenteActual();
        ProyectoTesis p = proyecto(tesisId, asesor);
        verificarRevisionAbierta(p);
        // El asesor puede dar conformidad a cualquier ítem (aprobarlo directo o cerrar una corrección);
        // si aún no tenía revisión, se crea.
        ProyectoRevision rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), campo)
                .orElseGet(() -> ProyectoRevision.builder().proyectoId(p.getId()).campo(campo).build());
        if (rev.getEstado() == EstadoItemRevision.CONFORME) {
            return; // ya está conforme
        }
        rev.setEstado(EstadoItemRevision.CONFORME);
        rev = revisionRepository.save(rev);
        registrarEvento(rev.getId(), "CONFORMIDAD", nombre(asesor.getPersona()), "ASESOR",
                "El asesor da conformidad al ítem.");
        // Si ya no quedan ítems pendientes, el proyecto queda conforme.
        boolean quedanPendientes = revisionRepository.listarPorProyecto(p.getId()).stream()
                .anyMatch(r -> PENDIENTES.contains(r.getEstado()));
        if (!quedanPendientes) {
            p.setEstado(EstadoProyecto.CONFORME);
            proyectoRepository.save(p);
        }
    }

    @Override
    @Transactional
    public void darConformidadSeccion(UUID tesisId, List<String> campos) {
        Docente asesor = docenteActual();
        ProyectoTesis p = proyecto(tesisId, asesor);
        verificarRevisionAbierta(p);
        if (campos == null || campos.isEmpty()) return;
        for (String campo : campos) {
            ProyectoRevision rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), campo)
                    .orElseGet(() -> ProyectoRevision.builder().proyectoId(p.getId()).campo(campo).build());
            EstadoItemRevision st = rev.getEstado();
            // No pisar una observación viva (el estudiante aún debe corregirla); lo demás se aprueba.
            if (st == EstadoItemRevision.CONFORME
                    || st == EstadoItemRevision.OBSERVADO
                    || st == EstadoItemRevision.EN_CORRECCION) {
                continue;
            }
            rev.setEstado(EstadoItemRevision.CONFORME);
            rev = revisionRepository.save(rev);
            registrarEvento(rev.getId(), "CONFORMIDAD", nombre(asesor.getPersona()), "ASESOR",
                    "El asesor da conformidad al ítem.");
        }
        boolean quedanPendientes = revisionRepository.listarPorProyecto(p.getId()).stream()
                .anyMatch(r -> PENDIENTES.contains(r.getEstado()));
        if (!quedanPendientes) {
            p.setEstado(EstadoProyecto.CONFORME);
            proyectoRepository.save(p);
        }
    }

    @Override
    @Transactional
    public void emitirCartaOpinion(UUID tesisId) {
        Docente asesor = docenteActual();
        ProyectoTesis p = proyecto(tesisId, asesor);
        if (!Boolean.TRUE.equals(p.getListoRevision())) {
            throw new BusinessException("El estudiante aún no ha enviado el proyecto a revisión");
        }
        // Debe estar CONFORME cada ítem revisable del enfoque (campos + plan), no solo "sin observaciones".
        Map<String, EstadoItemRevision> estados = revisionRepository.listarPorProyecto(p.getId()).stream()
                .collect(Collectors.toMap(ProyectoRevision::getCampo, ProyectoRevision::getEstado, (a, b) -> b));
        boolean todosConformes = ProyectoDefinicion.itemsRevisables(p.getEnfoque()).stream()
                .allMatch(k -> estados.get(k) == EstadoItemRevision.CONFORME);
        if (!todosConformes) {
            throw new BusinessException("Aún hay ítems sin conformidad del asesor");
        }
        p.setCartaAsesor(true);
        p.setFechaCartaAsesor(LocalDate.now());
        p.setEstado(EstadoProyecto.APROBADO);
        proyectoRepository.save(p);
    }

    // ── helpers ──
    /**
     * Una vez emitida la carta de opinión favorable, la revisión queda cerrada: no se puede observar ni dar
     * conformidad. Además, si el proyecto ya avanzó a etapas posteriores (revisores/defensa/informe final),
     * el asesor queda bloqueado igualmente: el flujo es hacia adelante, sin retroceder pasos.
     */
    private void verificarRevisionAbierta(ProyectoTesis p) {
        if (Boolean.TRUE.equals(p.getCartaAsesor())) {
            throw new BusinessException("La carta de opinión favorable ya fue emitida; la revisión está cerrada");
        }
        if (Boolean.TRUE.equals(p.getRevisoresConformes())
                || Boolean.TRUE.equals(p.getDefensaProgramada())
                || Boolean.TRUE.equals(p.getInformeFinalAprobado())
                || Boolean.TRUE.equals(p.getInformeFinalRevisado())) {
            throw new BusinessException("El proyecto ya avanzó a una etapa posterior; la revisión del asesor está cerrada.");
        }
    }

    private ProyectoTesis proyecto(UUID tesisId, Docente asesor) {
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("El proyecto no existe"));
        verificarAsesor(tesisId, asesor);
        asesorDesignado.sincronizar(p); // repara la copia asesor_id si el proyecto nació sin asesor
        return p;
    }

    /**
     * Solo el asesor principal actúa sobre el proyecto. El co-asesor no pasa por aquí: sus
     * accesos de escritura (observar, dar conformidad, carta) quedan bloqueados con este mensaje.
     */
    private void verificarAsesor(UUID tesisId, Docente asesor) {
        UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(a -> a.getDocenteId()).orElse(null);
        if (asesorId == null || !asesorId.equals(asesor.getPersonaId())) {
            throw new BusinessException(esCoasesor(tesisId, asesor)
                    ? "Eres co-asesor de esta tesis: tu acceso es de consulta; la revisión la realiza el asesor"
                    : "No eres el asesor de esta tesis");
        }
    }

    /** ¿El docente es el co-asesor de esta tesis? */
    private boolean esCoasesor(UUID tesisId, Docente docente) {
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR")
                .map(a -> docente.getPersonaId().equals(a.getDocenteId()))
                .orElse(false);
    }

    private unmsm.edu.pe.personas.domain.entities.Estudiante estudianteDe(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private ProyectoBandejaItem toBandejaItem(Object[] r) {
        return ProyectoBandejaItem.builder()
                .tesisId((UUID) r[0])
                .proyectoId((UUID) r[1])
                .estado(asStr(r[2]))
                .estudianteApellidos((asStr(r[3]) + " " + asStr(r[4])).trim())
                .estudianteNombres(asStr(r[5]))
                .codigoSistema(asStr(r[6]))
                .programaNombre(asStr(r[7]))
                .tituloTesis(asStr(r[8]))
                .build();
    }

    private void registrarEvento(UUID revisionId, String tipo, String autor, String rol, String texto) {
        eventoRepository.save(ProyectoRevisionEvento.builder()
                .revisionId(revisionId).tipo(tipo).autor(autor).rol(rol).texto(texto)
                .fechaEvento(LocalDateTime.now()).build());
    }

    private Docente docenteActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        Persona persona = personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
        return docenteRepository.findByPersonaId(persona.getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de docente"));
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
