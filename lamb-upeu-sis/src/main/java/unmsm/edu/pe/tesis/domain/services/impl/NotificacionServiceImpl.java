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
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
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
    @Inject unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository documentoTesisRepository;
    @Inject InformeRevisorRepository informeRevisorRepository;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository solicitudRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository sugerenciaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository dictamenRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenAprobacionRepository dictamenAprobacionRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.RubricaDefensaRepository rubricaDefensaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenExpeditoRepository dictamenExpeditoRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenJuradoInformeRepository dictamenJuradoInformeRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenSustentacionRepository dictamenSustentacionRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.JuradoSustentacionRepository juradoSustentacionRepository;
    @Inject unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository tutoriaRepository;
    @Inject unmsm.edu.pe.tutorias.domain.repositories.ReporteTutoresRepository reporteTutoresRepository;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String hoy() { return LocalDate.now().format(FECHA); }

    private boolean rubricaSubida(UUID tesisId) {
        return documentoTesisRepository.existePorTesisYTipo(tesisId, RevisorProyectoServiceImpl.T_RUBRICA);
    }

    /**
     * Lo que le falta a la Secretaría en un expediente cuya defensa ya está programada:
     * {título, descripción, icono}, o null si el cierre ya se completó.
     */
    private String[] pendienteDeCierre(UUID tesisId) {
        var p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
        if (p == null || Boolean.TRUE.equals(p.getProyectoAprobado())) {
            return null;
        }
        int esperadas = (int) revisorRepository.contarPorProyecto(p.getId());
        int recibidas = rubricaDefensaRepository.porTesis(tesisId).size();
        if (recibidas < esperadas) {
            return new String[]{"Recepciona las rúbricas de la defensa",
                    "Faltan " + (esperadas - recibidas) + " de " + esperadas
                            + " rúbricas de la defensa de {estudiante}.", "clipboard-check"};
        }
        if (!Boolean.TRUE.equals(p.getDefensaRealizada())) {
            return new String[]{"Registra el resultado de la defensa",
                    "Ya tienes las rúbricas de {estudiante}: registra si el proyecto fue aprobado.", "gavel"};
        }
        if (p.getResultadoDefensa() != null && !p.getResultadoDefensa().favorable()) {
            return null;    // desaprobado: no hay dictamen que emitir
        }
        var dic = dictamenAprobacionRepository.buscarPorTesisId(tesisId).orElse(null);
        if (dic == null || dic.getEstado() == unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.POR_ELABORAR) {
            return new String[]{"Elabora el dictamen de aprobación",
                    "El proyecto de {estudiante} fue aprobado en la defensa. Elabora el dictamen.", "stamp"};
        }
        if (dic.getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.FIRMADO) {
            return new String[]{"Sube el dictamen de aprobación firmado",
                    "El dictamen de {estudiante} está elaborado y espera la firma del Director.", "file-up"};
        }
        return new String[]{"Archiva el proyecto final",
                "Falta archivar el proyecto final de {estudiante} para cerrar la etapa.", "archive"};
    }

    /**
     * Lo que le falta a la Secretaría en el trámite del Jurado Informante / Dictamen de Expedito,
     * o null si ya está cerrado (Dictamen de Expedito firmado).
     */
    private String[] pendienteDeJuradoInforme(UUID tesisId) {
        var p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
        if (p == null) return null;
        var expedito = dictamenExpeditoRepository.buscarPorTesisId(tesisId).orElse(null);
        if (expedito != null && expedito.getEstado() == unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.FIRMADO) {
            return null;
        }
        if (!Boolean.TRUE.equals(p.getExpedienteInformeRecibido())) {
            return new String[]{"Recepciona el expediente del Jurado Informante",
                    "{estudiante} solicitó el Jurado Informante. Recepciona el expediente y comunícalo al Coordinador.", "inbox"};
        }
        var proy = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
        long numJurado = proy != null ? informeRevisorRepository.contarPorProyecto(proy.getId()) : 0;
        if (numJurado < 3) {
            return null; // esperando al Coordinador
        }
        var dic = dictamenJuradoInformeRepository.buscarPorTesisId(tesisId).orElse(null);
        if (dic == null || dic.getEstado() == unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.POR_ELABORAR) {
            return new String[]{"Elabora el dictamen del Jurado Informante",
                    "El Coordinador designó al Jurado Informante de {estudiante}. Elabora el dictamen de designación.", "stamp"};
        }
        if (dic.getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.FIRMADO) {
            return new String[]{"Sube el dictamen del Jurado Informante firmado",
                    "El dictamen del Jurado Informante de {estudiante} está elaborado y espera la firma.", "file-up"};
        }
        if (!Boolean.TRUE.equals(p.getInformeFinalRevisado())) {
            return null; // esperando la conformidad del jurado
        }
        if (!Boolean.TRUE.equals(p.getInformeFinalArchivado())) {
            return new String[]{"Archiva el expediente del Jurado Informante",
                    "El Jurado Informante dio conformidad al informe final de {estudiante}. Archiva el expediente.", "archive"};
        }
        return new String[]{"Elabora el Dictamen de Expedito",
                "El expediente del Jurado Informante de {estudiante} está archivado. Elabora el Dictamen de Expedito.", "stamp"};
    }

    /** Lo que le falta a la Secretaría en el trámite de Sustentación, o null si ya concluyó. */
    private String[] pendienteDeSustentacion(UUID tesisId, ProyectoTesis p) {
        if (Boolean.TRUE.equals(p.getTesisConcluida())) {
            return null;
        }
        if (!Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido())) {
            return new String[]{"Recepciona el expediente de sustentación",
                    "{estudiante} solicitó su Jurado de Sustentación. Recepciona el expediente y comunícalo al Coordinador.", "inbox"};
        }
        long numJurado = juradoSustentacionRepository.contarPorProyecto(p.getId());
        if (numJurado < 3) {
            return null; // esperando al Coordinador
        }
        var dic = dictamenSustentacionRepository.buscarPorTesisId(tesisId).orElse(null);
        if (dic == null || dic.getEstado() == unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.POR_ELABORAR) {
            return new String[]{"Elabora el dictamen del Jurado de Sustentación",
                    "El Coordinador designó al Jurado de Sustentación de {estudiante}. Elabora el dictamen de designación.", "stamp"};
        }
        if (dic.getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.FIRMADO) {
            return new String[]{"Sube el dictamen de sustentación firmado",
                    "El dictamen del Jurado de Sustentación de {estudiante} está elaborado y espera la firma.", "file-up"};
        }
        if (!Boolean.TRUE.equals(p.getSustentacionProgramada())) {
            return new String[]{"Programa la sustentación",
                    "El Jurado de Sustentación de {estudiante} ya fue designado. Coordina modalidad, lugar y fecha.", "calendar-check"};
        }
        if (!Boolean.TRUE.equals(p.getActaSustentacionSubida())) {
            return new String[]{"Registra el Acta de sustentación",
                    "Llegó el día de la sustentación de {estudiante}. Registra el resultado y sube el Acta firmada.", "gavel"};
        }
        return null;
    }

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
            // Rúbrica + programación de defensa: proyectos con revisores designados.
            for (Object[] r : proyectoRepository.bandejaDefensa(null, 0, 50)) {
                int numRevisores = r[9] != null ? ((Number) r[9]).intValue() : 0;
                UUID tesisId = (UUID) r[0];
                String estudiante = ((asStr(r[2]) + " " + asStr(r[3])).trim() + ", " + asStr(r[4])).trim();
                if (numRevisores > 0 && !rubricaSubida(tesisId)) {
                    out.add(NotificacionItem.builder()
                            .id("subrubrica-" + tesisId)
                            .title("Sube la rúbrica de los revisores")
                            .description("El coordinador designó los revisores de " + estudiante
                                    + ". Sube la rúbrica oficial (Excel) para habilitar la evaluación.")
                            .link("/admin/secretaria-defensa").icon("file-up").fecha(hoy()).build());
                }
                boolean revisoresConformes = r.length > 10 && Boolean.TRUE.equals(r[10]);
                boolean defensaProgramada = r.length > 11 && Boolean.TRUE.equals(r[11]);
                if (revisoresConformes && !defensaProgramada) {
                    out.add(NotificacionItem.builder()
                            .id("secprogdef-" + tesisId)
                            .title("Programa la defensa del proyecto")
                            .description("Los revisores aprobaron el proyecto de " + estudiante
                                    + ". Programa la fecha, hora y lugar (o enlace) de la defensa.")
                            .link("/admin/secretaria-defensa").icon("calendar-check").fecha(hoy()).build());
                }
                // Tras la defensa: rúbricas por recepcionar, resultado por registrar y dictamen de aprobación.
                if (defensaProgramada) {
                    var pendiente = pendienteDeCierre(tesisId);
                    if (pendiente != null) {
                        out.add(NotificacionItem.builder()
                                .id("cierre-" + tesisId)
                                .title(pendiente[0])
                                .description(pendiente[1].replace("{estudiante}", estudiante))
                                .link("/admin/cierre-proyecto/" + tesisId).icon(pendiente[2]).fecha(hoy()).build());
                    }
                }
                // Trámite del Jurado Informante y Dictamen de Expedito (Etapa 7).
                boolean juradoInformeSolicitado = r.length > 13 && Boolean.TRUE.equals(r[13]);
                if (juradoInformeSolicitado) {
                    var pendienteJI = pendienteDeJuradoInforme(tesisId);
                    if (pendienteJI != null) {
                        out.add(NotificacionItem.builder()
                                .id("jinforme-" + tesisId)
                                .title(pendienteJI[0])
                                .description(pendienteJI[1].replace("{estudiante}", estudiante))
                                .link("/admin/jurado-informante/" + tesisId).icon(pendienteJI[2]).fecha(hoy()).build());
                    }
                }
                // Trámite de Sustentación (Etapa 8, la última).
                ProyectoTesis pSust = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
                if (pSust != null && Boolean.TRUE.equals(pSust.getSustentacionSolicitada())) {
                    var pendienteSust = pendienteDeSustentacion(tesisId, pSust);
                    if (pendienteSust != null) {
                        out.add(NotificacionItem.builder()
                                .id("sustent-" + tesisId)
                                .title(pendienteSust[0])
                                .description(pendienteSust[1].replace("{estudiante}", estudiante))
                                .link("/admin/sustentacion/" + tesisId).icon(pendienteSust[2]).fecha(hoy()).build());
                    }
                }
            }
            // Dictamen de designación de asesor por elaborar (el estudiante ya subió ambos firmados).
            for (Object[] r : dictamenRepository.bandeja("POR_ELABORAR", null, null, null, 0, 50)) {
                UUID tesisId = (UUID) r[0];
                String estudiante = ((asStr(r[1]) + " " + asStr(r[2])).trim() + ", " + asStr(r[3])).trim();
                out.add(NotificacionItem.builder()
                        .id("dictelab-" + tesisId)
                        .title("Elabora el dictamen de designación de asesor")
                        .description(estudiante + " subió su solicitud y la carta de aceptación firmadas. Elabora el dictamen de designación.")
                        .link("/admin/dictamenes").icon("stamp").fecha(hoy()).build());
            }
        }

        // Coordinador: proyectos recepcionados que requieren su acción (designar revisores / programar defensa / jurado informante).
        if (roles.contains("COORDINADOR") || roles.contains("ADMIN")) {
            notificacionesCoordinador(out);
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
            notificacionesTutor(persona, out);              // designación de tutor: sugerir asesor
            notificacionesSolicitudAsesor(persona, out);    // solicitud de asesoría por responder
        }

        return out;
    }

    @Override
    @Transactional
    public List<NotificacionItem> notificacionesDeEstudiante(Persona persona) {
        List<NotificacionItem> out = new ArrayList<>();
        notificacionesEstudiante(persona, out);
        return out;
    }

    @Override
    @Transactional
    public List<NotificacionItem> notificacionesDeAsesor(Persona persona) {
        List<NotificacionItem> out = new ArrayList<>();
        notificacionesAsesor(persona, out);
        return out;
    }

    @Override
    @Transactional
    public List<NotificacionItem> notificacionesDeRevisor(Persona persona) {
        List<NotificacionItem> out = new ArrayList<>();
        notificacionesRevisor(persona, out);
        return out;
    }

    /** Al estudiante: si su proyecto tiene ítems OBSERVADO/EN_CORRECCION, avisa cuántos debe corregir. */
    private void notificacionesEstudiante(Persona persona, List<NotificacionItem> out) {
        Estudiante est = estudianteRepository.findByPersonaId(persona.getId()).orElse(null);
        if (est == null) return;
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(est.getPersonaId()).orElse(null);
        if (tesis == null) return;

        // Fase de designación de asesor (ocurre antes de que exista el editor del proyecto).
        notificacionesDesignacionAsesor(est, tesis, out);

        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesis.getId()).orElse(null);
        if (p == null) return;

        var revisiones = revisionRepository.listarPorProyecto(p.getId());
        long pendientes = revisiones.stream()
                .filter(r -> r.getEstado() == EstadoItemRevision.OBSERVADO
                        || r.getEstado() == EstadoItemRevision.EN_CORRECCION)
                .count();
        if (pendientes > 0) {
            // Antes de la carta del asesor las observaciones son suyas; después, del revisor.
            boolean faseRevisor = Boolean.TRUE.equals(p.getCartaAsesor());
            out.add(NotificacionItem.builder()
                    .id("obs-" + p.getId())
                    .title(faseRevisor ? "Observaciones de revisor por corregir" : "Observaciones por corregir")
                    .description(faseRevisor
                            ? "Un revisor observó " + pendientes + " ítem(s) de tu proyecto. Corrígelos desde tu proyecto."
                            : "Tu asesor observó " + pendientes + " ítem(s) de tu proyecto. Corrígelos y reenvía a revisión.")
                    .link("/admin/mi-proyecto")
                    .icon("flag")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 4: el asesor emitió su carta de opinión favorable → ya puede cerrar y presentar.
        if (Boolean.TRUE.equals(p.getCartaAsesor()) && !Boolean.TRUE.equals(p.getExpedienteSubido())) {
            out.add(NotificacionItem.builder()
                    .id("carta-" + p.getId())
                    .title("Tu carta de opinión favorable está lista")
                    .description("Tu asesor emitió su carta de opinión favorable. Ya puedes subir el Turnitin y el proyecto final, y presentar tu solicitud de aprobación.")
                    .link("/admin/mi-tesis/cierre")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: expediente recibido por Secretaría (aún sin revisores designados).
        if (Boolean.TRUE.equals(p.getExpedienteRecibido()) && revisorRepository.contarPorProyecto(p.getId()) == 0
                && !Boolean.TRUE.equals(p.getDefensaProgramada())) {
            out.add(NotificacionItem.builder()
                    .id("exprecibido-" + p.getId())
                    .title("Expediente recibido por Secretaría")
                    .description("Tu solicitud fue recibida y comunicada al Coordinador para la designación de revisores.")
                    .link("/admin/mi-tesis/cierre").icon("inbox").fecha(hoy()).build());
        }

        // Etapa 5: defensa programada.
        if (Boolean.TRUE.equals(p.getDefensaProgramada()) && p.getFechaDefensa() != null) {
            String cuando = p.getFechaDefensa().format(FECHA) + (p.getHoraDefensa() != null ? " " + p.getHoraDefensa() : "");
            out.add(NotificacionItem.builder()
                    .id("defensa-" + p.getId())
                    .title("Defensa del proyecto programada")
                    .description("Tu defensa fue programada para el " + cuando
                            + (p.getLugarDefensa() != null ? " en " + p.getLugarDefensa() : "") + ".")
                    .link("/admin/mi-tesis/cierre")
                    .icon("calendar-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: resultado de la defensa y aprobación del proyecto.
        if (Boolean.TRUE.equals(p.getDefensaRealizada()) && p.getResultadoDefensa() != null
                && !Boolean.TRUE.equals(p.getProyectoAprobado())) {
            boolean favorable = p.getResultadoDefensa().favorable();
            out.add(NotificacionItem.builder()
                    .id("resdefensa-" + p.getId())
                    .title("Resultado de tu defensa: " + p.getResultadoDefensa().etiqueta().toLowerCase())
                    .description(favorable
                            ? "Tu proyecto fue aprobado en la defensa. La Secretaría emitirá el dictamen de aprobación."
                            : "Tu proyecto fue desaprobado en la defensa. Los revisores lo evaluarán nuevamente: corrige las observaciones que te dejen.")
                    .link(favorable ? "/admin/mi-tesis/cierre" : "/admin/mi-proyecto")
                    .icon(favorable ? "badge-check" : "circle-alert")
                    .fecha(hoy())
                    .build());
        }
        if (Boolean.TRUE.equals(p.getProyectoAprobado())) {
            out.add(NotificacionItem.builder()
                    .id("proyaprob-" + p.getId())
                    .title("Proyecto de tesis aprobado")
                    .description("Se emitió el dictamen de aprobación de tu proyecto. Puedes iniciar la ejecución de la tesis.")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("stamp")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 6: informe final aprobado por el asesor.
        if (Boolean.TRUE.equals(p.getInformeFinalAprobado())) {
            out.add(NotificacionItem.builder()
                    .id("informe-" + p.getId())
                    .title("Informe final aprobado")
                    .description("Tu asesor aprobó el informe final de tu tesis. Puedes continuar con el trámite de Jurado Informante.")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: observaciones GENERALES de los revisores (comentario único, sin desglose por ítem).
        // Si el revisor observó por ítem, el aviso por-ítem de arriba ya lo cubre (se evita duplicar).
        boolean hayItemsRevisor = revisiones.stream()
                .anyMatch(r -> r.getEstado() == EstadoItemRevision.OBSERVADO
                        || r.getEstado() == EstadoItemRevision.EN_CORRECCION
                        || r.getEstado() == EstadoItemRevision.CORREGIDO);
        long revObs = revisorRepository.listarPorProyecto(p.getId()).stream()
                .filter(rv -> rv.getEstado() == EstadoRevisor.OBSERVADO && rv.getRespuestaEstudiante() == null)
                .count();
        if (revObs > 0 && !hayItemsRevisor) {
            out.add(NotificacionItem.builder()
                    .id("revobs-" + p.getId())
                    .title("Observaciones de los revisores")
                    .description(revObs + " revisor(es) observaron tu proyecto. Levanta las observaciones desde tu proyecto.")
                    .link("/admin/mi-proyecto")
                    .icon("clipboard-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: conformidad parcial. Sin esto, el alumno no se enteraba de que un revisor ya
        // había aprobado: solo avisábamos cuando estaban los dos, que puede tardar semanas.
        var revisores = revisorRepository.listarPorProyecto(p.getId());
        long conformes = revisores.stream().filter(rv -> rv.getEstado() == EstadoRevisor.CONFORME).count();
        if (conformes > 0 && !Boolean.TRUE.equals(p.getRevisoresConformes())) {
            out.add(NotificacionItem.builder()
                    .id("revconf-" + p.getId() + "-" + conformes)
                    .title(conformes == 1 ? "Un revisor dio conformidad" : conformes + " revisores dieron conformidad")
                    .description("Tu proyecto está conforme para " + conformes + " de " + revisores.size()
                            + " revisor(es). Falta la conformidad del resto.")
                    .link("/admin/mi-tesis/cierre")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 5: todos los revisores dieron conformidad.
        if (Boolean.TRUE.equals(p.getRevisoresConformes())) {
            out.add(NotificacionItem.builder()
                    .id("revok-" + p.getId())
                    .title("Los revisores aprobaron tu proyecto")
                    .description("Los revisores dieron conformidad a tu proyecto de tesis. Continúa con los siguientes pasos.")
                    .link("/admin/mi-tesis/cierre")
                    .icon("badge-check")
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
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("file-check")
                    .fecha(hoy())
                    .build());
        }
        if (Boolean.TRUE.equals(p.getInformeFinalRevisado())) {
            out.add(NotificacionItem.builder()
                    .id("inforev-" + p.getId())
                    .title("Informe final aprobado por el Jurado Informante")
                    .description("El Jurado Informante aprobó tu informe final. Secretaría está cerrando ese expediente; te avisaremos cuando tengas tu Dictamen de Expedito.")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 8: Dictamen de Expedito emitido → ya puede solicitar su Jurado de Sustentación.
        boolean expeditoFirmado = dictamenExpeditoRepository.buscarPorTesisId(p.getTesisId())
                .map(d -> d.getEstado() == unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.FIRMADO).orElse(false);
        if (expeditoFirmado && !Boolean.TRUE.equals(p.getSustentacionSolicitada())) {
            out.add(NotificacionItem.builder()
                    .id("expedito-" + p.getId())
                    .title("Ya estás expedito para sustentar")
                    .description("Secretaría emitió tu Dictamen de Expedito. Solicita tu Jurado de Sustentación desde Ejecución de tesis.")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 8: sustentación programada.
        if (Boolean.TRUE.equals(p.getSustentacionProgramada()) && p.getFechaSustentacion() != null
                && !Boolean.TRUE.equals(p.getTesisConcluida())) {
            String cuando = p.getFechaSustentacion().format(FECHA) + (p.getHoraSustentacion() != null ? " " + p.getHoraSustentacion() : "");
            out.add(NotificacionItem.builder()
                    .id("sustprog-" + p.getId())
                    .title("Tu sustentación fue programada")
                    .description("Tu sustentación de tesis fue programada para el " + cuando
                            + (p.getLugarSustentacion() != null ? " en " + p.getLugarSustentacion() : "") + ".")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("calendar-check")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 8: acto de sustentación desaprobado → Secretaría coordinará un nuevo acto.
        if (Boolean.TRUE.equals(p.getActaSustentacionSubida()) && p.getResultadoSustentacion() != null
                && !p.getResultadoSustentacion().favorable() && !Boolean.TRUE.equals(p.getTesisConcluida())) {
            out.add(NotificacionItem.builder()
                    .id("sustdesap-" + p.getId())
                    .title("Resultado de tu sustentación: " + p.getResultadoSustentacion().etiqueta().toLowerCase())
                    .description("Tu sustentación fue desaprobada. Secretaría coordinará contigo y tu Jurado un nuevo acto.")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("circle-alert")
                    .fecha(hoy())
                    .build());
        }

        // Etapa 8: tesis concluida (última notificación del proceso).
        if (Boolean.TRUE.equals(p.getTesisConcluida())) {
            out.add(NotificacionItem.builder()
                    .id("concluida-" + p.getId())
                    .title("¡Tu proceso de titulación ha concluido!")
                    .description("Secretaría archivó el Acta de sustentación" + (p.getResultadoSustentacion() != null
                            ? " · " + p.getResultadoSustentacion().etiqueta().toLowerCase() : "") + ". Tu tesis quedó sustentada.")
                    .link("/admin/mi-tesis/ejecucion")
                    .icon("badge-check")
                    .fecha(hoy())
                    .build());
        }
    }

    /** Al estudiante, fase de designación de asesor: tutor asignado, asesores sugeridos, aceptación/rechazo, dictamen. */
    private void notificacionesDesignacionAsesor(Estudiante est, Tesis tesis, List<NotificacionItem> out) {
        UUID estId = est.getPersonaId();
        UUID tesisId = tesis.getId();
        // La del ASESOR: una solicitud de co-asesoría (opcional, posterior) no debe tapar el
        // aviso de "tu asesor aceptó: firma y sube tus documentos".
        SolicitudAsesoria sol = solicitudRepository
                .ultimaDeEstudiantePorTipo(estId, unmsm.edu.pe.tesis.domain.enums.TipoAsesoria.ASESOR)
                .orElse(null);
        SolicitudAsesoria solCo = solicitudRepository
                .ultimaDeEstudiantePorTipo(estId, unmsm.edu.pe.tesis.domain.enums.TipoAsesoria.COASESOR)
                .orElse(null);
        boolean dictamenFirmado = documentoTesisRepository.existePorTesisYTipo(tesisId, "DICTAMEN_DESIGNACION_FIRMADO");

        // Aún no ha solicitado: tutor asignado y/o asesores sugeridos.
        if (sol == null) {
            var tut = tutoriaRepository.findVigenteByEstudiante(estId).orElse(null);
            boolean haySugerencias = !sugerenciaRepository.listarPorEstudiante(estId).isEmpty();
            if (tut != null && haySugerencias) {
                out.add(NotificacionItem.builder()
                        .id("alsug-" + tesisId)
                        .title("Tu tutor te sugirió asesor(es)")
                        .description("Tu tutor te sugirió asesor(es) para tu tema. Revisa la lista y solicita tu asesoría.")
                        .link("/admin/mi-asesoria").icon("users").fecha(hoy()).build());
            } else if (tut != null) {
                out.add(NotificacionItem.builder()
                        .id("altut-" + tesisId)
                        .title("Se te asignó un tutor")
                        .description("Tu tutor " + nombrePersona(tut.getDocente() != null ? tut.getDocente().getPersona() : null)
                                + " te sugerirá asesores para tu tema.")
                        .link("/admin/mi-asesoria").icon("user-check").fecha(hoy()).build());
            }
        }

        // Solicitud rechazada: puede pedir a otro asesor sugerido.
        if (sol != null && sol.getEstado() == EstadoSolicitud.RECHAZADA) {
            out.add(NotificacionItem.builder()
                    .id("alrech-" + sol.getId())
                    .title("Tu solicitud de asesoría fue rechazada")
                    .description("El docente rechazó tu solicitud. Solicita a otro asesor sugerido por tu tutor.")
                    .link("/admin/mi-asesoria").icon("circle-x").fecha(hoy()).build());
        }

        // Solicitud aceptada (y aún sin dictamen): firmar y subir los documentos.
        if (sol != null && sol.getEstado() == EstadoSolicitud.ACEPTADA && !dictamenFirmado) {
            out.add(NotificacionItem.builder()
                    .id("alacep-" + sol.getId())
                    .title("Tu asesor aceptó la asesoría")
                    .description("El asesor aceptó. Descarga, firma y sube tu Solicitud y la Carta de aceptación para que Secretaría elabore el dictamen.")
                    .link("/admin/mi-asesoria").icon("handshake").fecha(hoy()).build());
        }

        // Co-asesoría rechazada: es opcional, así que el aviso es aparte y no habla del asesor.
        if (solCo != null && solCo.getEstado() == EstadoSolicitud.RECHAZADA) {
            out.add(NotificacionItem.builder()
                    .id("alrechco-" + solCo.getId())
                    .title("Tu solicitud de co-asesoría fue rechazada")
                    .description("El docente rechazó la co-asesoría. Puedes solicitar a otro o continuar solo con tu asesor.")
                    .link("/admin/mi-asesoria").icon("circle-x").fecha(hoy()).build());
        }

        // Dictamen de designación emitido.
        if (dictamenFirmado) {
            out.add(NotificacionItem.builder()
                    .id("aldict-" + tesisId)
                    .title("Dictamen de designación de asesor emitido")
                    .description("Secretaría emitió el dictamen de designación de tu asesor. Descárgalo desde Mi asesoría.")
                    .link("/admin/mi-asesoria").icon("stamp").fecha(hoy()).build());
        }
    }

    /** Al revisor: proyectos donde el estudiante ya respondió sus observaciones y falta re-evaluar. */
    /** Al Coordinador: proyectos que esperan su designación de revisores / jurado informante / jurado de sustentación. */
    private void notificacionesCoordinador(List<NotificacionItem> out) {
        for (Object[] r : proyectoRepository.bandejaDefensa(null, 0, 50)) {
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            String estudiante = ((asStr(r[2]) + " " + asStr(r[3])).trim() + ", " + asStr(r[4])).trim();
            int numRevisores = r.length > 9 && r[9] != null ? ((Number) r[9]).intValue() : 0;
            boolean juradoSolicitado = r.length > 13 && Boolean.TRUE.equals(r[13]);
            int numJuradoInforme = r.length > 14 && r[14] != null ? ((Number) r[14]).intValue() : 0;

            if (numRevisores == 0) {
                out.add(NotificacionItem.builder()
                        .id("cordrev-" + tesisId)
                        .title("Designa los revisores del proyecto")
                        .description("El expediente de " + estudiante + " fue recepcionado. Designa a los 2 revisores.")
                        .link("/admin/coordinador-proyecto").icon("user-plus").fecha(hoy()).build());
            }
            // La programación de la defensa ya no es del coordinador (la hace la Secretaría).
            var p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
            if (p == null) continue;
            boolean expedienteInformeRecibido = Boolean.TRUE.equals(p.getExpedienteInformeRecibido());
            if (juradoSolicitado && expedienteInformeRecibido && numJuradoInforme == 0) {
                out.add(NotificacionItem.builder()
                        .id("cordjur-" + tesisId)
                        .title("Designa el Jurado Informante")
                        .description(estudiante + " solicitó el Jurado Informante. Designa a los 3 miembros.")
                        .link("/admin/coordinador-proyecto").icon("user-plus").fecha(hoy()).build());
            }
            boolean expedienteSustentacionRecibido = Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido());
            long numJuradoSustentacion = juradoSustentacionRepository.contarPorProyecto(proyectoId);
            if (expedienteSustentacionRecibido && numJuradoSustentacion == 0) {
                out.add(NotificacionItem.builder()
                        .id("cordsust-" + tesisId)
                        .title("Designa el Jurado de Sustentación")
                        .description(estudiante + " solicitó su sustentación. Designa a los 3 miembros del Jurado.")
                        .link("/admin/coordinador-proyecto").icon("user-plus").fecha(hoy()).build());
            }
        }
    }

    private void notificacionesRevisor(Persona persona, List<NotificacionItem> out) {
        if (docenteRepository.findByPersonaId(persona.getId()).isEmpty()) return;
        // Etapa 5: revisor del proyecto.
        for (Object[] r : revisorRepository.bandejaDeRevisor(persona.getId())) {
            UUID revisorId = (UUID) r[2];
            String estado = asStr(r[3]);
            UUID tesisId = (UUID) r[0];
            String estudiante = ((asStr(r[5]) + " " + asStr(r[6])).trim() + ", " + asStr(r[7])).trim();
            if ("DESIGNADO".equals(estado)) {
                if (rubricaSubida(tesisId)) {
                    out.add(NotificacionItem.builder()
                            .id("revrubrica-" + revisorId)
                            .title("Rúbrica lista: ya puedes evaluar")
                            .description("Secretaría subió la rúbrica oficial. Evalúa el proyecto de " + estudiante + ".")
                            .link("/admin/revisor-proyecto/" + tesisId)
                            .icon("clipboard-check").fecha(hoy()).build());
                } else {
                    out.add(NotificacionItem.builder()
                            .id("revdesig-" + revisorId)
                            .title("Fuiste designado revisor de un proyecto")
                            .description("Proyecto de " + estudiante + ". Podrás evaluar cuando Secretaría suba la rúbrica oficial.")
                            .link("/admin/revisor-proyecto/" + tesisId)
                            .icon("clipboard-check").fecha(hoy()).build());
                }
            } else if ("OBSERVADO".equals(estado)) {
                ProyectoRevisor rv = revisorRepository.buscarPorId(revisorId).orElse(null);
                if (rv != null && rv.getRespuestaEstudiante() != null) {
                    out.add(NotificacionItem.builder()
                            .id("revresp-" + revisorId)
                            .title("El estudiante respondió tus observaciones")
                            .description(estudiante + " levantó tus observaciones. Vuelve a evaluar el proyecto.")
                            .link("/admin/revisor-proyecto/" + tesisId)
                            .icon("clipboard-check").fecha(hoy()).build());
                }
            }
            // Correcciones por ítem del estudiante a las observaciones del revisor.
            UUID proyectoId = (UUID) r[1];
            long corregidos = revisionRepository.listarPorProyecto(proyectoId).stream()
                    .filter(rev -> rev.getEstado() == EstadoItemRevision.CORREGIDO)
                    .count();
            if (corregidos > 0) {
                out.add(NotificacionItem.builder()
                        .id("revcorr-" + revisorId)
                        .title("Correcciones por verificar")
                        .description(estudiante + " corrigió " + corregidos + " ítem(s) que observaste. Verifica y da conformidad.")
                        .link("/admin/revisor-proyecto/" + tesisId)
                        .icon("rotate-ccw").fecha(hoy()).build());
            }
            // Etapa 5: la Secretaría programó la defensa → evalúala con la rúbrica.
            ProyectoTesis pt = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
            if (pt != null && Boolean.TRUE.equals(pt.getDefensaProgramada()) && pt.getFechaDefensa() != null) {
                String cuando = pt.getFechaDefensa().format(FECHA) + (pt.getHoraDefensa() != null ? " " + pt.getHoraDefensa() : "");
                out.add(NotificacionItem.builder()
                        .id("revdefensa-" + revisorId)
                        .title("Evalúa la defensa con la rúbrica")
                        .description("La defensa de " + estudiante + " fue programada para el " + cuando
                                + (pt.getLugarDefensa() != null ? " en " + pt.getLugarDefensa() : "") + ". Evalúa la defensa con la rúbrica de evaluación.")
                        .link("/admin/revisor-proyecto/" + tesisId)
                        .icon("calendar-check").fecha(hoy()).build());
            }
        }
        // Etapa 7: miembro del Jurado Informante.
        for (Object[] r : informeRevisorRepository.bandejaDeJurado(persona.getId())) {
            String estado = asStr(r[3]);
            UUID tesisId = (UUID) r[0];
            String estudiante = ((asStr(r[5]) + " " + asStr(r[6])).trim() + ", " + asStr(r[7])).trim();
            if ("DESIGNADO".equals(estado)) {
                out.add(NotificacionItem.builder()
                        .id("jurdesig-" + r[2])
                        .title("Fuiste designado Jurado Informante")
                        .description("Evalúa el informe final de " + estudiante + ".")
                        .link("/admin/jurado-informe/" + tesisId)
                        .icon("file-check").fecha(hoy()).build());
            } else if ("OBSERVADO".equals(estado)) {
                InformeRevisor rv = informeRevisorRepository.buscarPorId((UUID) r[2]).orElse(null);
                if (rv != null && rv.getRespuestaEstudiante() != null) {
                    out.add(NotificacionItem.builder()
                            .id("jurresp-" + r[2])
                            .title("El estudiante respondió tus observaciones (informe)")
                            .description(estudiante + " levantó tus observaciones del informe final. Vuelve a evaluar.")
                            .link("/admin/jurado-informe/" + tesisId)
                            .icon("file-check").fecha(hoy()).build());
                }
            }
        }
    }

    /** Al asesor: por cada proyecto con ítems CORREGIDO (el estudiante corrigió), avisa que verifique. */
    private void notificacionesAsesor(Persona persona, List<NotificacionItem> out) {
        if (docenteRepository.findByPersonaId(persona.getId()).isEmpty()) return;
        UUID asesorId = persona.getId();
        for (Object[] r : proyectoRepository.bandejaDeAsesor(asesorId, null, null, 0, 50)) {
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            String estadoProyecto = asStr(r[2]);
            String estudiante = ((asStr(r[3]) + " " + asStr(r[4])).trim() + ", " + asStr(r[5])).trim();
            var revisiones = revisionRepository.listarPorProyecto(proyectoId);
            long corregidos = revisiones.stream()
                    .filter(rev -> rev.getEstado() == EstadoItemRevision.CORREGIDO)
                    .count();
            if (corregidos > 0) {
                out.add(NotificacionItem.builder()
                        .id("corr-" + proyectoId)
                        .title("Correcciones por verificar")
                        .description(estudiante + " corrigió " + corregidos + " ítem(s). Verifica y da conformidad.")
                        .link("/admin/revision-proyecto/" + tesisId)
                        .icon("rotate-ccw")
                        .fecha(hoy())
                        .build());
            } else if ("EN_REVISION".equals(estadoProyecto) && revisiones.isEmpty()) {
                // Proyecto recién enviado a revisión, aún sin observaciones.
                out.add(NotificacionItem.builder()
                        .id("nuevorev-" + proyectoId)
                        .title("Nuevo proyecto por revisar")
                        .description(estudiante + " envió su proyecto a revisión. Revísalo y observa o da conformidad por ítem.")
                        .link("/admin/revision-proyecto/" + tesisId)
                        .icon("clipboard-check")
                        .fecha(hoy())
                        .build());
            }
        }
    }

    /** Al tutor: por cada tutorando con tema y sin asesor al que aún no le sugirió asesores. */
    private void notificacionesTutor(Persona persona, List<NotificacionItem> out) {
        if (docenteRepository.findByPersonaId(persona.getId()).isEmpty()) return;
        for (Object[] r : reporteTutoresRepository.tutorandos(persona.getId(), null, 0, 50)) {
            UUID estId = (UUID) r[0];
            boolean tieneTema = r[9] != null;                                  // te.id
            boolean tieneAsesor = r[13] != null && ((Number) r[13]).intValue() == 1;
            if (!tieneTema || tieneAsesor) continue;
            if (!sugerenciaRepository.listarPorEstudiante(estId).isEmpty()) continue; // ya sugirió
            String estudiante = ((asStr(r[1]) + " " + asStr(r[2])).trim() + ", " + asStr(r[3])).trim();
            out.add(NotificacionItem.builder()
                    .id("tutsug-" + estId)
                    .title("Sugiere un asesor a tu tutorando")
                    .description(estudiante + " ya tiene tema y necesita asesor. Sugiérele docentes de su línea de investigación.")
                    .link("/admin/mis-tutorandos").icon("user-plus").fecha(hoy()).build());
        }
    }

    /** Al docente: solicitudes de asesoría PENDIENTES dirigidas a él, por aceptar o rechazar. */
    private void notificacionesSolicitudAsesor(Persona persona, List<NotificacionItem> out) {
        if (docenteRepository.findByPersonaId(persona.getId()).isEmpty()) return;
        for (Object[] r : solicitudRepository.listarPorDocente(persona.getId(), EstadoSolicitud.PENDIENTE, 0, 50)) {
            UUID solId = (UUID) r[0];
            String estudiante = ((asStr(r[3]) + " " + asStr(r[4])).trim() + ", " + asStr(r[2])).trim();
            out.add(NotificacionItem.builder()
                    .id("solped-" + solId)
                    .title("Solicitud de asesoría por responder")
                    .description(estudiante + " te solicitó como asesor. Acepta o rechaza la solicitud.")
                    .link("/admin/solicitudes-asesoria").icon("user-check").fecha(hoy()).build());
        }
    }

    private String fecha(Object o) {
        LocalDate d = null;
        if (o instanceof LocalDate ld) d = ld;
        else if (o instanceof java.sql.Date sd) d = sd.toLocalDate();
        return d != null ? d.format(FECHA) : null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }

    private String nz(String s) { return s == null ? "" : s; }

    private String nombrePersona(Persona pe) {
        if (pe == null) return "";
        return ((nz(pe.getApellidoPaterno()) + " " + nz(pe.getApellidoMaterno())).trim()
                + ", " + nz(pe.getNombres())).trim();
    }
}
