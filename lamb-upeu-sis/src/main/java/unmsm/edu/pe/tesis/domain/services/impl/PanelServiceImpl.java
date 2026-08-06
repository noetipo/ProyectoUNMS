package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.tesis.application.dto.PanelResponse;
import unmsm.edu.pe.tesis.application.dto.PanelResponse.Conteo;
import unmsm.edu.pe.tesis.application.dto.PanelResponse.Metrica;
import unmsm.edu.pe.tesis.application.dto.PanelResponse.Pendiente;
import unmsm.edu.pe.tesis.application.dto.SeguimientoAlumnoItem;
import unmsm.edu.pe.tesis.application.util.EtapasProceso;
import unmsm.edu.pe.tesis.domain.services.PanelService;
import unmsm.edu.pe.tesis.domain.services.SeguimientoService;
import unmsm.edu.pe.tesis.infrastructure.export.PanelExcel;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Panel de inicio.
 *
 * <p>Se apoya en el cálculo de etapas del tablero de Seguimiento —fuente única: si cambia la
 * cascada de hitos, el panel cambia con ella— pero <b>solo publica conteos</b> en la parte
 * institucional. El bloque personal se arma con lo que ese usuario ya puede ver por su rol.</p>
 */
@ApplicationScoped
public class PanelServiceImpl implements PanelService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject SeguimientoService seguimientoService;
    @Inject unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository proyectoRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository revisorRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository dictamenRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository asesoriaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository solicitudRepository;
    @Inject unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository lineaRepository;

    @Override
    @Transactional
    public PanelResponse panel() {
        Persona persona = personaActual();
        Set<String> roles = roles();
        List<SeguimientoAlumnoItem> alumnos = seguimientoService.tablero(null, null).getAlumnos();

        String rol = rolPrincipal(roles, persona);
        PanelResponse.PanelResponseBuilder b = PanelResponse.builder()
                .rol(rol)
                .rolLabel(rolLabel(rol))
                .saludo(saludo() + (persona != null && persona.getNombres() != null
                        ? ", " + primerNombre(persona.getNombres()) : ""));

        // Las cifras del programa son para quien gestiona o enseña; el doctorando ve las suyas.
        boolean institucional = !"ESTUDIANTE".equals(rol);
        if (institucional) {
            b.agregados(agregados(alumnos));
        }

        switch (rol) {
            case "ESTUDIANTE" -> doctorando(persona, alumnos, b);
            case "ASESOR" -> asesor(persona, alumnos, b);
            case "REVISOR" -> revisor(persona, b);
            case "SECRETARIA" -> secretaria(alumnos, b);
            case "COORDINADOR" -> coordinador(alumnos, b);
            case "TUTOR" -> tutor(persona, alumnos, b);
            default -> b.subtitulo("Así va el proceso de titulación del programa.")
                    .metricas(List.of()).pendientes(List.of())
                    .graficos(List.of(gEtapas(alumnos, "Doctorandos por etapa", "el proceso completo"),
                            gLineas(alumnos)));
        }
        return b.build();
    }

    @Override
    @Transactional
    public byte[] reporteExcel() {
        return PanelExcel.generar(panel());
    }

    // ── Bloques personales ───────────────────────────────────────────────────

    /** El doctorando: en qué etapa está y qué le falta ahora. */
    private void doctorando(Persona persona, List<SeguimientoAlumnoItem> alumnos,
                            PanelResponse.PanelResponseBuilder b) {
        SeguimientoAlumnoItem yo = persona == null ? null : alumnos.stream()
                .filter(a -> persona.getId().equals(a.getEstudianteId()))
                .findFirst().orElse(null);
        if (yo == null) {
            b.subtitulo("Aún no tienes un tema de tesis registrado.")
                    .metricas(List.of()).pendientes(List.of());
            return;
        }
        b.subtitulo("Tu tesis, paso a paso.")
                .miEtapa(yo.getEtapaNumero())
                .miEtapaTitulo(yo.getEtapaTitulo())
                .miAvancePct(yo.getAvancePct());

        List<Metrica> metricas = new ArrayList<>();
        metricas.add(Metrica.builder()
                .etiqueta("Mi etapa").valor(yo.getEtapaNumero() + " de " + EtapasProceso.TOTAL)
                .detalle(yo.getEtapaTitulo()).icono("route").tono("granate")
                .link("/admin/mi-tesis/avance").build());
        metricas.add(Metrica.builder()
                .etiqueta("Avance del proceso").valor(yo.getAvancePct() + "%")
                .detalle(yo.getAvancePct() >= 50 ? "Vas más de la mitad" : "Sigue avanzando")
                .icono("trending-up").tono("cielo").link("/admin/mi-tesis/proyecto").build());
        metricas.add(Metrica.builder()
                .etiqueta("Mi asesor").valor(yo.getAsesorNombre() != null ? "Designado" : "Sin designar")
                .detalle(yo.getAsesorNombre() != null ? yo.getAsesorNombre() : "Solicítalo a un asesor sugerido")
                .icono("handshake").tono(yo.getAsesorNombre() != null ? "esmeralda" : "ambar")
                .link("/admin/mi-tesis/asesoria").build());
        b.metricas(metricas);

        List<Pendiente> pend = new ArrayList<>();
        if (yo.getPendiente() != null && !yo.getPendiente().isBlank()) {
            boolean mio = "ESTUDIANTE".equalsIgnoreCase(yo.getResponsable());
            pend.add(Pendiente.builder()
                    .prioridad(mio ? "ALTA" : "INFO")
                    .titulo(yo.getPendiente())
                    .detalle(mio ? "Te toca a ti" : "Responsable: " + etiquetaResponsable(yo.getResponsable()))
                    .icono(mio ? "flag" : "clock")
                    .link(mio ? yo.getAccionLink() : null).build());
        }
        if (yo.getDiasEnEtapa() != null && yo.getDiasEnEtapa() > 30) {
            pend.add(Pendiente.builder().prioridad("MEDIA")
                    .titulo("Tu expediente lleva " + yo.getDiasEnEtapa() + " días sin movimiento")
                    .detalle("Consulta con tu asesor o con Secretaría")
                    .icono("triangle-alert").link("/admin/mi-tesis/avance").build());
        }
        b.pendientes(pend);

        // Sus gráficos: su avance, su recorrido y sus notas. Nada de otros doctorandos.
        List<PanelResponse.Grafico> gs = new ArrayList<>();
        gs.add(gMiAvance(yo));
        if (yo.getTesisId() != null) {
            var proy = proyectoRepository.buscarPorTesisId(yo.getTesisId()).orElse(null);
            if (proy != null) {
                var notas = gMisNotas(proy.getId());
                if (notas != null) gs.add(notas);
            }
        }
        gs.add(gMisHitos(yo));
        b.graficos(gs);
    }

    /** El asesor: sus asesorados y qué espera de él cada uno. */
    private void asesor(Persona persona, List<SeguimientoAlumnoItem> alumnos,
                        PanelResponse.PanelResponseBuilder b) {
        UUID id = persona != null ? persona.getId() : null;
        List<Object[]> filas = id != null
                ? proyectoRepository.bandejaDeAsesor(id, null, null, 0, 100) : List.of();
        long total = filas.size();
        long porRevisar = filas.stream().filter(r -> "EN_REVISION".equals(str(r[2]))).count();
        long observados = filas.stream().filter(r -> "OBSERVADO".equals(str(r[2]))).count();
        long conformes = filas.stream().filter(r -> "CONFORME".equals(str(r[2]))).count();

        b.subtitulo("Tus asesorados y lo que esperan de ti.")
                .metricas(List.of(
                        Metrica.builder().etiqueta("Mis asesorados").valor(String.valueOf(total))
                                .detalle("proyectos a tu cargo").icono("users-round").tono("granate")
                                .link("/admin/revision-proyecto").build(),
                        Metrica.builder().etiqueta("Por revisar").valor(String.valueOf(porRevisar))
                                .detalle("enviados a tu revisión").icono("file-search")
                                .tono(porRevisar > 0 ? "ambar" : "esmeralda")
                                .link("/admin/revision-proyecto").build(),
                        Metrica.builder().etiqueta("Listos para la carta").valor(String.valueOf(conformes))
                                .detalle("con todos los ítems conformes").icono("badge-check").tono("esmeralda")
                                .link("/admin/revision-proyecto").build()));

        List<Pendiente> pend = new ArrayList<>();
        if (porRevisar > 0) {
            pend.add(Pendiente.builder().prioridad("ALTA")
                    .titulo(porRevisar + " proyecto(s) esperando tu revisión")
                    .detalle("El estudiante ya lo envió").icono("file-search")
                    .link("/admin/revision-proyecto").build());
        }
        if (conformes > 0) {
            pend.add(Pendiente.builder().prioridad("MEDIA")
                    .titulo(conformes + " proyecto(s) listos para tu carta de opinión favorable")
                    .detalle("Sin observaciones pendientes").icono("stamp")
                    .link("/admin/revision-proyecto").build());
        }
        if (observados > 0) {
            pend.add(Pendiente.builder().prioridad("INFO")
                    .titulo(observados + " con observaciones tuyas")
                    .detalle("Esperando que el estudiante corrija").icono("clock")
                    .link("/admin/revision-proyecto").build());
        }
        b.pendientes(pend);

        // Sus asesorados por estado + en qué etapa están (solo los suyos).
        Map<String, Long> estados = new LinkedHashMap<>();
        estados.put("En elaboración", total - porRevisar - observados - conformes);
        estados.put("Por revisar", porRevisar);
        estados.put("Observados", observados);
        estados.put("Conformes", conformes);
        List<UUID> misTesis = filas.stream().map(r -> (UUID) r[0]).toList();
        b.graficos(List.of(
                gEstados("misasesorados", "Mis asesorados", "estado de cada proyecto", estados,
                        List.of("#cbd5e1", "#f59e0b", "#e11d48", "#059669")),
                gEtapas(alumnos.stream().filter(a -> misTesis.contains(a.getTesisId())).toList(),
                        "Mis asesorados por etapa", "dónde está cada uno")));
    }

    /** El revisor: sus evaluaciones de rúbrica. */
    private void revisor(Persona persona, PanelResponse.PanelResponseBuilder b) {
        UUID id = persona != null ? persona.getId() : null;
        // Fila: [tesisId, proyectoId, revisorId, estadoRevisor, puntaje, ...]
        List<Object[]> mios = id != null ? revisorRepository.bandejaDeRevisor(id) : List.of();
        long pendientes = mios.stream().filter(r -> r[3] == null || "DESIGNADO".equals(str(r[3]))).count();
        long observados = mios.stream().filter(r -> "OBSERVADO".equals(str(r[3]))).count();
        long conformes = mios.stream().filter(r -> "CONFORME".equals(str(r[3]))).count();

        b.subtitulo("Los proyectos que te toca evaluar.")
                .metricas(List.of(
                        Metrica.builder().etiqueta("Asignados").valor(String.valueOf(mios.size()))
                                .detalle("como revisor").icono("clipboard-check").tono("granate")
                                .link("/admin/revisor-proyecto").build(),
                        Metrica.builder().etiqueta("Por evaluar").valor(String.valueOf(pendientes))
                                .detalle("aún sin rúbrica llenada").icono("file-search")
                                .tono(pendientes > 0 ? "ambar" : "esmeralda")
                                .link("/admin/revisor-proyecto").build(),
                        Metrica.builder().etiqueta("Conformes").valor(String.valueOf(conformes))
                                .detalle("evaluaciones cerradas").icono("badge-check").tono("esmeralda")
                                .link("/admin/revisor-proyecto").build()));

        List<Pendiente> pend = new ArrayList<>();
        if (pendientes > 0) {
            pend.add(Pendiente.builder().prioridad("ALTA")
                    .titulo(pendientes + " proyecto(s) por evaluar con la rúbrica")
                    .detalle("Plazo: 15 días útiles").icono("clipboard-check")
                    .link("/admin/revisor-proyecto").build());
        }
        if (observados > 0) {
            pend.add(Pendiente.builder().prioridad("MEDIA")
                    .titulo(observados + " esperando que el estudiante subsane")
                    .detalle("Revisa cuando levante las observaciones").icono("clock")
                    .link("/admin/revisor-proyecto").build());
        }
        b.pendientes(pend);

        Map<String, Long> estados = new LinkedHashMap<>();
        estados.put("Por evaluar", pendientes);
        estados.put("Observados", observados);
        estados.put("Conformes", conformes);
        List<PanelResponse.Grafico> gs = new ArrayList<>();
        gs.add(gEstados("misevaluaciones", "Mis evaluaciones", "estado de cada rúbrica", estados,
                List.of("#f59e0b", "#e11d48", "#059669")));
        // Los puntajes que él mismo puso (columna 4 de la bandeja).
        List<String> cats = new ArrayList<>();
        List<Long> datos = new ArrayList<>();
        int n = 1;
        for (Object[] r : mios) {
            if (r[4] == null) continue;
            cats.add("Proyecto " + n++);
            datos.add(Long.parseLong(str(r[4])));
        }
        if (!cats.isEmpty()) {
            gs.add(PanelResponse.Grafico.builder().id("mispuntajes")
                    .titulo("Puntajes que he asignado").subtitulo("sobre 100 · aprueba con 65")
                    .tipo("barras").categorias(cats)
                    .series(List.of(PanelResponse.Serie.builder().nombre("Puntaje").datos(datos).build()))
                    .colores(List.of("#0369a1")).build());
        }
        b.graficos(gs);
    }

    /** La Secretaría: su cola de trámite. */
    private void secretaria(List<SeguimientoAlumnoItem> alumnos, PanelResponse.PanelResponseBuilder b) {
        long porElaborar = dictamenRepository.contarPorEstado("POR_ELABORAR");
        long sinFirma = dictamenRepository.contarPorEstado("ELABORADO");
        long porRecibir = alumnos.stream().filter(a -> "Recepcionar el expediente".equalsIgnoreCase(a.getPendiente())).count();
        long mios = alumnos.stream().filter(SeguimientoAlumnoItem::isPendienteSecretaria).count();

        b.subtitulo("Tu cola de trámite, ordenada por urgencia.")
                .metricas(List.of(
                        Metrica.builder().etiqueta("Pendientes tuyos").valor(String.valueOf(mios))
                                .detalle("expedientes esperando a Secretaría").icono("inbox")
                                .tono(mios > 0 ? "granate" : "esmeralda")
                                .link("/admin/seguimiento-alumnos").build(),
                        Metrica.builder().etiqueta("Dictámenes por elaborar").valor(String.valueOf(porElaborar))
                                .detalle("con los firmados completos").icono("stamp")
                                .tono(porElaborar > 0 ? "ambar" : "esmeralda")
                                .link("/admin/dictamenes").build(),
                        Metrica.builder().etiqueta("Sin la firma del Director").valor(String.valueOf(sinFirma))
                                .detalle("elaborados, falta subir el firmado").icono("file-check")
                                .tono(sinFirma > 0 ? "ambar" : "esmeralda")
                                .link("/admin/dictamenes").build()));

        List<Pendiente> pend = new ArrayList<>();
        if (porElaborar > 0) {
            pend.add(Pendiente.builder().prioridad("ALTA")
                    .titulo(porElaborar + " dictamen(es) de designación por elaborar")
                    .detalle("El estudiante ya subió sus dos firmados").icono("stamp")
                    .link("/admin/dictamenes").build());
        }
        if (sinFirma > 0) {
            pend.add(Pendiente.builder().prioridad("MEDIA")
                    .titulo(sinFirma + " dictamen(es) esperando la firma del Director")
                    .detalle("Súbelos escaneados al recibirlos").icono("upload")
                    .link("/admin/dictamenes").build());
        }
        if (porRecibir > 0) {
            pend.add(Pendiente.builder().prioridad("MEDIA")
                    .titulo(porRecibir + " expediente(s) por recepcionar")
                    .detalle("El doctorando solicitó la aprobación").icono("inbox")
                    .link("/admin/secretaria-defensa").build());
        }
        long detenidos = alumnos.stream().filter(a -> a.getDiasEnEtapa() != null && a.getDiasEnEtapa() > 30).count();
        if (detenidos > 0) {
            pend.add(Pendiente.builder().prioridad("INFO")
                    .titulo(detenidos + " doctorando(s) sin movimiento hace más de 30 días")
                    .detalle("Revisa a quién hay que empujar").icono("triangle-alert")
                    .link("/admin/seguimiento-alumnos").build());
        }
        b.pendientes(pend);

        b.graficos(List.of(
                gEtapas(alumnos, "Doctorandos por etapa", "el proceso completo"),
                gDictamenes(),
                gAntiguedad(alumnos),
                gResponsables(alumnos)));
    }

    /** El coordinador: designaciones que dependen de él. */
    private void coordinador(List<SeguimientoAlumnoItem> alumnos, PanelResponse.PanelResponseBuilder b) {
        long sinTema = alumnos.stream().filter(a -> a.getTesisId() == null).count();
        long sinRevisores = alumnos.stream()
                .filter(a -> a.getPendiente() != null && a.getPendiente().toLowerCase().contains("revisor")).count();
        long mios = alumnos.stream().filter(a -> "COORDINADOR".equalsIgnoreCase(a.getResponsable())).count();

        b.subtitulo("Las designaciones que dependen de ti.")
                .metricas(List.of(
                        Metrica.builder().etiqueta("Esperándote").valor(String.valueOf(mios))
                                .detalle("expedientes con acción tuya").icono("inbox")
                                .tono(mios > 0 ? "granate" : "esmeralda")
                                .link("/admin/seguimiento-alumnos").build(),
                        Metrica.builder().etiqueta("Sin tema registrado").valor(String.valueOf(sinTema))
                                .detalle("doctorandos por inscribir").icono("book-open")
                                .tono(sinTema > 0 ? "ambar" : "esmeralda")
                                .link("/admin/registro-tema").build(),
                        Metrica.builder().etiqueta("Revisores por designar").valor(String.valueOf(sinRevisores))
                                .detalle("proyectos recepcionados").icono("users-round")
                                .tono(sinRevisores > 0 ? "ambar" : "esmeralda")
                                .link("/admin/coordinador-proyecto").build()));

        List<Pendiente> pend = new ArrayList<>();
        if (sinRevisores > 0) {
            pend.add(Pendiente.builder().prioridad("ALTA")
                    .titulo(sinRevisores + " proyecto(s) esperan la designación de revisores")
                    .detalle("Dos revisores de la línea del tema").icono("users-round")
                    .link("/admin/coordinador-proyecto").build());
        }
        if (sinTema > 0) {
            pend.add(Pendiente.builder().prioridad("MEDIA")
                    .titulo(sinTema + " doctorando(s) sin tema registrado")
                    .detalle("El proceso no puede empezar sin el tema").icono("book-open")
                    .link("/admin/registro-tema").build());
        }
        b.pendientes(pend);

        b.graficos(List.of(
                gEtapas(alumnos, "Doctorandos por etapa", "el proceso completo"),
                gLineas(alumnos),
                gAntiguedad(alumnos),
                gResponsables(alumnos)));
    }

    /** El tutor: sus tutorandos y las sugerencias de asesor. */
    private void tutor(Persona persona, List<SeguimientoAlumnoItem> alumnos,
                       PanelResponse.PanelResponseBuilder b) {
        UUID id = persona != null ? persona.getId() : null;
        List<SeguimientoAlumnoItem> mios = id == null ? List.of()
                : alumnos.stream().filter(a -> a.getTutorNombre() != null).toList();
        long sinAsesor = mios.stream().filter(a -> a.getAsesorNombre() == null).count();

        b.subtitulo("Tus tutorandos y su avance.")
                .metricas(List.of(
                        Metrica.builder().etiqueta("Tutorandos").valor(String.valueOf(mios.size()))
                                .detalle("a tu cargo").icono("graduation-cap").tono("granate")
                                .link("/admin/mis-tutorandos").build(),
                        Metrica.builder().etiqueta("Sin asesor").valor(String.valueOf(sinAsesor))
                                .detalle("necesitan que sugieras uno").icono("user-round-plus")
                                .tono(sinAsesor > 0 ? "ambar" : "esmeralda")
                                .link("/admin/mis-tutorandos").build()));

        List<Pendiente> pend = new ArrayList<>();
        if (sinAsesor > 0) {
            pend.add(Pendiente.builder().prioridad("ALTA")
                    .titulo(sinAsesor + " tutorando(s) sin asesor sugerido")
                    .detalle("Sin tu sugerencia no pueden solicitar asesoría").icono("user-round-plus")
                    .link("/admin/mis-tutorandos").build());
        }
        b.pendientes(pend);

        b.graficos(List.of(
                gEtapas(mios, "Mis tutorandos por etapa", "en qué punto va cada uno"),
                gAntiguedad(mios)));
    }

    // ── Gráficos ─────────────────────────────────────────────────────────────

    /** Embudo de las 8 etapas: cuántos doctorandos hay en cada una. */
    private PanelResponse.Grafico gEtapas(List<SeguimientoAlumnoItem> alumnos, String titulo, String sub) {
        long[] c = new long[EtapasProceso.TOTAL + 1];
        for (SeguimientoAlumnoItem a : alumnos) {
            int e = Math.min(Math.max(a.getEtapaNumero(), 1), EtapasProceso.TOTAL);
            c[e]++;
        }
        List<String> cats = new ArrayList<>();
        List<Long> datos = new ArrayList<>();
        for (int i = 1; i <= EtapasProceso.TOTAL; i++) {
            cats.add(i + ". " + EtapasProceso.corto(i));
            datos.add(c[i]);
        }
        return PanelResponse.Grafico.builder()
                .id("etapas").titulo(titulo).subtitulo(sub).tipo("barrasH").ancho(true)
                .categorias(cats)
                .series(List.of(PanelResponse.Serie.builder().nombre("Doctorandos").datos(datos).build()))
                .colores(List.of("#8C1D2E"))
                .build();
    }

    /** Distribución por línea de investigación (las 6 con más doctorandos). */
    private PanelResponse.Grafico gLineas(List<SeguimientoAlumnoItem> alumnos) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (SeguimientoAlumnoItem a : alumnos) {
            if (a.getLineaNombre() != null && !a.getLineaNombre().isBlank()) {
                m.merge(a.getLineaNombre(), 1L, Long::sum);
            }
        }
        List<Map.Entry<String, Long>> top = m.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(6).toList();
        return PanelResponse.Grafico.builder()
                .id("lineas").titulo("Por línea de investigación").subtitulo("dónde se concentran los temas")
                .tipo("dona")
                .categorias(top.stream().map(Map.Entry::getKey).toList())
                .series(List.of(PanelResponse.Serie.builder().nombre("Doctorandos")
                        .datos(top.stream().map(Map.Entry::getValue).toList()).build()))
                .build();
    }

    /** Cuánto tiempo llevan quietos los expedientes. */
    private PanelResponse.Grafico gAntiguedad(List<SeguimientoAlumnoItem> alumnos) {
        long alDia = 0, medio = 0, tarde = 0, sinDatos = 0;
        for (SeguimientoAlumnoItem a : alumnos) {
            Integer d = a.getDiasEnEtapa();
            if (d == null) sinDatos++;
            else if (d <= 30) alDia++;
            else if (d <= 60) medio++;
            else tarde++;
        }
        return PanelResponse.Grafico.builder()
                .id("antiguedad").titulo("Tiempo sin movimiento").subtitulo("desde el último hito registrado")
                .tipo("dona")
                .categorias(List.of("Al día (≤30 d)", "31 a 60 días", "Más de 60 días", "Sin hitos"))
                .series(List.of(PanelResponse.Serie.builder().nombre("Doctorandos")
                        .datos(List.of(alDia, medio, tarde, sinDatos)).build()))
                .colores(List.of("#059669", "#f59e0b", "#e11d48", "#cbd5e1"))
                .build();
    }

    /** Quién tiene la pelota: reparto de responsables de lo pendiente. */
    private PanelResponse.Grafico gResponsables(List<SeguimientoAlumnoItem> alumnos) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (SeguimientoAlumnoItem a : alumnos) {
            String r = etiquetaResponsable(a.getResponsable());
            if (!"—".equals(r)) m.merge(r, 1L, Long::sum);
        }
        return PanelResponse.Grafico.builder()
                .id("responsables").titulo("Quién tiene la pelota").subtitulo("responsable del siguiente paso")
                .tipo("barras")
                .categorias(new ArrayList<>(m.keySet()))
                .series(List.of(PanelResponse.Serie.builder().nombre("Expedientes")
                        .datos(new ArrayList<>(m.values())).build()))
                .colores(List.of("#0369a1"))
                .build();
    }

    /** Estado de los dictámenes de designación. */
    private PanelResponse.Grafico gDictamenes() {
        return PanelResponse.Grafico.builder()
                .id("dictamenes").titulo("Dictámenes de designación").subtitulo("estado de la cola")
                .tipo("dona")
                .categorias(List.of("Sin redactar", "Falta la firma", "Firmados", "Observados"))
                .series(List.of(PanelResponse.Serie.builder().nombre("Dictámenes").datos(List.of(
                        dictamenRepository.contarPorEstado("POR_ELABORAR"),
                        dictamenRepository.contarPorEstado("ELABORADO"),
                        dictamenRepository.contarPorEstado("FIRMADO"),
                        dictamenRepository.contarPorEstado("OBSERVADO"))).build()))
                .colores(List.of("#8C1D2E", "#f59e0b", "#059669", "#e11d48"))
                .build();
    }

    /** El avance del propio doctorando sobre las 8 etapas. */
    private PanelResponse.Grafico gMiAvance(SeguimientoAlumnoItem yo) {
        return PanelResponse.Grafico.builder()
                .id("miavance").titulo("Mi avance").subtitulo("etapas completadas del proceso")
                .tipo("radial")
                .categorias(List.of("Etapa " + yo.getEtapaNumero() + " de " + EtapasProceso.TOTAL))
                .series(List.of(PanelResponse.Serie.builder().nombre("Avance")
                        .datos(List.of((long) yo.getAvancePct())).build()))
                .colores(List.of("#8C1D2E"))
                .build();
    }

    /** Los hitos del propio doctorando: qué lleva hecho y qué falta. */
    private PanelResponse.Grafico gMisHitos(SeguimientoAlumnoItem yo) {
        int etapa = yo.getEtapaNumero();
        List<String> cats = new ArrayList<>();
        List<Long> datos = new ArrayList<>();
        for (int i = 1; i <= EtapasProceso.TOTAL; i++) {
            cats.add(i + ". " + EtapasProceso.corto(i));
            datos.add(i < etapa ? 100L : i == etapa ? Math.max(10, yo.getAvancePct()) : 0L);
        }
        return PanelResponse.Grafico.builder()
                .id("mishitos").titulo("Mi recorrido").subtitulo("etapas superadas y la actual").ancho(true)
                .tipo("barrasH")
                .categorias(cats)
                .series(List.of(PanelResponse.Serie.builder().nombre("Completado %").datos(datos).build()))
                .colores(List.of("#8C1D2E"))
                .build();
    }

    /** Notas que los revisores le pusieron al doctorando. */
    private PanelResponse.Grafico gMisNotas(UUID proyectoId) {
        var revs = revisorRepository.listarPorProyecto(proyectoId);
        List<String> cats = new ArrayList<>();
        List<Long> datos = new ArrayList<>();
        for (var r : revs) {
            if (r.getPuntajeTotal() == null) continue;
            cats.add("Revisor " + (r.getOrden() != null ? r.getOrden() : cats.size() + 1));
            datos.add((long) r.getPuntajeTotal());
        }
        if (cats.isEmpty()) return null;
        return PanelResponse.Grafico.builder()
                .id("misnotas").titulo("Mis evaluaciones").subtitulo("puntaje sobre 100 · aprueba con 65")
                .tipo("barras")
                .categorias(cats)
                .series(List.of(PanelResponse.Serie.builder().nombre("Puntaje").datos(datos).build()))
                .colores(List.of("#0369a1"))
                .build();
    }

    /** Reparto por estado de una bandeja (asesor / revisor). */
    private PanelResponse.Grafico gEstados(String id, String titulo, String sub,
                                           Map<String, Long> conteos, List<String> colores) {
        return PanelResponse.Grafico.builder()
                .id(id).titulo(titulo).subtitulo(sub).tipo("dona")
                .categorias(new ArrayList<>(conteos.keySet()))
                .series(List.of(PanelResponse.Serie.builder().nombre("Proyectos")
                        .datos(new ArrayList<>(conteos.values())).build()))
                .colores(colores)
                .build();
    }

    // ── Agregados (solo conteos) ─────────────────────────────────────────────

    private PanelResponse.Agregados agregados(List<SeguimientoAlumnoItem> alumnos) {
        Map<Integer, Long> porEtapa = new LinkedHashMap<>();
        for (int i = 1; i <= EtapasProceso.TOTAL; i++) porEtapa.put(i, 0L);
        Map<String, Long> porLinea = new LinkedHashMap<>();
        long detenidos = 0, enRevision = 0, sustentados = 0;

        for (SeguimientoAlumnoItem a : alumnos) {
            int etapa = Math.min(Math.max(a.getEtapaNumero(), 1), EtapasProceso.TOTAL);
            porEtapa.merge(etapa, 1L, Long::sum);
            if (a.getDiasEnEtapa() != null && a.getDiasEnEtapa() > 30) detenidos++;
            if (a.getEtapaNumero() == 4 || a.getEtapaNumero() == 5) enRevision++;
            if (a.getEtapaNumero() > EtapasProceso.TOTAL) sustentados++;
            String linea = a.getLineaNombre();
            if (linea != null && !linea.isBlank()) porLinea.merge(linea, 1L, Long::sum);
        }

        List<Conteo> etapas = new ArrayList<>();
        porEtapa.forEach((n, c) -> etapas.add(Conteo.builder()
                .etiqueta(EtapasProceso.corto(n)).valor(c).orden(n).build()));

        List<Conteo> lineas = porLinea.entrySet().stream()
                .map(e -> Conteo.builder().etiqueta(e.getKey()).valor(e.getValue()).build())
                .sorted(Comparator.comparingLong(Conteo::getValor).reversed())
                .limit(6).toList();

        return PanelResponse.Agregados.builder()
                .doctorandosActivos(alumnos.size())
                .enRevision(enRevision)
                .detenidos(detenidos)
                .sustentados(sustentados)
                .docentes(docenteRepository.contarActivos())
                .lineas(porLinea.size())
                .porEtapa(etapas)
                .porLinea(lineas)
                .porAnio(List.of())
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Persona personaActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        return userId == null ? null : personaRepository.findByUserId(userId).orElse(null);
    }

    private Set<String> roles() {
        try {
            return Set.of(securityUtils.getCurrentUserRoles());
        } catch (Exception e) {
            return Set.of();
        }
    }

    /**
     * Con qué sombrero se arma el panel. Se prioriza el rol que MÁS trabajo implica en el sistema,
     * no la jerarquía: quien es asesor y además docente quiere ver sus asesorados.
     */
    private String rolPrincipal(Set<String> roles, Persona persona) {
        if (roles.contains("ESTUDIANTE")) return "ESTUDIANTE";
        if (roles.contains("SECRETARIA")) return "SECRETARIA";
        if (roles.contains("COORDINADOR") || roles.contains("COORD_PROG") || roles.contains("COORD_SEC")) return "COORDINADOR";
        UUID id = persona != null ? persona.getId() : null;
        if (id != null && !revisorRepository.bandejaDeRevisor(id).isEmpty()) return "REVISOR";
        if (roles.contains("ASESOR")) return "ASESOR";
        if (roles.contains("PROF_TUTOR")) return "TUTOR";
        if (roles.contains("REVISOR")) return "REVISOR";
        return roles.contains("ADMIN") ? "SECRETARIA" : "DOCENTE";
    }

    private String rolLabel(String rol) {
        return switch (rol) {
            case "ESTUDIANTE" -> "Doctorando";
            case "ASESOR" -> "Asesor";
            case "REVISOR" -> "Revisor";
            case "SECRETARIA" -> "Secretaría";
            case "COORDINADOR" -> "Coordinación";
            case "TUTOR" -> "Tutor";
            default -> "Docente";
        };
    }

    private String etiquetaResponsable(String r) {
        if (r == null) return "—";
        return switch (r.toUpperCase()) {
            case "SECRETARIA" -> "Secretaría";
            case "COORDINADOR" -> "Coordinador";
            case "ASESOR" -> "Asesor";
            case "REVISOR" -> "Revisores";
            case "JURADO" -> "Jurado";
            case "ESTUDIANTE" -> "Tú";
            default -> "—";
        };
    }

    private String saludo() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Buenos días" : h < 19 ? "Buenas tardes" : "Buenas noches";
    }

    private String primerNombre(String nombres) {
        String[] p = nombres.trim().split("\\s+");
        return p.length > 0 ? p[0] : nombres;
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
