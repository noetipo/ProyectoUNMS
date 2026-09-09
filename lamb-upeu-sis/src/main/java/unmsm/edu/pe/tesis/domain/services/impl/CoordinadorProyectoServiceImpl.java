package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.CoordinadorProyectoService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CoordinadorProyectoServiceImpl implements CoordinadorProyectoService {

    @Inject SecurityUtils securityUtils;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject TesisRepository tesisRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository informeRevisorRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.JuradoSustentacionRepository juradoSustentacionRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject PersonaRepository personaRepository;
    @Inject AsesorRolService asesorRolService;

    @Override
    @Transactional
    public PageResponse<DefensaBandejaItem> bandeja(String buscar, int page, int size) {
        guard();
        List<DefensaBandejaItem> content = proyectoRepository.bandejaDefensa(buscar, page, size)
                .stream().map(this::toBandeja).collect(Collectors.toList());
        long total = proyectoRepository.contarBandejaDefensa(buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public List<DocenteOpcion> docentesDisponibles(UUID tesisId) {
        guard();
        // Solo docentes que llevan la línea de investigación del tema de la tesis.
        UUID lineaId = tesisRepository.buscarPorId(tesisId)
                .map(Tesis::getLineaInvestigacion)
                .map(LineaInvestigacion::getId)
                .orElse(null);
        // El asesor del proyecto no puede ser revisor: se excluye de la lista.
        UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(Asesoria::getDocenteId).orElse(null);
        // Estricto: SOLO docentes de la línea de investigación de la tesis. Sin línea → sin candidatos.
        List<Object[]> filas = lineaId != null
                ? revisorRepository.docentesOpcionPorLinea(lineaId)
                : List.of();
        return filas.stream()
                .filter(r -> asesorId == null || !asesorId.equals(r[0]))
                .map(r -> DocenteOpcion.builder()
                        .id((UUID) r[0])
                        .nombre(nombre(asStr(r[1]), asStr(r[2]), asStr(r[3])))
                        .categoria(r[4] != null ? r[4].toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RevisorItem> revisores(UUID tesisId) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        return revisorRepository.listarPorProyecto(p.getId()).stream()
                .map(this::toRevisor).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void designarRevisores(UUID tesisId, DesignarRevisoresRequest req) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getExpedienteRecibido())) {
            throw new BusinessException("El expediente aún no ha sido recepcionado por Secretaría");
        }
        if (revisorRepository.contarPorProyecto(p.getId()) > 0) {
            throw new BusinessException("Este proyecto ya tiene revisores designados");
        }
        List<UUID> ids = req != null && req.getDocenteIds() != null
                ? new ArrayList<>(new LinkedHashSet<>(req.getDocenteIds())) : List.of();
        if (ids.size() != 2) {
            throw new ValidationException("Debes designar exactamente 2 revisores (distintos)");
        }
        UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(Asesoria::getDocenteId).orElse(null);
        int orden = 1;
        for (UUID docenteId : ids) {
            if (docenteRepository.findByPersonaId(docenteId).isEmpty()) {
                throw new ValidationException("Uno de los docentes seleccionados no existe");
            }
            if (docenteId.equals(asesorId)) {
                throw new BusinessException("El asesor del proyecto no puede ser revisor");
            }
            revisorRepository.save(ProyectoRevisor.builder()
                    .proyectoId(p.getId()).docenteId(docenteId).orden(orden++)
                    .estado(EstadoRevisor.DESIGNADO).build());
            // Igual que con el asesor: el docente pasa a ser revisor recién cuando se le designa.
            asesorRolService.otorgarRolRevisor(docenteId);
        }
    }

    @Override
    @Transactional
    public DefensaInfo defensa(UUID tesisId) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        // Quiénes evalúan la defensa: los mismos dos revisores del proyecto (el diagrama del
        // proceso no contempla un jurado aparte para este paso — eso es de la Sustentación
        // final, Etapa 8). Se programa desde Secretaría; aquí solo se muestra en modo lectura.
        List<JuradoItem> jurado = revisorRepository.listarPorProyecto(p.getId()).stream()
                .sorted(java.util.Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> {
                    Persona pe = personaRepository.buscarPorId(rv.getDocenteId()).orElse(null);
                    return JuradoItem.builder()
                            .docenteId(rv.getDocenteId())
                            .docenteNombre(pe != null ? nombre(pe.getNombres(), pe.getApellidoPaterno(), pe.getApellidoMaterno()) : null)
                            .rol("REVISOR")
                            .orden(rv.getOrden())
                            .build();
                })
                .collect(Collectors.toList());
        return DefensaInfo.builder()
                .programada(Boolean.TRUE.equals(p.getDefensaProgramada()))
                .fecha(p.getFechaDefensa()).hora(p.getHoraDefensa()).lugar(p.getLugarDefensa())
                .modalidad(p.getModalidadDefensa() != null ? p.getModalidadDefensa().name() : null)
                .modalidadLabel(p.getModalidadDefensa() != null ? p.getModalidadDefensa().etiqueta() : null)
                .enlace(p.getEnlaceDefensa())
                .dictamenNumero(p.getDictamenNumero())
                .jurado(jurado)
                .build();
    }

    @Override
    @Transactional
    public List<unmsm.edu.pe.tesis.application.dto.InformeRevisorItem> juradoInforme(UUID tesisId) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        return informeRevisorRepository.listarPorProyecto(p.getId()).stream()
                .map(rv -> {
                    Persona pe = personaRepository.buscarPorId(rv.getDocenteId()).orElse(null);
                    return unmsm.edu.pe.tesis.application.dto.InformeRevisorItem.builder()
                            .id(rv.getId()).docenteId(rv.getDocenteId())
                            .docenteNombre(pe != null ? nombre(pe.getNombres(), pe.getApellidoPaterno(), pe.getApellidoMaterno()) : null)
                            .orden(rv.getOrden()).presidente(Boolean.TRUE.equals(rv.getPresidente()))
                            .estado(rv.getEstado() != null ? rv.getEstado().name() : null)
                            .puntaje(rv.getPuntaje()).comentario(rv.getComentario())
                            .respuesta(rv.getRespuestaEstudiante())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void designarJuradoInforme(UUID tesisId, unmsm.edu.pe.tesis.application.dto.DesignarJuradoInformeRequest req) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getJuradoInformanteSolicitado())) {
            throw new BusinessException("El estudiante aún no ha solicitado el Jurado Informante");
        }
        if (!Boolean.TRUE.equals(p.getExpedienteInformeRecibido())) {
            throw new BusinessException("Secretaría aún no recepciona el expediente del Jurado Informante");
        }
        if (informeRevisorRepository.contarPorProyecto(p.getId()) > 0) {
            throw new BusinessException("El Jurado Informante ya fue designado");
        }
        List<UUID> ids = req != null && req.getDocenteIds() != null
                ? new ArrayList<>(new LinkedHashSet<>(req.getDocenteIds())) : List.of();
        if (ids.size() != 3) {
            throw new ValidationException("Debes designar exactamente 3 miembros (distintos)");
        }
        UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(Asesoria::getDocenteId).orElse(null);
        int orden = 1;
        for (UUID docenteId : ids) {
            if (docenteRepository.findByPersonaId(docenteId).isEmpty()) {
                throw new ValidationException("Uno de los docentes seleccionados no existe");
            }
            if (docenteId.equals(asesorId)) {
                throw new BusinessException("El asesor no puede integrar el Jurado Informante");
            }
            informeRevisorRepository.save(unmsm.edu.pe.tesis.domain.entities.InformeRevisor.builder()
                    .proyectoId(p.getId()).docenteId(docenteId).orden(orden)
                    .presidente(orden == 1) // el primero es Presidente
                    .estado(EstadoRevisor.DESIGNADO).build());
            orden++;
        }
    }

    @Override
    @Transactional
    public List<unmsm.edu.pe.tesis.application.dto.JuradoItem> juradoSustentacion(UUID tesisId) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        return juradoSustentacionRepository.listarPorProyecto(p.getId()).stream()
                .sorted(java.util.Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> {
                    Persona pe = personaRepository.buscarPorId(rv.getDocenteId()).orElse(null);
                    return unmsm.edu.pe.tesis.application.dto.JuradoItem.builder()
                            .docenteId(rv.getDocenteId())
                            .docenteNombre(pe != null ? nombre(pe.getNombres(), pe.getApellidoPaterno(), pe.getApellidoMaterno()) : null)
                            .rol(Boolean.TRUE.equals(rv.getPresidente()) ? "PRESIDENTE" : "MIEMBRO")
                            .orden(rv.getOrden())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void designarJuradoSustentacion(UUID tesisId, unmsm.edu.pe.tesis.application.dto.DesignarJuradoInformeRequest req) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido())) {
            throw new BusinessException("Secretaría aún no recepciona el expediente de sustentación");
        }
        if (juradoSustentacionRepository.contarPorProyecto(p.getId()) > 0) {
            throw new BusinessException("El Jurado de Sustentación ya fue designado");
        }
        List<UUID> ids = req != null && req.getDocenteIds() != null
                ? new ArrayList<>(new LinkedHashSet<>(req.getDocenteIds())) : List.of();
        if (ids.size() != 3) {
            throw new ValidationException("Debes designar exactamente 3 miembros (distintos)");
        }
        UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(Asesoria::getDocenteId).orElse(null);
        int orden = 1;
        for (UUID docenteId : ids) {
            if (docenteRepository.findByPersonaId(docenteId).isEmpty()) {
                throw new ValidationException("Uno de los docentes seleccionados no existe");
            }
            if (docenteId.equals(asesorId)) {
                throw new BusinessException("El asesor no puede integrar el Jurado de Sustentación");
            }
            juradoSustentacionRepository.save(unmsm.edu.pe.tesis.domain.entities.JuradoSustentacion.builder()
                    .proyectoId(p.getId()).docenteId(docenteId).orden(orden)
                    .presidente(orden == 1)
                    .build());
            orden++;
        }
    }

    /** N° de dictamen referencial: DICTAMEN N° 000NNN-AAAA-UPG-VDIP-FM/UNMSM. */
    private String generarDictamen(UUID proyectoId, java.time.LocalDate fecha) {
        long correlativo = Math.abs(proyectoId.getLeastSignificantBits() % 1000000);
        int anio = fecha != null ? fecha.getYear() : java.time.LocalDate.now().getYear();
        return String.format("DICTAMEN N° %06d-%d-UPG-VDIP-FM/UNMSM", correlativo, anio);
    }

    // ── helpers ──
    private void guard() {
        securityUtils.requireAnyRole("COORDINADOR", "ADMIN");
    }

    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    private DefensaBandejaItem toBandeja(Object[] r) {
        int num = r[9] != null ? ((Number) r[9]).intValue() : 0;
        UUID proyectoId = (UUID) r[1];
        UUID tesisId = (UUID) r[0];
        var p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
        int numJuradoSustentacion = (int) juradoSustentacionRepository.contarPorProyecto(proyectoId);
        return DefensaBandejaItem.builder()
                .tesisId(tesisId)
                .proyectoId(proyectoId)
                .estudianteApellidos((asStr(r[2]) + " " + asStr(r[3])).trim())
                .estudianteNombres(asStr(r[4]))
                .codigoSistema(asStr(r[5]))
                .programaNombre(asStr(r[6]))
                .tituloTesis(asStr(r[7]))
                .fechaRecepcion(toLocalDate(r[8]))
                .numRevisores(num)
                .revisoresDesignados(num >= 2)
                .revisoresConformes(r.length > 10 && Boolean.TRUE.equals(r[10]))
                .defensaProgramada(r.length > 11 && Boolean.TRUE.equals(r[11]))
                .fechaDefensa(r.length > 12 ? toLocalDate(r[12]) : null)
                .juradoInformanteSolicitado(r.length > 13 && Boolean.TRUE.equals(r[13]))
                .numJuradoInforme(r.length > 14 && r[14] != null ? ((Number) r[14]).intValue() : 0)
                .juradoInformeDesignado(r.length > 14 && r[14] != null && ((Number) r[14]).intValue() >= 3)
                .informeFinalRevisado(r.length > 15 && Boolean.TRUE.equals(r[15]))
                .expedienteSustentacionRecibido(p != null && Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido()))
                .numJuradoSustentacion(numJuradoSustentacion)
                .juradoSustentacionDesignado(numJuradoSustentacion >= 3)
                .build();
    }

    private RevisorItem toRevisor(ProyectoRevisor rv) {
        Persona pe = personaRepository.buscarPorId(rv.getDocenteId()).orElse(null);
        return RevisorItem.builder()
                .id(rv.getId()).docenteId(rv.getDocenteId())
                .docenteNombre(pe != null ? nombre(pe.getNombres(), pe.getApellidoPaterno(), pe.getApellidoMaterno()) : null)
                .orden(rv.getOrden())
                .estado(rv.getEstado() != null ? rv.getEstado().name() : null)
                .comentario(rv.getComentario())
                .build();
    }

    private String nombre(String nombres, String apPat, String apMat) {
        return (nz(nombres) + " " + nz(apPat) + " " + nz(apMat)).trim().replaceAll("\\s+", " ");
    }

    private java.time.LocalDate toLocalDate(Object o) {
        if (o instanceof java.time.LocalDate ld) return ld;
        if (o instanceof java.sql.Date sd) return sd.toLocalDate();
        return null;
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
