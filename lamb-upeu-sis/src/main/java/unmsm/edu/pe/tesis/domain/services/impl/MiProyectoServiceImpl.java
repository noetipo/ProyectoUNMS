package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.application.util.ProyectoDefinicion;
import unmsm.edu.pe.tesis.domain.entities.*;
import unmsm.edu.pe.tesis.domain.enums.*;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.MiProyectoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class MiProyectoServiceImpl implements MiProyectoService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoCampoRepository campoRepository;
    @Inject ProyectoObjetivoRepository objetivoRepository;
    @Inject ProyectoActividadRepository actividadRepository;
    @Inject ProyectoPartidaRepository partidaRepository;
    @Inject ProyectoRevisionRepository revisionRepository;
    @Inject ProyectoRevisionEventoRepository eventoRepository;
    @Inject ProyectoReferenciaRepository referenciaRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository informeRevisorRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject ProyectoEditorAssembler assembler;
    @Inject unmsm.edu.pe.tesis.infrastructure.export.ProyectoDocumentoExporter documentoExporter;
    @Inject unmsm.edu.pe.tesis.infrastructure.external.CrossRefClient crossRefClient;

    private static final String T_TURNITIN = "TURNITIN_INFORME";
    private static final String T_PROYECTO_FINAL = "PROYECTO_VERSION_FINAL";
    private static final String T_INFORME_FINAL = "INFORME_FINAL_TESIS";

    @Override
    @Transactional
    public ProyectoEditorResponse editor() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        return assembler.armar(p, tesis, est, asesorNombre(tesis.getId()));
    }

    @Override
    @Transactional
    public void guardarCampo(String campo, String valor) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        String v = valor != null ? valor : "";

        if ("titulo".equals(campo)) {
            tesis.setTitulo(v);
            tesisRepository.save(tesis);
        } else if ("resumen".equals(campo)) {
            tesis.setResumen(v);
            tesisRepository.save(tesis);
        } else {
            if (!ProyectoDefinicion.esCampoValido(campo)) {
                throw new ValidationException("Campo de proyecto inválido: " + campo);
            }
            ProyectoCampo c = campoRepository.buscarPorProyectoYClave(p.getId(), campo)
                    .orElseGet(() -> ProyectoCampo.builder().proyectoId(p.getId()).clave(campo).build());
            c.setValor(v);
            campoRepository.save(c);
        }

        // Si el ítem estaba OBSERVADO, editarlo lo pasa a EN_CORRECCION.
        revisionRepository.buscarPorProyectoYCampo(p.getId(), campo).ifPresent(rev -> {
            if (rev.getEstado() == EstadoItemRevision.OBSERVADO) {
                rev.setEstado(EstadoItemRevision.EN_CORRECCION);
                revisionRepository.save(rev);
                registrarEvento(rev.getId(), "EDICIÓN", nombre(est.getPersona()), "ESTUDIANTE",
                        "El estudiante inició la corrección del contenido del ítem.");
            }
        });
    }

    @Override
    @Transactional
    public void setEnfoque(String enfoque) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        EnfoqueInvestigacion nuevo;
        try {
            nuevo = EnfoqueInvestigacion.valueOf(enfoque != null ? enfoque.trim().toUpperCase() : "");
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Enfoque inválido: " + enfoque);
        }
        if (Boolean.TRUE.equals(p.getEnfoqueBloqueado()) && p.getEnfoque() != nuevo) {
            throw new BusinessException("El enfoque está bloqueado; desbloquéalo para cambiarlo");
        }
        p.setEnfoque(nuevo);
        p.setEnfoqueBloqueado(true); // al elegir, queda bloqueado
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void desbloquearEnfoque() {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        p.setEnfoqueBloqueado(false);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void setFinanciamiento(String financiamiento) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        p.setFinanciamiento(financiamiento != null ? financiamiento.trim() : null);
        proyectoRepository.save(p);
    }

    // ── Objetivos ──
    @Override
    @Transactional
    public ObjetivoItem agregarObjetivo(ObjetivoRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoObjetivo o = ProyectoObjetivo.builder()
                .proyectoId(p.getId())
                .texto(req.getTexto())
                .orden(req.getOrden() != null ? req.getOrden() : objetivoRepository.listarPorProyecto(p.getId()).size())
                .build();
        o = objetivoRepository.save(o);
        return ObjetivoItem.builder().id(o.getId()).orden(o.getOrden()).texto(o.getTexto()).build();
    }

    @Override
    @Transactional
    public void actualizarObjetivo(UUID id, ObjetivoRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoObjetivo o = objetivoRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Objetivo no encontrado"));
        verificarPertenece(o.getProyectoId(), p.getId());
        if (req.getTexto() != null) o.setTexto(req.getTexto());
        if (req.getOrden() != null) o.setOrden(req.getOrden());
        objetivoRepository.save(o);
    }

    @Override
    @Transactional
    public void eliminarObjetivo(UUID id) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoObjetivo o = objetivoRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Objetivo no encontrado"));
        verificarPertenece(o.getProyectoId(), p.getId());
        objetivoRepository.eliminar(o);
    }

    // ── Actividades ──
    @Override
    @Transactional
    public ActividadItem agregarActividad(ActividadRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        verificarPlanEditable(p);
        ProyectoActividad a = ProyectoActividad.builder()
                .proyectoId(p.getId())
                .nombre(req.getNombre())
                .fase(fase(req.getFase()))
                .mesInicio(req.getMesInicio())
                .mesFin(req.getMesFin())
                .fechaInicio(req.getFechaInicio())
                .fechaFin(req.getFechaFin())
                .estado(estadoActividad(req.getEstado()))
                .orden(req.getOrden() != null ? req.getOrden() : actividadRepository.listarPorProyecto(p.getId()).size())
                .build();
        a = actividadRepository.save(a);
        return toActividadItem(a);
    }

    @Override
    @Transactional
    public void actualizarActividad(UUID id, ActividadRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoActividad a = actividadRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Actividad no encontrada"));
        verificarPertenece(a.getProyectoId(), p.getId());
        // Con el plan publicado, solo se permite actualizar el ESTADO de ejecución (seguimiento);
        // la estructura (nombre, fase, fechas, orden) queda congelada.
        boolean bloqueado = Boolean.TRUE.equals(p.getPlanPublicado());
        if (!bloqueado) {
            if (req.getNombre() != null) a.setNombre(req.getNombre());
            if (req.getFase() != null) a.setFase(fase(req.getFase()));
            if (req.getMesInicio() != null) a.setMesInicio(req.getMesInicio());
            if (req.getMesFin() != null) a.setMesFin(req.getMesFin());
            if (req.getFechaInicio() != null) a.setFechaInicio(req.getFechaInicio());
            if (req.getFechaFin() != null) a.setFechaFin(req.getFechaFin());
            if (req.getOrden() != null) a.setOrden(req.getOrden());
        }
        if (req.getEstado() != null) a.setEstado(estadoActividad(req.getEstado()));
        actividadRepository.save(a);
    }

    @Override
    @Transactional
    public void eliminarActividad(UUID id) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        verificarPlanEditable(p);
        ProyectoActividad a = actividadRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Actividad no encontrada"));
        verificarPertenece(a.getProyectoId(), p.getId());
        actividadRepository.eliminar(a);
    }

    // ── Referencias bibliográficas ──
    @Override
    public ReferenciaRequest buscarReferenciaPorDoi(String doi) {
        estudianteActual(); // solo estudiantes autenticados
        return crossRefClient.porDoi(doi);
    }

    @Override
    public java.util.List<ReferenciaRequest> buscarReferenciasPorTitulo(String q) {
        estudianteActual();
        // OpenAlex primero (title.search es más preciso y de cobertura amplia), luego CrossRef; sin duplicados.
        java.util.LinkedHashMap<String, ReferenciaRequest> unicos = new java.util.LinkedHashMap<>();
        for (ReferenciaRequest r : crossRefClient.buscarEnOpenAlex(q)) {
            if (r.getTitulo() != null) unicos.putIfAbsent(claveTitulo(r), r);
        }
        for (ReferenciaRequest r : crossRefClient.buscarPorTitulo(q)) {
            if (r.getTitulo() != null) unicos.putIfAbsent(claveTitulo(r), r);
        }
        return unicos.values().stream().limit(8).toList();
    }

    @Override
    public ReferenciaRequest parsearBibtex(String bibtex) {
        estudianteActual();
        return unmsm.edu.pe.tesis.application.util.BibtexParser.parse(bibtex);
    }

    private String claveTitulo(ReferenciaRequest r) {
        if (r.getDoi() != null && !r.getDoi().isBlank()) return "doi:" + r.getDoi().toLowerCase();
        return "t:" + r.getTitulo().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    @Override
    @Transactional
    public ReferenciaItem agregarReferencia(ReferenciaRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoReferencia r = ProyectoReferencia.builder()
                .proyectoId(p.getId())
                .orden(req.getOrden() != null ? req.getOrden() : referenciaRepository.listarPorProyecto(p.getId()).size())
                .build();
        aplicarReferencia(r, req);
        r = referenciaRepository.save(r);
        regenerarCampoReferencias(p);
        int numero = referenciaRepository.listarPorProyecto(p.getId()).size();
        return toReferenciaItem(r, estiloDe(p), numero);
    }

    @Override
    @Transactional
    public void actualizarReferencia(UUID id, ReferenciaRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoReferencia r = referenciaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Referencia no encontrada"));
        verificarPertenece(r.getProyectoId(), p.getId());
        aplicarReferencia(r, req);
        referenciaRepository.save(r);
        regenerarCampoReferencias(p);
    }

    @Override
    @Transactional
    public void eliminarReferencia(UUID id) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoReferencia r = referenciaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Referencia no encontrada"));
        verificarPertenece(r.getProyectoId(), p.getId());
        referenciaRepository.eliminar(r);
        regenerarCampoReferencias(p);
    }

    @Override
    @Transactional
    public void setEstiloCita(String estilo) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        EstiloCita nuevo;
        try {
            nuevo = EstiloCita.valueOf(estilo != null ? estilo.trim().toUpperCase() : "");
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Estilo de cita inválido: " + estilo);
        }
        if (Boolean.TRUE.equals(p.getEstiloCitaBloqueado()) && p.getEstiloCita() != nuevo) {
            throw new BusinessException("El estilo de cita está bloqueado; desbloquéalo para cambiarlo");
        }
        p.setEstiloCita(nuevo);
        p.setEstiloCitaBloqueado(true); // un documento usa un solo estilo
        proyectoRepository.save(p);
        regenerarCampoReferencias(p);
    }

    @Override
    @Transactional
    public void desbloquearEstiloCita() {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        p.setEstiloCitaBloqueado(false);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public ArchivoDescargable exportarDocumento(String formato) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        ProyectoEditorResponse editor = assembler.armar(p, tesis, est, asesorNombre(tesis.getId()));
        EstiloCita estilo = estiloDe(p);
        java.util.List<String> refs = referenciaRepository.listarPorProyecto(p.getId()).stream()
                .map(r -> unmsm.edu.pe.tesis.application.util.CitationFormatter.formatear(r, estilo))
                .toList();
        boolean word = "docx".equalsIgnoreCase(formato) || "word".equalsIgnoreCase(formato);
        byte[] bytes = word ? documentoExporter.docx(editor, refs, estilo) : documentoExporter.pdf(editor, refs, estilo);
        String ct = word ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf";
        String nombre = "Proyecto de tesis" + (word ? ".docx" : ".pdf");
        return new ArchivoDescargable(bytes, ct, nombre);
    }

    private EstiloCita estiloDe(ProyectoTesis p) {
        return p.getEstiloCita() != null ? p.getEstiloCita() : EstiloCita.APA;
    }

    private void aplicarReferencia(ProyectoReferencia r, ReferenciaRequest req) {
        if (req.getTipo() != null) {
            try { r.setTipo(TipoReferencia.valueOf(req.getTipo().trim().toUpperCase())); }
            catch (IllegalArgumentException ex) { throw new ValidationException("Tipo de referencia inválido: " + req.getTipo()); }
        }
        r.setAutores(req.getAutores());
        r.setAnio(req.getAnio());
        r.setTitulo(req.getTitulo());
        r.setFuente(req.getFuente());
        r.setVolumen(req.getVolumen());
        r.setNumero(req.getNumero());
        r.setPaginas(req.getPaginas());
        r.setEditorial(req.getEditorial());
        r.setCiudad(req.getCiudad());
        r.setDoi(req.getDoi());
        r.setUrl(req.getUrl());
        r.setFechaAcceso(req.getFechaAcceso());
        if (req.getOrden() != null) r.setOrden(req.getOrden());
    }

    private ReferenciaItem toReferenciaItem(ProyectoReferencia r, EstiloCita estilo, int numero) {
        String fmt = unmsm.edu.pe.tesis.application.util.CitationFormatter.formatear(r, estilo)
                .replace("«i»", "").replace("«/i»", "");
        return ReferenciaItem.builder().id(r.getId()).orden(r.getOrden())
                .tipo(r.getTipo() != null ? r.getTipo().name() : null)
                .autores(r.getAutores()).anio(r.getAnio()).titulo(r.getTitulo()).fuente(r.getFuente())
                .volumen(r.getVolumen()).numero(r.getNumero()).paginas(r.getPaginas())
                .editorial(r.getEditorial()).ciudad(r.getCiudad()).doi(r.getDoi()).url(r.getUrl())
                .fechaAcceso(r.getFechaAcceso()).formateada(fmt)
                .citaTexto(unmsm.edu.pe.tesis.application.util.CitationFormatter.citaEnTexto(r, estilo, numero))
                .citaNarrativa(unmsm.edu.pe.tesis.application.util.CitationFormatter.citaNarrativa(r, estilo, numero))
                .build();
    }

    /** Vuelca las referencias formateadas al campo de texto 'referencias' (para avance/matriz y legado). */
    private void regenerarCampoReferencias(ProyectoTesis p) {
        EstiloCita estilo = estiloDe(p);
        java.util.List<ProyectoReferencia> refs = referenciaRepository.listarPorProyecto(p.getId());
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (ProyectoReferencia r : refs) {
            String linea = unmsm.edu.pe.tesis.application.util.CitationFormatter.formatear(r, estilo)
                    .replace("«i»", "").replace("«/i»", "");
            if (linea.isBlank()) continue;
            if (estilo.esNumerico()) sb.append('[').append(n++).append("] ");
            sb.append(linea).append('\n');
        }
        ProyectoCampo c = campoRepository.buscarPorProyectoYClave(p.getId(), "referencias")
                .orElseGet(() -> ProyectoCampo.builder().proyectoId(p.getId()).clave("referencias").build());
        c.setValor(sb.toString().trim());
        campoRepository.save(c);
    }

    // ── Presupuesto ──
    @Override
    @Transactional
    public PartidaItem agregarPartida(PartidaRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoPartida pa = ProyectoPartida.builder()
                .proyectoId(p.getId())
                .rubro(req.getRubro())
                .descripcion(req.getDescripcion())
                .monto(req.getMonto() != null ? req.getMonto() : BigDecimal.ZERO)
                .orden(req.getOrden() != null ? req.getOrden() : partidaRepository.listarPorProyecto(p.getId()).size())
                .build();
        pa = partidaRepository.save(pa);
        return PartidaItem.builder().id(pa.getId()).rubro(pa.getRubro()).descripcion(pa.getDescripcion())
                .monto(pa.getMonto()).orden(pa.getOrden()).build();
    }

    @Override
    @Transactional
    public void actualizarPartida(UUID id, PartidaRequest req) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoPartida pa = partidaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Partida no encontrada"));
        verificarPertenece(pa.getProyectoId(), p.getId());
        if (req.getRubro() != null) pa.setRubro(req.getRubro());
        if (req.getDescripcion() != null) pa.setDescripcion(req.getDescripcion());
        if (req.getMonto() != null) pa.setMonto(req.getMonto());
        if (req.getOrden() != null) pa.setOrden(req.getOrden());
        partidaRepository.save(pa);
    }

    @Override
    @Transactional
    public void eliminarPartida(UUID id) {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        ProyectoPartida pa = partidaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Partida no encontrada"));
        verificarPertenece(pa.getProyectoId(), p.getId());
        partidaRepository.eliminar(pa);
    }

    // ── Hitos de los 5 pasos ──
    @Override
    @Transactional
    public void marcarListoRevision() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        ProyectoEditorResponse editor = assembler.armar(p, tesis, est, asesorNombre(tesis.getId()));
        if (editor.getAvancePct() < 100) {
            throw new BusinessException("Completa todos los campos del proyecto antes de enviarlo a revisión");
        }
        p.setListoRevision(true);
        p.setFechaListoRevision(LocalDate.now());
        p.setEstado(EstadoProyecto.EN_REVISION);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void reenviarRevision() {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        if (p.getEstado() != EstadoProyecto.OBSERVADO) {
            throw new BusinessException("El proyecto no tiene observaciones que reenviar");
        }
        boolean hayPendientesSinCorregir = revisionRepository.listarPorProyecto(p.getId()).stream()
                .anyMatch(r -> r.getEstado() == EstadoItemRevision.OBSERVADO
                        || r.getEstado() == EstadoItemRevision.EN_CORRECCION);
        if (hayPendientesSinCorregir) {
            throw new BusinessException("Corrige todas las observaciones antes de reenviar a revisión");
        }
        // Estar OBSERVADO ya implica que el proyecto fue enviado; se reafirma el hito por si acaso.
        p.setListoRevision(true);
        if (p.getFechaListoRevision() == null) {
            p.setFechaListoRevision(LocalDate.now());
        }
        p.setEstado(EstadoProyecto.EN_REVISION);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void publicarPlan() {
        ProyectoTesis p = proyectoDe(tesisActiva(estudianteActual()));
        if (actividadRepository.listarPorProyecto(p.getId()).isEmpty()) {
            throw new BusinessException("Agrega al menos una actividad antes de publicar el plan");
        }
        p.setPlanPublicado(true);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void subirTurnitin(byte[] contenido, String nombreOriginal, String contentType, Integer porcentaje) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        validarArchivo(contenido, contentType);

        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesis.getId(), T_TURNITIN);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        DocumentoTesis doc = existente.orElseGet(
                () -> DocumentoTesis.builder().tesisId(tesis.getId()).tipo(T_TURNITIN).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setHashSha256(sha256(contenido));
        doc.setFechaCarga(LocalDateTime.now());
        doc.setSubidoPor(est.getPersonaId());
        documentoTesisRepository.save(doc);

        if (porcentaje != null) {
            p.setPorcentajeSimilitud(Math.max(0, Math.min(100, porcentaje)));
        }
        p.setTurnitinSubido(true);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void subirProyectoFinal(byte[] contenido, String nombreOriginal, String contentType) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        proyectoDe(tesis);
        validarArchivo(contenido, contentType);

        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesis.getId(), T_PROYECTO_FINAL);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        DocumentoTesis doc = existente.orElseGet(
                () -> DocumentoTesis.builder().tesisId(tesis.getId()).tipo(T_PROYECTO_FINAL).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setHashSha256(sha256(contenido));
        doc.setFechaCarga(LocalDateTime.now());
        doc.setSubidoPor(est.getPersonaId());
        documentoTesisRepository.save(doc);
    }

    @Override
    @Transactional
    public void subirInformeFinal(byte[] contenido, String nombreOriginal, String contentType) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        if (!Boolean.TRUE.equals(p.getDefensaProgramada())) {
            throw new BusinessException("El informe final se sube durante la ejecución (tras aprobar el proyecto)");
        }
        validarArchivo(contenido, contentType);

        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesis.getId(), T_INFORME_FINAL);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        DocumentoTesis doc = existente.orElseGet(
                () -> DocumentoTesis.builder().tesisId(tesis.getId()).tipo(T_INFORME_FINAL).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setHashSha256(sha256(contenido));
        doc.setFechaCarga(LocalDateTime.now());
        doc.setSubidoPor(est.getPersonaId());
        documentoTesisRepository.save(doc);
    }

    @Override
    @Transactional
    public unmsm.edu.pe.tesis.application.dto.ArchivoDescargable descargarDocumento(String tipo) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        String t = switch (tipo != null ? tipo.trim().toLowerCase() : "") {
            case "turnitin" -> T_TURNITIN;
            case "proyecto-final", "proyecto_final", "final" -> T_PROYECTO_FINAL;
            case "informe-final", "informe_final", "informe" -> T_INFORME_FINAL;
            default -> throw new ValidationException("Tipo de documento inválido: " + tipo);
        };
        DocumentoTesis doc = documentoTesisRepository.buscarPorTesisYTipo(tesis.getId(), t)
                .orElseThrow(() -> new BusinessException("Aún no has subido ese documento"));
        byte[] bytes = almacenamiento.obtener(doc.getStorageKey());
        return new unmsm.edu.pe.tesis.application.dto.ArchivoDescargable(bytes, doc.getContentType(), doc.getNombreOriginal());
    }

    @Override
    @Transactional
    public void corregirItem(String campo, String respuesta) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        ProyectoRevision rev = revisionRepository.buscarPorProyectoYCampo(p.getId(), campo)
                .orElseThrow(() -> new BusinessException("Ese ítem no tiene observaciones"));
        if (rev.getEstado() != EstadoItemRevision.OBSERVADO && rev.getEstado() != EstadoItemRevision.EN_CORRECCION) {
            throw new BusinessException("El ítem no está en estado corregible");
        }
        rev.setEstado(EstadoItemRevision.CORREGIDO);
        revisionRepository.save(rev);
        // El evento de corrección muestra el contenido corregido del ítem (lo que el asesor debe re-verificar).
        // Si el estudiante escribió una nota, se prioriza; si no, se guarda el nuevo valor del campo.
        String contenido = valorCampoActual(campo, tesis, p);
        String texto = respuesta != null && !respuesta.isBlank() ? respuesta.trim()
                : (contenido != null && !contenido.isBlank() ? contenido.trim()
                        : "El estudiante marcó el ítem como corregido.");
        registrarEvento(rev.getId(), "CORRECCIÓN", nombre(est.getPersona()), "ESTUDIANTE", texto);
    }

    /** Valor actual de un campo del editor (título/resumen viven en la tesis; el resto en proyecto_campos). */
    private String valorCampoActual(String campo, Tesis tesis, ProyectoTesis p) {
        if ("titulo".equals(campo)) return tesis.getTitulo();
        if ("resumen".equals(campo)) return tesis.getResumen();
        return campoRepository.buscarPorProyectoYClave(p.getId(), campo)
                .map(ProyectoCampo::getValor).orElse(null);
    }

    @Override
    @Transactional
    public void responderRevisor(UUID revisorId, String respuesta) {
        Estudiante est = estudianteActual();
        ProyectoTesis p = proyectoDe(tesisActiva(est));
        ProyectoRevisor rv = revisorRepository.buscarPorId(revisorId)
                .orElseThrow(() -> new NotFoundException("Revisor no encontrado"));
        if (!rv.getProyectoId().equals(p.getId())) {
            throw new BusinessException("Ese revisor no pertenece a tu proyecto");
        }
        if (rv.getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoRevisor.OBSERVADO) {
            throw new BusinessException("Solo puedes responder a un revisor que dejó observaciones");
        }
        if (respuesta == null || respuesta.isBlank()) {
            throw new ValidationException("Escribe cómo levantaste las observaciones del revisor");
        }
        rv.setRespuestaEstudiante(respuesta.trim());
        rv.setFechaRespuesta(LocalDate.now());
        revisorRepository.save(rv);
    }

    @Override
    @Transactional
    public void solicitarJuradoInformante() {
        Estudiante est = estudianteActual();
        ProyectoTesis p = proyectoDe(tesisActiva(est));
        if (Boolean.TRUE.equals(p.getJuradoInformanteSolicitado())) {
            throw new BusinessException("Ya solicitaste el Jurado Informante");
        }
        if (!Boolean.TRUE.equals(p.getInformeFinalAprobado())) {
            throw new BusinessException("Falta la carta del asesor aprobando el informe final");
        }
        if (!Boolean.TRUE.equals(p.getTurnitinSubido())) {
            throw new BusinessException("Falta subir el informe de similitud de Turnitin");
        }
        p.setJuradoInformanteSolicitado(true);
        p.setFechaJuradoInformante(LocalDate.now());
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void responderJuradoInforme(UUID revisorId, String respuesta) {
        Estudiante est = estudianteActual();
        ProyectoTesis p = proyectoDe(tesisActiva(est));
        var rv = informeRevisorRepository.buscarPorId(revisorId)
                .orElseThrow(() -> new NotFoundException("Miembro del jurado no encontrado"));
        if (!rv.getProyectoId().equals(p.getId())) {
            throw new BusinessException("Ese jurado no pertenece a tu tesis");
        }
        if (rv.getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoRevisor.OBSERVADO) {
            throw new BusinessException("Solo puedes responder a un jurado que dejó observaciones");
        }
        if (respuesta == null || respuesta.isBlank()) {
            throw new ValidationException("Escribe cómo levantaste las observaciones del jurado");
        }
        rv.setRespuestaEstudiante(respuesta.trim());
        informeRevisorRepository.save(rv);
    }

    @Override
    @Transactional
    public void solicitarAprobacion() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);
        if (!Boolean.TRUE.equals(p.getCartaAsesor())) {
            throw new BusinessException("Falta la carta de opinión favorable del asesor");
        }
        if (!Boolean.TRUE.equals(p.getTurnitinSubido())) {
            throw new BusinessException("Falta subir el informe de Turnitin");
        }
        if (!documentoTesisRepository.existePorTesisYTipo(tesis.getId(), T_PROYECTO_FINAL)) {
            throw new BusinessException("Falta subir el proyecto en versión final");
        }
        p.setExpedienteSubido(true);
        p.setFechaSolicitudAprobacion(LocalDate.now());
        proyectoRepository.save(p);
        // Avanza el proceso a la Etapa 5 (defensa/dictamen del proyecto).
        tesis.setEstado(EstadoTesis.PROYECTO_PRESENTADO);
        tesisRepository.save(tesis);
    }

    // ── DEMO / pruebas ──
    private static final java.util.Map<String, String> CAMPOS_DEMO = java.util.Map.ofEntries(
            java.util.Map.entry("palabras", "diabetes mellitus tipo 2; altitud; prevalencia"),
            java.util.Map.entry("situacion", "La diabetes mellitus tipo 2 es un problema creciente de salud pública. En poblaciones de gran altitud la evidencia es escasa y contradictoria."),
            java.util.Map.entry("formulacion", "¿Cuál es la prevalencia de diabetes mellitus tipo 2 en adultos residentes por encima de los 3500 m.s.n.m.?"),
            java.util.Map.entry("justificacion", "El estudio aporta evidencia local para orientar políticas de tamizaje y prevención en zonas altoandinas."),
            java.util.Map.entry("objGeneral", "Determinar la prevalencia de diabetes mellitus tipo 2 en adultos residentes de gran altitud."),
            java.util.Map.entry("antecedentes", "Estudios previos reportan prevalencias entre 5% y 12% con metodologías heterogéneas."),
            java.util.Map.entry("bases", "Fisiopatología de la resistencia a la insulina y adaptación a la hipoxia crónica de altura."),
            java.util.Map.entry("glosario", "Prevalencia: proporción de casos existentes en una población en un momento dado."),
            java.util.Map.entry("hipotesis", "La prevalencia de DM2 en gran altitud es menor que la reportada a nivel del mar."),
            java.util.Map.entry("variables", "Variable dependiente: diabetes mellitus tipo 2. Variables independientes: edad, sexo, IMC, altitud de residencia."),
            java.util.Map.entry("operacional", "DM2 · glucemia en ayunas ≥126 mg/dL o diagnóstico previo · dicotómica · nominal."),
            java.util.Map.entry("diseno", "Observacional, descriptivo, de corte transversal."),
            java.util.Map.entry("poblacion", "Adultos de 18 a 65 años residentes en distritos por encima de 3500 m.s.n.m.; muestreo probabilístico."),
            java.util.Map.entry("tecnicas", "Encuesta estructurada y medición de glucemia capilar con equipo calibrado."),
            java.util.Map.entry("analisis", "Estadística descriptiva; prevalencia con IC95%; asociaciones con chi-cuadrado y regresión logística."),
            java.util.Map.entry("eticos", "Consentimiento informado, aprobación del comité de ética y confidencialidad de los datos."),
            java.util.Map.entry("referencias", "1. OMS. Informe mundial sobre la diabetes. 2. MINSA. Guía de práctica clínica de DM2."),
            java.util.Map.entry("anexos", "Anexo 1: cuestionario. Anexo 2: formato de consentimiento informado."));

    @Override
    @Transactional
    public void seedDemo() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);

        // Enfoque cuantitativo (bloqueado, como al elegirlo)
        p.setEnfoque(EnfoqueInvestigacion.CUANTITATIVO);
        p.setEnfoqueBloqueado(true);

        // Título y resumen viven en la tesis
        if (tesis.getTitulo() == null || tesis.getTitulo().isBlank()) {
            tesis.setTitulo("Prevalencia de diabetes mellitus tipo 2 en poblaciones de gran altitud");
        }
        tesis.setResumen("Estudio observacional de corte transversal para estimar la prevalencia de DM2 y sus factores asociados en adultos residentes de gran altitud.");
        tesisRepository.save(tesis);

        // Campos persistidos
        CAMPOS_DEMO.forEach((clave, valor) -> {
            ProyectoCampo c = campoRepository.buscarPorProyectoYClave(p.getId(), clave)
                    .orElseGet(() -> ProyectoCampo.builder().proyectoId(p.getId()).clave(clave).build());
            c.setValor(valor);
            campoRepository.save(c);
        });

        // Objetivos específicos
        if (objetivoRepository.listarPorProyecto(p.getId()).isEmpty()) {
            String[] objs = {
                    "Estimar la prevalencia global de DM2 en la población de estudio.",
                    "Identificar los factores asociados a la presencia de DM2.",
            };
            for (int i = 0; i < objs.length; i++) {
                objetivoRepository.save(ProyectoObjetivo.builder()
                        .proyectoId(p.getId()).texto(objs[i]).orden(i).build());
            }
        }

        // Cronograma (plan de actividades)
        if (actividadRepository.listarPorProyecto(p.getId()).isEmpty()) {
            LocalDate base = LocalDate.now().withDayOfMonth(1);
            actividadRepository.save(ProyectoActividad.builder().proyectoId(p.getId())
                    .nombre("Elaboración del proyecto y trámites éticos").fase(FaseActividad.PLANIFICACION)
                    .mesInicio(1).mesFin(3).fechaInicio(base).fechaFin(base.plusMonths(2))
                    .estado(EstadoActividad.HECHA).orden(0).build());
            actividadRepository.save(ProyectoActividad.builder().proyectoId(p.getId())
                    .nombre("Recolección de datos en campo").fase(FaseActividad.TRABAJO_CAMPO)
                    .mesInicio(4).mesFin(9).fechaInicio(base.plusMonths(3)).fechaFin(base.plusMonths(8))
                    .estado(EstadoActividad.EN_CURSO).orden(1).build());
            actividadRepository.save(ProyectoActividad.builder().proyectoId(p.getId())
                    .nombre("Análisis estadístico y redacción del informe").fase(FaseActividad.ANALISIS)
                    .mesInicio(10).mesFin(14).fechaInicio(base.plusMonths(9)).fechaFin(base.plusMonths(13))
                    .estado(EstadoActividad.PENDIENTE).orden(2).build());
        }

        // Presupuesto por partidas
        if (partidaRepository.listarPorProyecto(p.getId()).isEmpty()) {
            partidaRepository.save(ProyectoPartida.builder().proyectoId(p.getId())
                    .rubro("Insumos de laboratorio").descripcion("Tiras reactivas y lancetas")
                    .monto(new BigDecimal("1800.00")).orden(0).build());
            partidaRepository.save(ProyectoPartida.builder().proyectoId(p.getId())
                    .rubro("Movilidad").descripcion("Traslados a comunidades altoandinas")
                    .monto(new BigDecimal("1200.00")).orden(1).build());
            partidaRepository.save(ProyectoPartida.builder().proyectoId(p.getId())
                    .rubro("Publicación").descripcion("Cargo por procesamiento de artículo")
                    .monto(new BigDecimal("900.00")).orden(2).build());
        }

        p.setFinanciamiento("Autofinanciado");
        p.setPlanPublicado(true);
        p.setListoRevision(true);
        p.setFechaListoRevision(LocalDate.now());
        p.setEstado(EstadoProyecto.EN_REVISION);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void resetDemo() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisActiva(est);
        ProyectoTesis p = proyectoDe(tesis);

        // Borra físicamente observaciones/correcciones para reiniciar la revisión.
        // (Debe ser hard-delete: hay un UNIQUE (proyecto_id, campo); un soft-delete dejaría
        //  la fila y una nueva observación del mismo campo chocaría con el constraint.)
        java.util.List<UUID> revIds = revisionRepository.idsPorProyecto(p.getId());
        eventoRepository.eliminarPorRevisiones(revIds);
        revisionRepository.eliminarPorProyecto(p.getId());

        // Vuelve al estado "aún no enviado" (conserva el contenido de los campos)
        p.setListoRevision(false);
        p.setFechaListoRevision(null);
        p.setCartaAsesor(false);
        p.setFechaCartaAsesor(null);
        p.setTurnitinSubido(false);
        p.setPorcentajeSimilitud(null);
        p.setExpedienteSubido(false);
        p.setFechaSolicitudAprobacion(null);
        p.setExpedienteRecibido(null);
        p.setFechaRecepcion(null);
        p.setPlanPublicado(false);
        p.setEstado(EstadoProyecto.EN_ELABORACION);
        proyectoRepository.save(p);
    }

    // ── helpers ──
    /** Una vez publicado el plan, el cronograma queda congelado (no se agregan ni eliminan actividades). */
    private void verificarPlanEditable(ProyectoTesis p) {
        if (Boolean.TRUE.equals(p.getPlanPublicado())) {
            throw new BusinessException("El plan de actividades ya fue publicado; solo puedes actualizar el estado de cada actividad");
        }
    }

    private ProyectoTesis proyectoDe(Tesis tesis) {
        return proyectoRepository.buscarPorTesisId(tesis.getId()).orElseGet(() -> {
            UUID asesorId = asesoriaRepository.buscarPorTesisYTipo(tesis.getId(), "ASESOR")
                    .map(Asesoria::getDocenteId).orElse(null);
            return proyectoRepository.save(ProyectoTesis.builder()
                    .tesisId(tesis.getId()).asesorId(asesorId).build());
        });
    }

    private Tesis tesisActiva(Estudiante est) {
        return tesisRepository.tesisActivaDeEstudiante(est.getPersonaId())
                .orElseThrow(() -> new BusinessException("No tienes una tesis activa"));
    }

    private String asesorNombre(UUID tesisId) {
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .flatMap(a -> docenteRepository.findByPersonaId(a.getDocenteId()))
                .map(d -> nombre(d.getPersona()))
                .orElse(null);
    }

    private void registrarEvento(UUID revisionId, String tipo, String autor, String rol, String texto) {
        eventoRepository.save(ProyectoRevisionEvento.builder()
                .revisionId(revisionId).tipo(tipo).autor(autor).rol(rol).texto(texto)
                .fechaEvento(LocalDateTime.now()).build());
    }

    private void verificarPertenece(UUID owner, UUID proyectoId) {
        if (owner == null || !owner.equals(proyectoId)) {
            throw new BusinessException("El elemento no pertenece a tu proyecto");
        }
    }

    private FaseActividad fase(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return FaseActividad.valueOf(v.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Fase inválida: " + v);
        }
    }

    private EstadoActividad estadoActividad(String v) {
        if (v == null || v.isBlank()) return EstadoActividad.PENDIENTE;
        try {
            return EstadoActividad.valueOf(v.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Estado de actividad inválido: " + v);
        }
    }

    private ActividadItem toActividadItem(ProyectoActividad a) {
        return ActividadItem.builder().id(a.getId()).nombre(a.getNombre())
                .fase(a.getFase() != null ? a.getFase().name() : null)
                .mesInicio(a.getMesInicio()).mesFin(a.getMesFin())
                .fechaInicio(a.getFechaInicio()).fechaFin(a.getFechaFin())
                .estado(a.getEstado() != null ? a.getEstado().name() : null)
                .orden(a.getOrden()).build();
    }

    private Estudiante estudianteActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        Persona persona = personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
        return estudianteRepository.findByPersonaId(persona.getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de estudiante"));
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private static final java.util.Set<String> TIPOS_PERMITIDOS = java.util.Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword");

    private void validarArchivo(byte[] contenido, String contentType) {
        if (contenido == null || contenido.length == 0) throw new ValidationException("El archivo está vacío");
        if (contenido.length > 10 * 1024 * 1024) throw new ValidationException("El archivo supera 10 MB");
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType))
            throw new ValidationException("Formato no permitido; sube un PDF o Word (.docx)");
    }

    private String sha256(byte[] c) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(c)) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
