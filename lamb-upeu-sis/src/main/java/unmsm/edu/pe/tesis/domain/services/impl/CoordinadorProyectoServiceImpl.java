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
import unmsm.edu.pe.tesis.domain.entities.ProyectoJurado;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.enums.RolJurado;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoJuradoRepository;
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
    @Inject ProyectoJuradoRepository juradoRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository informeRevisorRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject PersonaRepository personaRepository;

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
        }
    }

    @Override
    @Transactional
    public DefensaInfo defensa(UUID tesisId) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        List<JuradoItem> jurado = juradoRepository.listarPorProyecto(p.getId()).stream()
                .map(j -> {
                    Persona pe = personaRepository.buscarPorId(j.getDocenteId()).orElse(null);
                    return JuradoItem.builder()
                            .docenteId(j.getDocenteId())
                            .docenteNombre(pe != null ? nombre(pe.getNombres(), pe.getApellidoPaterno(), pe.getApellidoMaterno()) : null)
                            .rol(j.getRolJurado() != null ? j.getRolJurado().name() : null)
                            .orden(j.getOrden())
                            .build();
                })
                .collect(Collectors.toList());
        return DefensaInfo.builder()
                .programada(Boolean.TRUE.equals(p.getDefensaProgramada()))
                .fecha(p.getFechaDefensa()).hora(p.getHoraDefensa()).lugar(p.getLugarDefensa())
                .dictamenNumero(p.getDictamenNumero())
                .jurado(jurado)
                .build();
    }

    @Override
    @Transactional
    public void programarDefensa(UUID tesisId, ProgramarDefensaRequest req) {
        guard();
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getRevisoresConformes())) {
            throw new BusinessException("Los revisores aún no dieron conformidad al proyecto");
        }
        if (Boolean.TRUE.equals(p.getDefensaProgramada())) {
            throw new BusinessException("La defensa de este proyecto ya fue programada");
        }
        if (req == null || req.getPresidenteId() == null || req.getMiembroIds() == null || req.getFecha() == null) {
            throw new ValidationException("Indica presidente, 2 miembros y la fecha de la defensa");
        }
        // Presidente + 2 miembros, todos distintos.
        List<UUID> miembros = new ArrayList<>(new LinkedHashSet<>(req.getMiembroIds()));
        if (miembros.size() != 2) {
            throw new ValidationException("Debes designar exactamente 2 miembros (distintos)");
        }
        if (miembros.contains(req.getPresidenteId())) {
            throw new ValidationException("El presidente no puede ser también miembro");
        }
        UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(Asesoria::getDocenteId).orElse(null);

        List<UUID> jurado = new ArrayList<>();
        jurado.add(req.getPresidenteId());
        jurado.addAll(miembros);
        for (UUID docenteId : jurado) {
            if (docenteRepository.findByPersonaId(docenteId).isEmpty()) {
                throw new ValidationException("Uno de los docentes del jurado no existe");
            }
        }

        int orden = 1;
        juradoRepository.save(ProyectoJurado.builder()
                .proyectoId(p.getId()).docenteId(req.getPresidenteId()).rolJurado(RolJurado.PRESIDENTE).orden(orden++).build());
        for (UUID m : miembros) {
            juradoRepository.save(ProyectoJurado.builder()
                    .proyectoId(p.getId()).docenteId(m).rolJurado(RolJurado.MIEMBRO).orden(orden++).build());
        }
        if (asesorId != null) {
            juradoRepository.save(ProyectoJurado.builder()
                    .proyectoId(p.getId()).docenteId(asesorId).rolJurado(RolJurado.ASESOR).orden(orden++).build());
        }

        p.setDefensaProgramada(true);
        p.setFechaDefensa(req.getFecha());
        p.setHoraDefensa(req.getHora());
        p.setLugarDefensa(req.getLugar());
        p.setDictamenNumero(generarDictamen(p.getId(), req.getFecha()));
        proyectoRepository.save(p);
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
        return DefensaBandejaItem.builder()
                .tesisId((UUID) r[0])
                .proyectoId((UUID) r[1])
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
