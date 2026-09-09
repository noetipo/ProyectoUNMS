package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.DefensaInfo;
import unmsm.edu.pe.tesis.application.dto.ExpedienteBandejaItem;
import unmsm.edu.pe.tesis.application.dto.JuradoItem;
import unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest;
import unmsm.edu.pe.tesis.application.dto.RubricaBandejaItem;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.RubricaDefensa;
import unmsm.edu.pe.tesis.domain.enums.ModalidadDefensa;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.RubricaDefensaRepository;
import unmsm.edu.pe.tesis.domain.services.SecretariaDefensaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SecretariaDefensaServiceImpl implements SecretariaDefensaService {

    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject RubricaDefensaRepository rubricaRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaRepository personaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.PlantillaRubricaRepository plantillaRepository;

    @Override
    @Transactional
    public PageResponse<ExpedienteBandejaItem> bandeja(String buscar, int page, int size) {
        List<ExpedienteBandejaItem> content = proyectoRepository.bandejaExpedientes(buscar, page, size)
                .stream().map(this::toItem).collect(Collectors.toList());
        long total = proyectoRepository.contarBandejaExpedientes(buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public void recibir(UUID tesisId) {
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        if (!Boolean.TRUE.equals(p.getExpedienteSubido())) {
            throw new BusinessException("El estudiante aún no ha enviado la solicitud de aprobación");
        }
        p.setExpedienteRecibido(true);
        p.setFechaRecepcion(LocalDate.now());
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public List<RubricaBandejaItem> bandejaRubricas(String buscar) {
        List<RubricaBandejaItem> out = new ArrayList<>();
        for (Object[] r : proyectoRepository.bandejaDefensa(buscar, 0, 100)) {
            int numRevisores = r[9] != null ? ((Number) r[9]).intValue() : 0;
            if (numRevisores == 0) {
                continue;   // solo proyectos con revisores designados
            }
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
            String enfoque = p != null && p.getEnfoque() != null ? p.getEnfoque().name() : null;
            var doc = documentoTesisRepository.buscarPorTesisYTipo(tesisId, RevisorProyectoServiceImpl.T_RUBRICA);
            List<String> revisores = revisorRepository.listarPorProyecto(proyectoId).stream()
                    .sorted(java.util.Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                    .map(rv -> nombreDocente(rv.getDocenteId()))
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.toList());
            out.add(RubricaBandejaItem.builder()
                    .tesisId(tesisId)
                    .estudianteApellidos((asStr(r[2]) + " " + asStr(r[3])).trim())
                    .estudianteNombres(asStr(r[4]))
                    .codigoSistema(asStr(r[5]))
                    .programaNombre(asStr(r[6]))
                    .tituloTesis(asStr(r[7]))
                    .enfoque(enfoque)
                    .numRevisores(numRevisores)
                    .revisores(revisores)
                    .rubricaSubida(doc.isPresent())
                    .rubricaNombreArchivo(doc.map(DocumentoTesis::getNombreOriginal).orElse(null))
                    // Interruptor: la rúbrica oficial ya vive en el sistema; aquí solo se enciende.
                    .rubricaHabilitada(p != null && Boolean.TRUE.equals(p.getRubricaHabilitada()))
                    .plantillaLabel("CUALITATIVO".equalsIgnoreCase(enfoque) ? "Cualitativa" : "Cuantitativa / mixta")
                    .plantillaVersion(plantillaRepository
                            .vigente("CUALITATIVO".equalsIgnoreCase(enfoque) ? "CUALITATIVO" : "CUANTITATIVO")
                            .map(pl -> pl.getVersion()).orElse(null))
                    .plantillaVersionAplicada(p != null ? p.getRubricaVersion() : null)
                    .revisoresConformes(p != null && Boolean.TRUE.equals(p.getRevisoresConformes()))
                    .defensaProgramada(p != null && Boolean.TRUE.equals(p.getDefensaProgramada()))
                    .fechaDefensa(p != null ? p.getFechaDefensa() : null)
                    .horaDefensa(p != null ? p.getHoraDefensa() : null)
                    .lugarDefensa(p != null ? p.getLugarDefensa() : null)
                    .build());
        }
        return out;
    }

    private static final String WORD_CT = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /**
     * Enciende o apaga la evaluación de los revisores con la rúbrica oficial del sistema.
     *
     * <p>Al habilitar se <b>congela la versión</b> vigente de la plantilla del enfoque: si más
     * adelante se publica la del año siguiente, este proyecto sigue evaluándose con la que se le
     * aplicó. Apagar el interruptor suspende la evaluación (p. ej. si hay que corregir algo antes),
     * sin borrar lo ya evaluado.</p>
     */
    @Override
    @Transactional
    public void habilitarRubrica(UUID tesisId, boolean habilitar) {
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        if (habilitar) {
            if (revisorRepository.contarPorProyecto(p.getId()) == 0) {
                throw new BusinessException("Aún no se han designado revisores para este proyecto");
            }
            String enfoque = p.getEnfoque() != null && "CUALITATIVO".equalsIgnoreCase(p.getEnfoque().name())
                    ? "CUALITATIVO" : "CUANTITATIVO";
            var plantilla = plantillaRepository.vigente(enfoque)
                    .orElseThrow(() -> new BusinessException(
                            "Aún no se ha publicado la rúbrica oficial " + enfoque.toLowerCase()
                                    + ". Súbela en Configuración › Rúbricas oficiales."));
            p.setRubricaVersion(plantilla.getVersion());
        }
        p.setRubricaHabilitada(habilitar);
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void subirRubrica(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        if (revisorRepository.contarPorProyecto(p.getId()) == 0) {
            throw new BusinessException("Aún no se han designado revisores para este proyecto");
        }
        if (contenido == null || contenido.length == 0) {
            throw new BusinessException("Adjunta la rúbrica en Word (.docx)");
        }
        if (!esWord(nombreOriginal, contentType)) {
            throw new BusinessException("La rúbrica debe subirse en Word (.docx).");
        }
        // Se guarda el Word tal cual: es lo que el revisor descargará (sin alterar el documento oficial).
        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesisId, RevisorProyectoServiceImpl.T_RUBRICA);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, WORD_CT);
        DocumentoTesis doc = existente.orElseGet(
                () -> DocumentoTesis.builder().tesisId(tesisId).tipo(RevisorProyectoServiceImpl.T_RUBRICA).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(WORD_CT);
        doc.setTamanioBytes((long) contenido.length);
        doc.setFechaCarga(LocalDateTime.now());
        documentoTesisRepository.save(doc);
    }

    // ── Programación de la defensa (Etapa 5) — la realiza la Secretaría ──

    @Override
    @Transactional
    public DefensaInfo defensa(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        return DefensaInfo.builder()
                .programada(Boolean.TRUE.equals(p.getDefensaProgramada()))
                .fecha(p.getFechaDefensa()).hora(p.getHoraDefensa()).lugar(p.getLugarDefensa())
                .modalidad(p.getModalidadDefensa() != null ? p.getModalidadDefensa().name() : null)
                .modalidadLabel(p.getModalidadDefensa() != null ? p.getModalidadDefensa().etiqueta() : null)
                .enlace(p.getEnlaceDefensa())
                .dictamenNumero(p.getDictamenNumero())
                .jurado(evaluadoresDeLaDefensa(p.getId()))
                .build();
    }

    /**
     * Quiénes evalúan la defensa: los mismos dos revisores que ya evaluaron el proyecto con la
     * rúbrica (el diagrama del proceso no contempla un jurado aparte para este paso — eso es de
     * la Sustentación final, Etapa 8). Se listan aquí solo para informar, no para elegir a nadie.
     */
    private List<JuradoItem> evaluadoresDeLaDefensa(UUID proyectoId) {
        return revisorRepository.listarPorProyecto(proyectoId).stream()
                .sorted(java.util.Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> JuradoItem.builder()
                        .docenteId(rv.getDocenteId())
                        .docenteNombre(nombreDocente(rv.getDocenteId()))
                        .rol("REVISOR")
                        .orden(rv.getOrden())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void programarDefensa(UUID tesisId, ProgramarDefensaRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getRevisoresConformes())) {
            throw new BusinessException("Los revisores aún no dieron conformidad al proyecto");
        }
        if (Boolean.TRUE.equals(p.getDefensaProgramada())) {
            throw new BusinessException("La defensa de este proyecto ya fue programada");
        }
        if (req == null || req.getFecha() == null) {
            throw new ValidationException("Indica la fecha de la defensa");
        }
        // La modalidad decide qué dato de ubicación es obligatorio: aula, enlace o ambos.
        ModalidadDefensa modalidad = modalidadDe(req.getModalidad());
        String lugar = req.getLugar() != null ? req.getLugar().trim() : "";
        String enlace = req.getEnlace() != null ? req.getEnlace().trim() : "";
        if (modalidad.requiereLugar() && lugar.isEmpty()) {
            throw new ValidationException("Indica el aula o ambiente donde se realizará la defensa");
        }
        if (modalidad.requiereEnlace() && enlace.isEmpty()) {
            throw new ValidationException("Indica el enlace de la sesión para la defensa " + modalidad.etiqueta().toLowerCase());
        }

        // Si esta es una reprogramación tras una defensa desaprobada, el resultado y las rúbricas
        // del acto anterior quedan sin efecto: el próximo acto es uno nuevo, no una continuación.
        p.setDefensaRealizada(false);
        p.setResultadoDefensa(null);
        p.setFechaResultadoDefensa(null);
        p.setObservacionDefensa(null);
        for (RubricaDefensa r : rubricaRepository.porTesis(tesisId)) {
            if (r.getStorageKey() != null) {
                almacenamiento.eliminar(r.getStorageKey());
            }
        }
        rubricaRepository.eliminarPorTesis(tesisId);

        p.setDefensaProgramada(true);
        p.setFechaDefensa(req.getFecha());
        p.setHoraDefensa(req.getHora());
        p.setLugarDefensa(lugar.isEmpty() ? null : lugar);
        p.setModalidadDefensa(modalidad);
        p.setEnlaceDefensa(enlace.isEmpty() ? null : enlace);
        p.setDictamenNumero(generarDictamen(p.getId(), req.getFecha()));
        proyectoRepository.save(p);
    }

    /** Sin modalidad indicada se asume presencial, que es como se venía programando. */
    private ModalidadDefensa modalidadDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return ModalidadDefensa.PRESENCIAL;
        }
        try {
            return ModalidadDefensa.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Modalidad no válida");
        }
    }

    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    /** N° de dictamen referencial de la defensa. */
    private String generarDictamen(UUID proyectoId, LocalDate fecha) {
        long correlativo = Math.abs(proyectoId.getLeastSignificantBits() % 1000000);
        int anio = fecha != null ? fecha.getYear() : LocalDate.now().getYear();
        return String.format("DICTAMEN N° %06d-%d-UPG-VDIP-FM/UNMSM", correlativo, anio);
    }

    private String nombre(String nombres, String apPat, String apMat) {
        return ((apPat != null ? apPat : "") + " " + (apMat != null ? apMat : "") + " "
                + (nombres != null ? nombres : "")).trim().replaceAll("\\s+", " ");
    }

    private boolean esWord(String nombre, String contentType) {
        String n = nombre != null ? nombre.toLowerCase() : "";
        return n.endsWith(".docx")
                || (contentType != null && contentType.toLowerCase().contains("wordprocessingml"));
    }

    @Override
    @Transactional
    public unmsm.edu.pe.tesis.application.dto.ArchivoDescargable documentoRubrica(UUID tesisId) {
        DocumentoTesis doc = documentoTesisRepository.buscarPorTesisYTipo(tesisId, RevisorProyectoServiceImpl.T_RUBRICA)
                .orElseThrow(() -> new NotFoundException("Aún no se ha subido la rúbrica de este proyecto"));
        byte[] bytes = almacenamiento.obtener(doc.getStorageKey());
        String ct = doc.getContentType() != null ? doc.getContentType() : WORD_CT;
        String nombre = doc.getNombreOriginal() != null ? doc.getNombreOriginal() : "rubrica.docx";
        return new unmsm.edu.pe.tesis.application.dto.ArchivoDescargable(bytes, ct, nombre);
    }

    @Override
    @Transactional
    public List<String> previsualizarRubrica(UUID tesisId) {
        DocumentoTesis doc = documentoTesisRepository.buscarPorTesisYTipo(tesisId, RevisorProyectoServiceImpl.T_RUBRICA)
                .orElseThrow(() -> new NotFoundException("Aún no se ha subido la rúbrica de este proyecto"));
        byte[] bytes = almacenamiento.obtener(doc.getStorageKey());
        List<String> lineas = new ArrayList<>();
        try (org.apache.poi.xwpf.usermodel.XWPFDocument d =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
            for (org.apache.poi.xwpf.usermodel.IBodyElement el : d.getBodyElements()) {
                if (el instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph par) {
                    String t = par.getText();
                    if (t != null && !t.isBlank()) lineas.add(t.trim());
                } else if (el instanceof org.apache.poi.xwpf.usermodel.XWPFTable table) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableRow tr : table.getRows()) {
                        String row = tr.getTableCells().stream()
                                .map(c -> c.getText().replaceAll("\\s+", " ").trim())
                                .collect(Collectors.joining("  |  "));
                        if (!row.isBlank()) lineas.add(row);
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("No se pudo leer la vista previa del documento");
        }
        return lineas;
    }

    private String nombreDocente(UUID personaId) {
        if (personaId == null) return null;
        return personaRepository.buscarPorId(personaId)
                .map(pe -> (nz(pe.getNombres()) + " " + nz(pe.getApellidoPaterno()) + " " + nz(pe.getApellidoMaterno()))
                        .trim().replaceAll("\\s+", " "))
                .orElse(null);
    }

    private String nz(String s) { return s == null ? "" : s; }

    private ExpedienteBandejaItem toItem(Object[] r) {
        return ExpedienteBandejaItem.builder()
                .tesisId((UUID) r[0])
                .estudianteApellidos((asStr(r[1]) + " " + asStr(r[2])).trim())
                .estudianteNombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .programaNombre(asStr(r[5]))
                .tituloTesis(asStr(r[6]))
                .fechaSolicitud(toLocalDate(r[7]))
                .recibido(Boolean.TRUE.equals(r[8]))
                .build();
    }

    private LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        return null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
}
