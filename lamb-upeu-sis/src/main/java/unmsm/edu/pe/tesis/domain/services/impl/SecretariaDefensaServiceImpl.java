package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.DefensaInfo;
import unmsm.edu.pe.tesis.application.dto.DocenteOpcion;
import unmsm.edu.pe.tesis.application.dto.ExpedienteBandejaItem;
import unmsm.edu.pe.tesis.application.dto.JuradoItem;
import unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest;
import unmsm.edu.pe.tesis.application.dto.RubricaBandejaItem;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.ProyectoJurado;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.RolJurado;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
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
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaRepository personaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.ProyectoJuradoRepository juradoRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository asesoriaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.TesisRepository tesisRepository;
    @Inject unmsm.edu.pe.personas.domain.repositories.DocenteRepository docenteRepository;
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
    public List<DocenteOpcion> docentesDefensa(UUID tesisId) {
        // Solo docentes de la línea de investigación de la tesis (mismo criterio que revisores).
        UUID lineaId = tesisRepository.buscarPorId(tesisId)
                .map(Tesis::getLineaInvestigacion)
                .map(LineaInvestigacion::getId)
                .orElse(null);
        List<Object[]> filas = lineaId != null
                ? revisorRepository.docentesOpcionPorLinea(lineaId)
                : List.of();
        return filas.stream()
                .map(r -> DocenteOpcion.builder()
                        .id((UUID) r[0])
                        .nombre(nombre(asStr(r[1]), asStr(r[2]), asStr(r[3])))
                        .categoria(r[4] != null ? r[4].toString() : null)
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
        if (req == null || req.getPresidenteId() == null || req.getMiembroIds() == null || req.getFecha() == null) {
            throw new ValidationException("Indica presidente, 2 miembros y la fecha de la defensa");
        }
        List<UUID> miembros = new ArrayList<>(new java.util.LinkedHashSet<>(req.getMiembroIds()));
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
