package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.application.util.ProyectoDefinicion;
import unmsm.edu.pe.tesis.application.util.RubricaDefinicion;
import unmsm.edu.pe.tesis.domain.entities.*;
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.RevisorProyectoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RevisorProyectoServiceImpl implements RevisorProyectoService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject ProyectoRubricaPuntajeRepository puntajeRepository;
    @Inject ProyectoRevisionRepository revisionRepository;
    @Inject ProyectoRevisionEventoRepository eventoRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject ProyectoReferenciaRepository referenciaRepository;
    @Inject unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos almacenamiento;
    @Inject unmsm.edu.pe.tesis.infrastructure.export.ProyectoDocumentoExporter documentoExporter;
    @Inject ProyectoEditorAssembler assembler;
    @Inject AsesorDesignadoService asesorDesignado;
    @Inject unmsm.edu.pe.tesis.domain.repositories.PlantillaRubricaRepository plantillaRepository;
    @Inject unmsm.edu.pe.tesis.domain.services.PlantillaRubricaService plantillaService;

    /** Tipo de documento: rúbrica oficial de revisores (Excel) que sube Secretaría. */
    public static final String T_RUBRICA = "RUBRICA_REVISOR";

    /** Ítems observados por el revisor a la espera de que el estudiante los corrija. */
    private static final Set<EstadoItemRevision> ESPERANDO_ESTUDIANTE = Set.of(
            EstadoItemRevision.OBSERVADO, EstadoItemRevision.EN_CORRECCION);

    @Override
    @Transactional
    public List<RevisorBandejaItem> bandeja() {
        UUID docenteId = docenteActual().getPersonaId();
        List<RevisorBandejaItem> out = new ArrayList<>();
        for (Object[] r : revisorRepository.bandejaDeRevisor(docenteId)) {
            out.add(RevisorBandejaItem.builder()
                    .tesisId((UUID) r[0])
                    .proyectoId((UUID) r[1])
                    .estudianteApellidos((asStr(r[5]) + " " + asStr(r[6])).trim())
                    .estudianteNombres(asStr(r[7]))
                    .codigoSistema(asStr(r[8]))
                    .programaNombre(asStr(r[9]))
                    .tituloTesis(asStr(r[10]))
                    .fechaRecepcion(toLocalDate(r[11]))
                    .miEstado(asStr(r[3]))
                    .miPuntaje(r[4] != null ? ((Number) r[4]).intValue() : null)
                    .build());
        }
        return out;
    }

    @Override
    @Transactional
    public EvaluacionRevisorResponse detalle(UUID tesisId) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);

        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        ProyectoEditorResponse proyecto = assembler.armar(p, tesis, estudianteDe(tesisId), asesorNombre(tesisId), "REVISOR");

        RubricaDefinicion.Rubrica rubrica = RubricaDefinicion.porEnfoque(
                p.getEnfoque() != null ? p.getEnfoque().name() : null);

        Map<String, ProyectoRubricaPuntaje> mis = new java.util.HashMap<>();
        for (ProyectoRubricaPuntaje pj : puntajeRepository.listarPorRevisor(rv.getId())) {
            mis.put(pj.getCriterio(), pj);
        }
        boolean evaluado = !mis.isEmpty();

        int totalObtenido = 0;
        List<RubricaSeccionItem> secciones = new ArrayList<>();
        for (RubricaDefinicion.Seccion s : rubrica.secciones()) {
            List<RubricaCriterioItem> crits = new ArrayList<>();
            int subObt = 0;
            for (RubricaDefinicion.Criterio c : s.criterios()) {
                ProyectoRubricaPuntaje pj = mis.get(c.key());
                Integer pt = pj != null ? pj.getPuntaje() : null;
                if (pt != null) subObt += pt;
                // Qué corrigió el estudiante en el ítem que corresponde a este criterio.
                String[] correccion = pj != null && pj.getObservacion() != null
                        ? ultimaCorreccionDelItem(p, c.key()) : null;
                crits.add(RubricaCriterioItem.builder()
                        .key(c.key()).titulo(c.titulo()).descripcion(c.descripcion())
                        .cumple(c.cumple()).parcial(c.parcial()).noCumple(c.noCumple())
                        .nivel(pj != null ? pj.getNivel() : null).puntaje(pt)
                        .observacion(pj != null ? pj.getObservacion() : null)
                        .correccionEstudiante(correccion != null ? correccion[0] : null)
                        .correccionFecha(correccion != null ? correccion[1] : null)
                        .build());
            }
            totalObtenido += subObt;
            secciones.add(RubricaSeccionItem.builder()
                    .key(s.key()).titulo(s.titulo()).subtotalMaximo(s.subtotal())
                    .subtotalObtenido(evaluado ? subObt : null).criterios(crits)
                    .build());
        }

        // Habilitada por la Secretaría (rúbrica oficial del sistema) o, en expedientes antiguos,
        // por el Word que se subía proyecto a proyecto.
        var docRubrica = documentoTesisRepository.buscarPorTesisYTipo(tesisId, T_RUBRICA);
        boolean habilitada = Boolean.TRUE.equals(p.getRubricaHabilitada()) || docRubrica.isPresent();

        return EvaluacionRevisorResponse.builder()
                .proyecto(proyecto)
                .rubricaEnfoque(rubrica.enfoque()).rubricaTitulo(rubrica.titulo())
                .secciones(secciones)
                .miEstado(rv.getEstado() != null ? rv.getEstado().name() : null)
                .miComentario(rv.getComentario())
                .miPuntajeTotal(evaluado ? totalObtenido : null)
                .puntajeMaximo(rubrica.total())
                .aprobadoMin(RubricaDefinicion.APROBADO_MIN)
                .aprobado(evaluado ? RubricaDefinicion.aprobado(totalObtenido) : null)
                .cerrada(rv.getEstado() == EstadoRevisor.CONFORME || etapaAvanzada(p))
                .rubricaDisponible(habilitada)
                .rubricaNombreArchivo(docRubrica.map(DocumentoTesis::getNombreOriginal)
                        .orElseGet(() -> plantillaRepository.vigente(rubrica.enfoque())
                                .map(pl -> pl.getNombreOriginal()).orElse(null)))
                .respuestaEstudiante(rv.getRespuestaEstudiante())
                .build();
    }

    @Override
    @Transactional
    public ArchivoDescargable descargarRubrica(UUID tesisId) {
        // Primero el Word propio del expediente (modo anterior); si no, la rúbrica oficial vigente.
        var doc = documentoTesisRepository.buscarPorTesisYTipo(tesisId, T_RUBRICA);
        if (doc.isPresent()) {
            byte[] bytes = almacenamiento.obtener(doc.get().getStorageKey());
            return new ArchivoDescargable(bytes, doc.get().getContentType(), doc.get().getNombreOriginal());
        }
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        return plantillaService.documentoVigente(
                p.getEnfoque() != null ? p.getEnfoque().name() : "CUANTITATIVO");
    }

    @Override
    @Transactional
    public ArchivoDescargable descargarRubricaLlenada(UUID tesisId) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);
        RubricaDefinicion.Rubrica rubrica = RubricaDefinicion.porEnfoque(
                p.getEnfoque() != null ? p.getEnfoque().name() : null);
        Map<String, String> niveles = new java.util.HashMap<>();
        Map<String, String> observaciones = new java.util.HashMap<>();
        int total = 0;
        for (ProyectoRubricaPuntaje pj : puntajeRepository.listarPorRevisor(rv.getId())) {
            niveles.put(pj.getCriterio(), pj.getNivel());
            if (pj.getObservacion() != null) observaciones.put(pj.getCriterio(), pj.getObservacion());
            total += pj.getPuntaje() != null ? pj.getPuntaje() : 0;
        }
        Estudiante est = estudianteDe(tesisId);
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElse(null);
        byte[] docx = unmsm.edu.pe.tesis.infrastructure.export.RubricaLlenadaWord.generar(
                rubrica, niveles, observaciones, total, RubricaDefinicion.aprobado(total),
                nombre(docente.getPersona()), rv.getComentario(),
                est != null ? nombre(est.getPersona()) : null,
                tesis != null ? tesis.getTitulo() : null);
        return new ArchivoDescargable(docx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "rubrica-evaluada.docx");
    }

    @Override
    @Transactional
    public void observarItem(UUID tesisId, ObservarItemRequest req) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);
        verificarRevisorHabilitado(p);
        verificarRubricaDisponible(tesisId);
        if (rv.getEstado() == EstadoRevisor.CONFORME) {
            throw new BusinessException("Ya diste conformidad a este proyecto; la evaluación está cerrada");
        }
        if (req == null || req.getCampo() == null || req.getCampo().isBlank()
                || req.getTexto() == null || req.getTexto().isBlank()) {
            throw new ValidationException("Indica el campo y el texto de la observación");
        }
        // Reutiliza el hilo por campo; el evento lleva rol REVISOR para que el estudiante lo vea como
        // "informe de revisor" y el asesor no lo vea en su propia vista.
        registrarObservacionDeCampo(p, docente, req.getCampo(), req.getTexto().trim());
        // El revisor con observaciones abiertas queda OBSERVADO (no conforme) y se limpia su levantamiento previo.
        rv.setEstado(EstadoRevisor.OBSERVADO);
        rv.setRespuestaEstudiante(null);
        rv.setFechaRespuesta(null);
        revisorRepository.save(rv);
        // Una nueva observación revierte la conformidad global del conjunto de revisores.
        if (Boolean.TRUE.equals(p.getRevisoresConformes())) {
            p.setRevisoresConformes(false);
            p.setFechaRevisoresConformes(null);
            proyectoRepository.save(p);
        }
    }

    @Override
    @Transactional
    public void darConformidadItem(UUID tesisId, String campo) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);
        verificarRevisorHabilitado(p);
        verificarRubricaDisponible(tesisId);
        if (rv.getEstado() == EstadoRevisor.CONFORME) {
            throw new BusinessException("Ya diste conformidad a este proyecto; la evaluación está cerrada");
        }
        if (campo == null || campo.isBlank()) {
            throw new ValidationException("Indica el ítem a validar");
        }
        ProyectoRevision rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), campo)
                .orElseThrow(() -> new BusinessException("Ese ítem no tiene observaciones"));
        if (rev.getEstado() == EstadoItemRevision.CONFORME) {
            return; // ya validado
        }
        rev.setEstado(EstadoItemRevision.CONFORME);
        revisionRepository.save(rev);
        eventoRepository.save(ProyectoRevisionEvento.builder()
                .revisionId(rev.getId()).tipo("CONFORMIDAD").autor(nombre(docente.getPersona())).rol("REVISOR")
                .texto("El revisor validó la corrección del ítem.").fechaEvento(LocalDateTime.now()).build());
    }

    @Override
    @Transactional
    public void evaluar(UUID tesisId, EvaluarRevisorRequest req) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);
        verificarRevisorHabilitado(p);
        verificarRubricaDisponible(tesisId);
        if (rv.getEstado() == EstadoRevisor.CONFORME) {
            throw new BusinessException("Ya diste conformidad a este proyecto; la evaluación está cerrada");
        }
        Map<String, String> niveles = req != null ? req.getNiveles() : null;
        if (niveles == null || niveles.isEmpty()) {
            throw new ValidationException("Completa la rúbrica antes de evaluar");
        }
        // Cada criterio debe tener un nivel válido (CUMPLE/PARCIAL/NO_CUMPLE); el puntaje se deriva del nivel.
        RubricaDefinicion.Rubrica rubrica = RubricaDefinicion.porEnfoque(
                p.getEnfoque() != null ? p.getEnfoque().name() : null);
        int total = 0;
        Map<String, Integer> puntajePorCriterio = new java.util.HashMap<>();
        Map<String, String> nivelPorCriterio = new java.util.HashMap<>();
        for (RubricaDefinicion.Criterio c : rubrica.criterios()) {
            String nStr = niveles.get(c.key());
            if (nStr == null || nStr.isBlank()) {
                throw new ValidationException("Falta calificar: " + c.titulo());
            }
            RubricaDefinicion.Nivel nivel;
            try {
                nivel = RubricaDefinicion.Nivel.valueOf(nStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Nivel inválido en: " + c.titulo());
            }
            int pt = c.puntaje(nivel);
            total += pt;
            puntajePorCriterio.put(c.key(), pt);
            nivelPorCriterio.put(c.key(), nivel.name());
        }
        // Observar exige al menos una observación (por criterio) o un comentario general.
        Map<String, String> observaciones = req.getObservaciones() != null ? req.getObservaciones() : java.util.Map.of();
        boolean hayObservacion = observaciones.values().stream().anyMatch(o -> o != null && !o.isBlank());
        boolean comentarioVacio = req.getComentario() == null || req.getComentario().isBlank();
        if (!req.isConforme() && comentarioVacio && !hayObservacion) {
            throw new ValidationException("Escribe al menos una observación para el estudiante");
        }
        // Si registró observaciones, la única salida es Observar: no puede dar conformidad.
        if (req.isConforme() && hayObservacion) {
            throw new ValidationException("No puedes dar conformidad con observaciones registradas; marca el proyecto como Observado");
        }

        // Observaciones que ya estaban guardadas: sirven para publicar en el historial del alumno
        // SOLO las nuevas o modificadas (si no, cada re-evaluación duplicaría los eventos).
        Map<String, String> observacionesPrevias = new java.util.HashMap<>();
        for (ProyectoRubricaPuntaje pj : puntajeRepository.listarPorRevisor(rv.getId())) {
            if (pj.getObservacion() != null) {
                observacionesPrevias.put(pj.getCriterio(), pj.getObservacion());
            }
        }

        // Reemplaza los puntajes previos por los nuevos (nivel + puntaje + observación por criterio).
        puntajeRepository.eliminarPorRevisor(rv.getId());
        for (RubricaDefinicion.Criterio c : rubrica.criterios()) {
            String obs = observaciones.get(c.key());
            puntajeRepository.save(ProyectoRubricaPuntaje.builder()
                    .revisorId(rv.getId()).criterio(c.key())
                    .nivel(nivelPorCriterio.get(c.key()))
                    .puntaje(puntajePorCriterio.get(c.key()))
                    .observacion(obs != null && !obs.isBlank() ? obs.trim() : null)
                    .build());
        }

        // Las observaciones de la rúbrica también entran al hilo por campo, para que el estudiante
        // las vea en el historial del ítem junto a las del asesor (y no solo como un resumen suelto).
        publicarObservacionesEnHistorial(p, docente, rubrica, observaciones, observacionesPrevias);

        rv.setPuntajeTotal(total);
        // El comentario del revisor (que ve el estudiante) resume las observaciones por criterio.
        rv.setComentario(!comentarioVacio ? req.getComentario().trim()
                : resumenObservaciones(rubrica, observaciones));
        if (req.isConforme()) {
            rv.setEstado(EstadoRevisor.CONFORME);
            rv.setFechaConformidad(LocalDate.now());
        } else {
            rv.setEstado(EstadoRevisor.OBSERVADO);
            // nueva observación: se borra el levantamiento previo del estudiante
            rv.setRespuestaEstudiante(null);
            rv.setFechaRespuesta(null);
        }
        revisorRepository.save(rv);

        // Si ambos revisores dieron conformidad, el proyecto queda aprobado para la defensa.
        if (req.isConforme()) {
            java.util.List<ProyectoRevisor> todos = revisorRepository.listarPorProyecto(p.getId());
            boolean todosConformes = !todos.isEmpty()
                    && todos.stream().allMatch(x -> x.getEstado() == EstadoRevisor.CONFORME);
            if (todosConformes && !Boolean.TRUE.equals(p.getRevisoresConformes())) {
                p.setRevisoresConformes(true);
                p.setFechaRevisoresConformes(LocalDate.now());
                proyectoRepository.save(p);
            }
        }
    }

    @Override
    @Transactional
    public ArchivoDescargable descargarProyectoPdf(UUID tesisId) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        revisorDe(p, docente); // valida que es revisor del proyecto
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        ProyectoEditorResponse editor = assembler.armar(p, tesis, estudianteDe(tesisId), asesorNombre(tesisId));
        unmsm.edu.pe.tesis.domain.enums.EstiloCita estilo = p.getEstiloCita() != null
                ? p.getEstiloCita() : unmsm.edu.pe.tesis.domain.enums.EstiloCita.APA;
        List<String> refs = referenciaRepository.listarPorProyecto(p.getId()).stream()
                .map(r -> unmsm.edu.pe.tesis.application.util.CitationFormatter.formatear(r, estilo))
                .toList();
        byte[] pdf = documentoExporter.pdf(editor, refs, estilo);
        return new ArchivoDescargable(pdf, "application/pdf", "proyecto.pdf");
    }

    // ── helpers ──

    /**
     * Vuelca las observaciones de la rúbrica al hilo por campo del proyecto (mismo hilo que usa el
     * asesor), de modo que el estudiante las vea en el historial del ítem correspondiente.
     *
     * <p>La mayoría de criterios de la rúbrica comparte clave con un campo del proyecto
     * ({@code situacion}, {@code formulacion}, {@code objetivos}…); los que no la comparten
     * (redacción, presentación, cronograma…) no tienen ítem donde colgarse y quedan únicamente en
     * el resumen del revisor. Solo se publica lo nuevo o modificado para no duplicar eventos al
     * re-evaluar.</p>
     */
    private void publicarObservacionesEnHistorial(ProyectoTesis p, Docente docente,
                                                  RubricaDefinicion.Rubrica rubrica,
                                                  Map<String, String> observaciones,
                                                  Map<String, String> previas) {
        for (RubricaDefinicion.Criterio c : rubrica.criterios()) {
            String obs = observaciones.get(c.key());
            if (obs == null || obs.isBlank()) {
                continue;
            }
            String texto = obs.trim();
            if (texto.equals(previas.get(c.key()))) {
                continue; // ya publicada en una evaluación anterior
            }
            String campo = campoDelCriterio(c.key());
            if (campo == null) {
                continue; // criterio sin nada equivalente en el editor del estudiante
            }
            // Se antepone el criterio observado: en el editor, un mismo campo puede recibir
            // observaciones de varios criterios de la rúbrica y hay que poder distinguirlas.
            registrarObservacionDeCampo(p, docente, campo, c.titulo() + ": " + texto);
        }
    }

    /**
     * Última corrección del estudiante en el ítem que corresponde a un criterio de la rúbrica:
     * {texto, fecha}. Se busca <b>después</b> de la última observación del revisor, para no mostrar
     * como "recién corregido" algo que ya estaba ahí antes de observar.
     */
    private String[] ultimaCorreccionDelItem(ProyectoTesis p, String criterio) {
        String campo = campoDelCriterio(criterio);
        if (campo == null) return null;
        var rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), campo).orElse(null);
        if (rev == null) return null;
        var eventos = eventoRepository.listarPorRevisiones(List.of(rev.getId()));
        LocalDateTime desde = null;
        String texto = null;
        LocalDateTime fecha = null;
        for (var ev : eventos) { // vienen en orden cronológico
            if ("OBSERVACIÓN".equals(ev.getTipo()) && "REVISOR".equals(ev.getRol())) {
                desde = ev.getFechaEvento();
                texto = null;
                fecha = null;
            } else if ("CORRECCIÓN".equals(ev.getTipo())
                    && (desde == null || ev.getFechaEvento() == null || !ev.getFechaEvento().isBefore(desde))) {
                texto = ev.getTexto();
                fecha = ev.getFechaEvento();
            }
        }
        if (texto == null || texto.isBlank()) return null;
        return new String[]{texto.trim(), fecha != null ? fecha.format(FECHA_EVENTO) : null};
    }

    private static final java.time.format.DateTimeFormatter FECHA_EVENTO =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Campo del editor donde el estudiante subsana un criterio de la rúbrica.
     *
     * <p>Los criterios de la rúbrica NO se llaman igual que los campos del proyecto (la rúbrica es
     * un documento de la UPG, el editor es nuestro). Antes se descartaba en silencio todo criterio
     * cuya clave no coincidiera con un campo, así que la mayoría de observaciones del revisor no
     * llegaba al hilo del ítem y el estudiante no era llevado a ninguna parte para corregirlas.</p>
     */
    private String campoDelCriterio(String criterio) {
        if (criterio == null) return null;
        if (ProyectoDefinicion.esCampoValido(criterio)) return criterio;
        return switch (criterio) {
            case "objetivos" -> "objGeneral";
            case "matriz", "tipoDiseno", "abordaje", "escenario" -> "diseno";
            case "sujetos", "unidad", "tamano", "seleccion" -> "poblacion";
            case "recoleccion" -> "tecnicas";
            case "fundamentacion" -> "bases";
            case "cronograma", "presupuesto" -> "plan";
            // "redaccion" y "presentacion" son transversales (ortografía, formato): no pertenecen a
            // un ítem concreto, así que viajan en el comentario del revisor y no abren hilo.
            default -> null;
        };
    }

    /** Abre/actualiza el hilo del campo y agrega el evento de observación del revisor. */
    private void registrarObservacionDeCampo(ProyectoTesis p, Docente docente, String campo, String texto) {
        ProyectoRevision rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), campo)
                .orElseGet(() -> ProyectoRevision.builder().proyectoId(p.getId()).campo(campo).build());
        rev.setEstado(EstadoItemRevision.OBSERVADO);
        rev = revisionRepository.save(rev);
        eventoRepository.save(ProyectoRevisionEvento.builder()
                .revisionId(rev.getId()).tipo("OBSERVACIÓN").autor(nombre(docente.getPersona())).rol("REVISOR")
                .texto(texto).fechaEvento(LocalDateTime.now()).build());
    }

    private String resumenObservaciones(RubricaDefinicion.Rubrica rubrica, Map<String, String> observaciones) {
        if (observaciones == null || observaciones.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (RubricaDefinicion.Criterio c : rubrica.criterios()) {
            String o = observaciones.get(c.key());
            if (o != null && !o.isBlank()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(c.titulo()).append(": ").append(o.trim());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    /**
     * Candado de etapa: una vez que el proyecto avanzó más allá de la revisión (defensa programada o
     * etapas posteriores), la evaluación de los revisores queda cerrada. El flujo es hacia adelante:
     * no se puede volver a observar ni retroceder pasos.
     */
    private boolean etapaAvanzada(ProyectoTesis p) {
        return Boolean.TRUE.equals(p.getDefensaProgramada())
                || Boolean.TRUE.equals(p.getInformeFinalAprobado())
                || Boolean.TRUE.equals(p.getInformeFinalRevisado());
    }

    private void verificarRevisorHabilitado(ProyectoTesis p) {
        if (etapaAvanzada(p)) {
            throw new BusinessException("El proyecto ya avanzó a una etapa posterior (defensa/informe final); "
                    + "la evaluación de los revisores está cerrada.");
        }
    }

    /** La evaluación de los revisores solo se habilita cuando Secretaría subió la rúbrica oficial. */
    /**
     * La evaluación se abre cuando la Secretaría la habilita con la rúbrica oficial del sistema.
     * Se acepta también el Word subido por proyecto (modo anterior) para no dejar colgados a los
     * expedientes que ya lo tenían.
     */
    private void verificarRubricaDisponible(UUID tesisId) {
        boolean habilitada = proyectoRepository.buscarPorTesisId(tesisId)
                .map(p -> Boolean.TRUE.equals(p.getRubricaHabilitada())).orElse(false);
        if (!habilitada && !documentoTesisRepository.existePorTesisYTipo(tesisId, T_RUBRICA)) {
            throw new BusinessException("Secretaría aún no habilita la evaluación con la rúbrica oficial; "
                    + "no puedes evaluar todavía.");
        }
    }

    private ProyectoRevisor revisorDe(ProyectoTesis p, Docente docente) {
        return revisorRepository.buscarPorProyectoYDocente(p.getId(), docente.getPersonaId())
                .orElseThrow(() -> new BusinessException("No eres revisor de este proyecto"));
    }

    private Estudiante estudianteDe(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private String asesorNombre(UUID tesisId) {
        // Desde `asesorias` (la designación vigente): proyectos_tesis.asesor_id puede venir vacío.
        return java.util.Optional.ofNullable(asesorDesignado.asesorId(tesisId))
                .flatMap(id -> docenteRepository.findByPersonaId(id))
                .map(d -> nombre(d.getPersona()))
                .orElse(null);
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

    private java.time.LocalDate toLocalDate(Object o) {
        if (o instanceof java.time.LocalDate ld) return ld;
        if (o instanceof java.sql.Date sd) return sd.toLocalDate();
        return null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }

    private String nz(String s) { return s == null ? "" : s; }
}
