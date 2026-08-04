package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.DictamenDesignacion;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService;
import unmsm.edu.pe.tesis.domain.services.SecretariaDictamenService;
import unmsm.edu.pe.tesis.infrastructure.export.DocumentoAsesoriaRenderer;
import unmsm.edu.pe.tesis.infrastructure.export.PlantillaDictamen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SecretariaDictamenServiceImpl implements SecretariaDictamenService {

    @Inject SecurityUtils securityUtils;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject DictamenDesignacionRepository dictamenRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject PersonaGradoAcademicoRepository gradoRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject ResolverDatosPlantillaService resolverPlantilla;
    @Inject DocumentoAsesoriaRenderer renderer;
    @Inject com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final String T_SOL = "SOLICITUD_ASESORIA_FIRMADA";
    private static final String T_CARTA = "CARTA_ACEPTACION_FIRMADA";
    private static final String T_DICT = "DICTAMEN_DESIGNACION_FIRMADO";

    @Override
    public PageResponse<DictamenBandejaItem> bandeja(String estado, UUID facultadId, UUID programaId, String buscar, int page, int size) {
        List<DictamenBandejaItem> content = dictamenRepository.bandeja(estado, facultadId, programaId, buscar, page, size)
                .stream().map(this::toBandejaItem).collect(Collectors.toList());
        long total = dictamenRepository.contarBandeja(estado, facultadId, programaId, buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    public DictamenResumen resumen() {
        return DictamenResumen.builder()
                .porElaborar(dictamenRepository.contarPorEstado("POR_ELABORAR"))
                .elaborados(dictamenRepository.contarPorEstado("ELABORADO"))
                .firmados(dictamenRepository.contarPorEstado("FIRMADO"))
                .build();
    }

    @Override
    public DictamenDetalle detalle(UUID tesisId) {
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        Estudiante e = estudiante(tesisId);
        Docente asesor = docenteDe(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR").orElse(null));
        Docente coasesor = docenteDe(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR").orElse(null));
        DictamenDesignacion dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
        Persona pe = e != null ? e.getPersona() : null;
        ProgramaPosgrado prog = e != null ? e.getPrograma() : null;
        return DictamenDetalle.builder()
                .tesisId(tesisId)
                .estudianteNombre(apellidosNombres(pe))
                .codigoSistema(e != null ? e.getCodigoSistema() : null)
                .programaNombre(prog != null ? prog.getNombre() : null)
                .tituloTesis(tesis.getTitulo())
                .asesorNombre(asesor != null ? nombresApellidos(asesor.getPersona()) : null)
                .asesorGrado(asesor != null ? grado(asesor.getPersona()) : null)
                .coasesorNombre(coasesor != null ? nombresApellidos(coasesor.getPersona()) : null)
                .coasesorGrado(coasesor != null ? grado(coasesor.getPersona()) : null)
                .numero(dic != null ? dic.getNumero() : null)
                .expediente(dic != null ? dic.getExpediente() : null)
                .fechaSolicitud(dic != null ? dic.getFechaSolicitud() : null)
                .estado(dic != null && dic.getEstado() != null ? dic.getEstado().name() : null)
                .motivoObservacion(dic != null ? dic.getMotivoObservacion() : null)
                .solicitudFirmadaDisponible(documentoTesisRepository.existePorTesisYTipo(tesisId, T_SOL))
                .cartaFirmadaDisponible(documentoTesisRepository.existePorTesisYTipo(tesisId, T_CARTA))
                .dictamenFirmadoSubido(documentoTesisRepository.existePorTesisYTipo(tesisId, T_DICT))
                .build();
    }

    @Override
    @Transactional
    public ArchivoDescargable descargarFirmadoEstudiante(UUID tesisId, String tipo) {
        String t = "carta".equalsIgnoreCase(tipo) || "carta_aceptacion".equalsIgnoreCase(tipo) ? T_CARTA : T_SOL;
        DocumentoTesis doc = documentoTesisRepository.buscarPorTesisYTipo(tesisId, t)
                .orElseThrow(() -> new BusinessException("El estudiante aún no ha subido ese documento firmado"));
        byte[] bytes = almacenamiento.obtener(doc.getStorageKey());
        return new ArchivoDescargable(bytes, doc.getContentType(), doc.getNombreOriginal());
    }

    @Override
    @Transactional
    public DictamenResponse elaborar(UUID tesisId, ElaborarDictamenRequest req) {
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        if (!documentoTesisRepository.tieneFirmadosCompletos(tesisId)) {
            throw new BusinessException("El estudiante aún no ha subido los dos documentos firmados");
        }
        Asesoria asesoria = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .orElseThrow(() -> new BusinessException("La tesis no tiene un asesor designado"));
        Docente asesor = docenteDe(asesoria);
        Docente coasesor = docenteDe(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR").orElse(null));
        Estudiante e = estudiante(tesisId);

        DictamenDesignacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseGet(() -> DictamenDesignacion.builder().tesisId(tesisId).estado(EstadoDictamen.POR_ELABORAR).build());
        if (dic.getEstado() == EstadoDictamen.FIRMADO) {
            throw new BusinessException("El dictamen ya fue firmado; no puede re-elaborarse");
        }

        int anio = LocalDate.now().getYear();
        if (dic.getNumero() == null || dic.getAnio() == null || dic.getAnio() != anio) {
            int corr = dictamenRepository.siguienteCorrelativo(anio);
            dic.setAnio(anio);
            dic.setCorrelativo(corr);
            dic.setNumero(formatoNumero(corr, anio, e));
            dic.setExpediente(formatoExpediente(corr, anio));
        }
        // Numeración manual: la UPG lleva sus propios correlativos, así que lo que escriba la
        // Secretaría manda. El correlativo calculado arriba queda solo como propuesta inicial.
        if (req.getNumero() != null && !req.getNumero().isBlank()) {
            dic.setNumero(req.getNumero().trim());
        }
        if (req.getExpediente() != null && !req.getExpediente().isBlank()) {
            dic.setExpediente(req.getExpediente().trim());
        }
        dic.setFechaSolicitud(req.getFechaSolicitud());
        dic.setEstado(EstadoDictamen.ELABORADO);
        dic.setMotivoObservacion(null);

        var datos = resolverPlantilla.resolverDictamen(dic, tesis, e, asesor, coasesor, LocalDate.now());
        dic.setDatosDictamen(json(datos));
        dictamenRepository.save(dic);

        return toResponse(dic);
    }

    @Override
    @Transactional
    public byte[] documentoDictamen(UUID tesisId, String formato) {
        DictamenDesignacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido elaborado"));
        if (dic.getDatosDictamen() == null || dic.getDatosDictamen().isBlank()) {
            throw new BusinessException("El dictamen aún no ha sido elaborado");
        }
        java.util.Map<String, String> datos;
        try {
            datos = objectMapper.readValue(dic.getDatosDictamen(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, String>>() {});
        } catch (Exception ex) {
            throw new BusinessException("No se pudo leer el dictamen elaborado");
        }
        boolean docx = "docx".equalsIgnoreCase(formato != null ? formato.trim() : "pdf");
        return docx ? renderer.docx(PlantillaDictamen.DICTAMEN, datos) : renderer.pdf(PlantillaDictamen.DICTAMEN, datos);
    }

    @Override
    @Transactional
    public void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        DictamenDesignacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido elaborado"));
        if (dic.getEstado() != EstadoDictamen.ELABORADO && dic.getEstado() != EstadoDictamen.FIRMADO) {
            throw new BusinessException("Debes elaborar el dictamen antes de subir el firmado");
        }
        validarArchivo(contenido, contentType);
        UUID actor = securityUtils.getCurrentUserIdAsUUID();
        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesisId, T_DICT);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        DocumentoTesis doc = existente.orElseGet(() -> DocumentoTesis.builder().tesisId(tesisId).tipo(T_DICT).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setHashSha256(sha256(contenido));
        doc.setFechaCarga(LocalDateTime.now());
        doc.setSubidoPor(actor);
        documentoTesisRepository.save(doc);

        dic.setEstado(EstadoDictamen.FIRMADO);
        dic.setFechaEmision(LocalDate.now());
        dictamenRepository.save(dic);
    }

    @Override
    @Transactional
    public void observar(UUID tesisId, String motivo) {
        DictamenDesignacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("No hay dictamen para esta tesis"));
        if (dic.getEstado() == EstadoDictamen.FIRMADO) {
            throw new BusinessException("El dictamen ya fue firmado; no puede observarse");
        }
        dic.setEstado(EstadoDictamen.OBSERVADO);
        dic.setMotivoObservacion(motivo != null ? motivo.trim() : null);
        dictamenRepository.save(dic);
    }

    // ── helpers ──
    /** N° de expediente digital: correlativo automático por año, ej. EXP-2026-000123. */
    private String formatoExpediente(int correlativo, int anio) {
        return String.format("EXP-%04d-%06d", anio, correlativo);
    }

    private String formatoNumero(int correlativo, int anio, Estudiante e) {
        String facCod = "UNMSM";
        if (e != null && e.getPrograma() != null && e.getPrograma().getFacultad() != null
                && e.getPrograma().getFacultad().getCodigo() != null) {
            facCod = e.getPrograma().getFacultad().getCodigo();
        }
        return String.format("%06d-%04d-UPG-VDIP-%s/UNMSM", correlativo, anio, facCod);
    }

    private DictamenBandejaItem toBandejaItem(Object[] r) {
        UUID tesisId = (UUID) r[0];
        Docente asesor = docenteDe(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR").orElse(null));
        Docente coasesor = docenteDe(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR").orElse(null));
        return DictamenBandejaItem.builder()
                .tesisId(tesisId)
                .estudianteApellidos(((asStr(r[1]) + " " + asStr(r[2])).trim()))
                .estudianteNombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .programaNombre(asStr(r[5]))
                .tituloTesis(asStr(r[6]))
                .estadoDictamen(asStr(r[7]))
                .numero(asStr(r[8]))
                .asesorNombre(asesor != null ? nombresApellidos(asesor.getPersona()) : null)
                .coasesorNombre(coasesor != null ? nombresApellidos(coasesor.getPersona()) : null)
                .build();
    }

    private Estudiante estudiante(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private Docente docenteDe(Asesoria a) {
        return a != null ? docenteRepository.findByPersonaId(a.getDocenteId()).orElse(null) : null;
    }

    private String grado(Persona p) {
        if (p == null) return null;
        String g = gradoRepository.gradoPrincipal(p.getId());
        return unmsm.edu.pe.tesis.application.util.FormatoDocumentos.tratamientoPorGrado(g, p.getSexo() != null ? p.getSexo().name() : null);
    }

    private String nombresApellidos(Persona p) {
        if (p == null) return "";
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno())).trim().replaceAll("\\s+", " ");
    }

    private String apellidosNombres(Persona p) {
        if (p == null) return "";
        String ap = (nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno())).trim().replaceAll("\\s+", " ");
        return (ap + ", " + nz(p.getNombres())).trim();
    }

    private DictamenResponse toResponse(DictamenDesignacion d) {
        return DictamenResponse.builder()
                .id(d.getId()).tesisId(d.getTesisId()).numero(d.getNumero())
                .estado(d.getEstado() != null ? d.getEstado().name() : null)
                .expediente(d.getExpediente()).fechaSolicitud(d.getFechaSolicitud()).fechaEmision(d.getFechaEmision())
                .build();
    }

    private String json(java.util.Map<String, String> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception ex) {
            throw new BusinessException("No se pudo generar el dictamen: " + ex.getMessage());
        }
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
        } catch (Exception e) { return null; }
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
    private String nz(String s) { return s == null ? "" : s; }
}
