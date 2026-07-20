package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.tesis.application.dto.NotificacionItem;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;
import unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisionRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.NotificacionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NotificacionServiceImpl implements NotificacionService {

    @Inject SecurityUtils securityUtils;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoRevisionRepository revisionRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject InformeRevisorRepository informeRevisorRepository;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject TesisRepository tesisRepository;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String hoy() { return LocalDate.now().format(FECHA); }

    @Override
    @Transactional
    public List<NotificacionItem> paraUsuarioActual() {
        List<NotificacionItem> out = new ArrayList<>();
        List<String> roles = Arrays.asList(securityUtils.getCurrentUserRoles());
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        Persona persona = userId != null ? personaRepository.findByUserId(userId).orElse(null) : null;

        // Secretaría: expedientes con solicitud enviada aún sin recepcionar.
        if (roles.contains("SECRETARIA") || roles.contains("ADMIN")) {
            for (Object[] r : proyectoRepository.bandejaExpedientes(null, 0, 50)) {
                if (Boolean.TRUE.equals(r[8])) {
                    continue; // ya recibido
                }
                UUID tesisId = (UUID) r[0];
                String estudiante = ((asStr(r[1]) + " " + asStr(r[2])).trim() + ", " + asStr(r[3])).trim();
                out.add(NotificacionItem.builder()
                        .id("exp-" + tesisId)
                        .title("Expediente por recepcionar")
                        .description("Recepciona el expediente de " + estudiante + " y comunícalo al Coordinador.")
                        .link("/admin/secretaria-defensa")
                        .icon("inbox")
                        .fecha(fecha(r[7]))
                        .build());
            }
        }

        // Estudiante: observaciones del asesor por corregir.
        if (persona != null && roles.contains("ESTUDIANTE")) {
            notificacionesEstudiante(persona, out);
        }

        // Asesor (docente): correcciones del estudiante por verificar.
        if (persona != null && roles.contains("ASESOR")) {
            notificacionesAsesor(persona, out);
        }

        // Revisor (docente): levantamientos del estudiante por re-evaluar.
        if (persona != null && roles.contains("DOCENTE")) {
            notificacionesRevisor(persona, out);
        }

        return out;
    }

    /** Al estudiante: si su proyecto tiene ítems OBSERVADO/EN_CORRECCION, avisa cuántos debe corregir. */
    private void notificacionesEstudiante(Persona persona, List<NotificacionItem> out) {
        Estudiante est = estudianteRepository.findByPersonaId(persona.getId()).orElse(null);
        if (est == null) return;
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(est.getPersonaId()).orElse(null);
        if (tesis == null) return;
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesis.getId()).orElse(null);
        if (p == null) return;

        long pendientes = revisionRepository.listarPorProyecto(p.getId()).stream()
                .filter(r -> r.getEstado() == EstadoItemRevision.OBSERVADO
                        || r.getEstado() == EstadoItemRevision.EN_CORRECCION)
                .count();
        if (pendientes > 0) {
            out.add(NotificacionItem.builder()
                    .id("obs-" + p.getId())
                    .title("Observaciones por corregir")
                    .description("Tu asesor observó " + pendientes + " ítem(s) de tu proyecto. Corrígelos y reenvía a revisión.")
                    .link("/admin/mi-proyecto")
                    .icon("flag")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: defensa programada.
        if (Boolean.TRUE.equals(p.getDefensaProgramada()) && p.getFechaDefensa() != null) {
            String cuando = p.getFechaDefensa().format(FECHA) + (p.getHoraDefensa() != null ? " " + p.getHoraDefensa() : "");
            out.add(NotificacionItem.builder()
                    .id("defensa-" + p.getId())
                    .title("Defensa del proyecto programada")
                    .description("Tu defensa fue programada para el " + cuando
                            + (p.getLugarDefensa() != null ? " en " + p.getLugarDefensa() : "") + ".")
                    .link("/admin/mi-proyecto")
                    .icon("calendar-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 6: informe final aprobado por el asesor.
        if (Boolean.TRUE.equals(p.getInformeFinalAprobado())) {
            out.add(NotificacionItem.builder()
                    .id("informe-" + p.getId())
                    .title("Informe final aprobado")
                    .description("Tu asesor aprobó el informe final de tu tesis. Puedes continuar con el trámite de Jurado Informante.")
                    .link("/admin/mi-proyecto")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: observaciones de los revisores por levantar.
        long revObs = revisorRepository.listarPorProyecto(p.getId()).stream()
                .filter(rv -> rv.getEstado() == EstadoRevisor.OBSERVADO && rv.getRespuestaEstudiante() == null)
                .count();
        if (revObs > 0) {
            out.add(NotificacionItem.builder()
                    .id("revobs-" + p.getId())
                    .title("Observaciones de los revisores")
                    .description(revObs + " revisor(es) observaron tu proyecto. Levanta las observaciones desde tu proyecto.")
                    .link("/admin/mi-proyecto")
                    .icon("clipboard-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 7: observaciones del Jurado Informante por levantar.
        long jurObs = informeRevisorRepository.listarPorProyecto(p.getId()).stream()
                .filter(rv -> rv.getEstado() == EstadoRevisor.OBSERVADO && rv.getRespuestaEstudiante() == null)
                .count();
        if (jurObs > 0) {
            out.add(NotificacionItem.builder()
                    .id("jurobs-" + p.getId())
                    .title("Observaciones del Jurado Informante")
                    .description(jurObs + " miembro(s) del Jurado Informante observaron tu informe final. Levanta las observaciones.")
                    .link("/admin/mi-proyecto")
                    .icon("file-check")
                    .fecha(hoy())
                    .build());
        }
        if (Boolean.TRUE.equals(p.getInformeFinalRevisado())) {
            out.add(NotificacionItem.builder()
                    .id("inforev-" + p.getId())
                    .title("Informe final aprobado por el Jurado Informante")
                    .description("El Jurado Informante aprobó tu informe final. Puedes continuar con el trámite de expedito.")
                    .link("/admin/mi-proyecto")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }
    }

    /** Al revisor: proyectos donde el estudiante ya respondió sus observaciones y falta re-evaluar. */
    private void notificacionesRevisor(Persona persona, List<NotificacionItem> out) {
        if (docenteRepository.findByPersonaId(persona.getId()).isEmpty()) return;
        for (Object[] r : revisorRepository.bandejaDeRevisor(persona.getId())) {
            UUID revisorId = (UUID) r[2];
            String estado = asStr(r[3]);
            if (!"OBSERVADO".equals(estado)) continue;
            ProyectoRevisor rv = revisorRepository.buscarPorId(revisorId).orElse(null);
            if (rv == null || rv.getRespuestaEstudiante() == null) continue;
            UUID tesisId = (UUID) r[0];
            String estudiante = ((asStr(r[5]) + " " + asStr(r[6])).trim() + ", " + asStr(r[7])).trim();
            out.add(NotificacionItem.builder()
                    .id("revresp-" + revisorId)
                    .title("El estudiante respondió tus observaciones")
                    .description(estudiante + " levantó tus observaciones. Vuelve a evaluar el proyecto.")
                    .link("/admin/revisor-proyecto/" + tesisId)
                    .icon("clipboard-check")
                    .fecha(hoy())
                    .build());
        }
        // Etapa 7: como miembro del Jurado Informante, informes con respuesta del estudiante por re-evaluar.
        for (Object[] r : informeRevisorRepository.bandejaDeJurado(persona.getId())) {
            if (!"OBSERVADO".equals(asStr(r[3]))) continue;
            InformeRevisor rv = informeRevisorRepository.buscarPorId((UUID) r[2]).orElse(null);
            if (rv == null || rv.getRespuestaEstudiante() == null) continue;
            UUID tesisId = (UUID) r[0];
            String estudiante = ((asStr(r[5]) + " " + asStr(r[6])).trim() + ", " + asStr(r[7])).trim();
            out.add(NotificacionItem.builder()
                    .id("jurresp-" + r[2])
                    .title("El estudiante respondió tus observaciones (informe)")
                    .description(estudiante + " levantó tus observaciones del informe final. Vuelve a evaluar.")
                    .link("/admin/jurado-informe/" + tesisId)
                    .icon("file-check")
                    .fecha(hoy())
                    .build());
        }
    }

    /** Al asesor: por cada proyecto con ítems CORREGIDO (el estudiante corrigió), avisa que verifique. */
    private void notificacionesAsesor(Persona persona, List<NotificacionItem> out) {
        if (docenteRepository.findByPersonaId(persona.getId()).isEmpty()) return;
        UUID asesorId = persona.getId();
        for (Object[] r : proyectoRepository.bandejaDeAsesor(asesorId, null, null, 0, 50)) {
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            long corregidos = revisionRepository.listarPorProyecto(proyectoId).stream()
                    .filter(rev -> rev.getEstado() == EstadoItemRevision.CORREGIDO)
                    .count();
            if (corregidos > 0) {
                String estudiante = ((asStr(r[3]) + " " + asStr(r[4])).trim() + ", " + asStr(r[5])).trim();
                out.add(NotificacionItem.builder()
                        .id("corr-" + proyectoId)
                        .title("Correcciones por verificar")
                        .description(estudiante + " corrigió " + corregidos + " ítem(s). Verifica y da conformidad.")
                        .link("/admin/revision-proyecto/" + tesisId)
                        .icon("rotate-ccw")
                        .fecha(hoy())
                        .build());
            }
        }
    }

    private String fecha(Object o) {
        LocalDate d = null;
        if (o instanceof LocalDate ld) d = ld;
        else if (o instanceof java.sql.Date sd) d = sd.toLocalDate();
        return d != null ? d.format(FECHA) : null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
}
