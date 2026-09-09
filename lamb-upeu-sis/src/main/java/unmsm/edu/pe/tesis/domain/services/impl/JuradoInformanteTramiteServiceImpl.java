package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenExpeditoRequest;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenJuradoInformeRequest;
import unmsm.edu.pe.tesis.application.dto.JuradoInformeBandejaItem;
import unmsm.edu.pe.tesis.application.dto.JuradoInformeTramiteResponse;
import unmsm.edu.pe.tesis.application.dto.JuradoItem;
import unmsm.edu.pe.tesis.domain.entities.DictamenExpedito;
import unmsm.edu.pe.tesis.domain.entities.DictamenJuradoInforme;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenExpeditoRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenJuradoInformeRepository;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.JuradoInformanteTramiteService;
import unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService;
import unmsm.edu.pe.tesis.infrastructure.export.DocumentoAsesoriaRenderer;
import unmsm.edu.pe.tesis.infrastructure.export.PlantillaDictamen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JuradoInformanteTramiteServiceImpl implements JuradoInformanteTramiteService {

    public static final String T_DICTAMEN_JURADO_INFORME = "DICTAMEN_JURADO_INFORME_FIRMADO";
    public static final String T_EXPEDIENTE_INFORME_ARCHIVADO = "EXPEDIENTE_INFORME_ARCHIVADO";
    public static final String T_DICTAMEN_EXPEDITO = "DICTAMEN_EXPEDITO_FIRMADO";

    private static final java.util.Set<String> TIPOS_PERMITIDOS = java.util.Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword");

    @Inject SecurityUtils securityUtils;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject InformeRevisorRepository informeRevisorRepository;
    @Inject DictamenJuradoInformeRepository dictamenRepository;
    @Inject DictamenExpeditoRepository expeditoRepository;
    @Inject DictamenDesignacionRepository correlativoRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject PersonaRepository personaRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject ResolverDatosPlantillaService resolverPlantilla;
    @Inject DocumentoAsesoriaRenderer renderer;
    @Inject com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<JuradoInformeBandejaItem> bandeja(String buscar) {
        List<JuradoInformeBandejaItem> out = new ArrayList<>();
        for (Object[] r : proyectoRepository.bandejaDefensa(buscar, 0, 100)) {
            boolean solicitado = r.length > 13 && Boolean.TRUE.equals(r[13]);
            if (!solicitado) {
                continue;   // el trámite empieza cuando el estudiante solicita el Jurado Informante
            }
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
            if (p == null) continue;
            int numJurado = r.length > 14 && r[14] != null ? ((Number) r[14]).intValue() : 0;
            boolean informeFinalRevisado = r.length > 15 && Boolean.TRUE.equals(r[15]);
            DictamenJuradoInforme dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
            DictamenExpedito expedito = expeditoRepository.buscarPorTesisId(tesisId).orElse(null);

            out.add(JuradoInformeBandejaItem.builder()
                    .tesisId(tesisId)
                    .estudianteNombre((asStr(r[2]) + " " + asStr(r[3]) + ", " + asStr(r[4])).trim())
                    .codigoSistema(asStr(r[5]))
                    .programaNombre(asStr(r[6]))
                    .tituloTesis(asStr(r[7]))
                    .fechaSolicitud(p.getFechaJuradoInformante())
                    .expedienteRecibido(Boolean.TRUE.equals(p.getExpedienteInformeRecibido()))
                    .numJurado(numJurado)
                    .informeFinalRevisado(informeFinalRevisado)
                    .estadoDictamen(dic != null ? dic.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                    .informeFinalArchivado(Boolean.TRUE.equals(p.getInformeFinalArchivado()))
                    .estadoExpedito(expedito != null ? expedito.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                    .pendiente(pendiente(p, numJurado, informeFinalRevisado, dic, expedito))
                    .build());
        }
        return out;
    }

    private String pendiente(ProyectoTesis p, int numJurado, boolean informeFinalRevisado,
                              DictamenJuradoInforme dic, DictamenExpedito expedito) {
        if (expedito != null && expedito.getEstado() == EstadoDictamen.FIRMADO) {
            return "Cerrado";
        }
        if (!Boolean.TRUE.equals(p.getExpedienteInformeRecibido())) {
            return "Recepcionar el expediente y comunicar al Coordinador";
        }
        if (numJurado < 3) {
            return "Esperando que el Coordinador designe al Jurado Informante";
        }
        if (dic == null || dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            return "Elaborar el dictamen de designación del Jurado";
        }
        if (dic.getEstado() != EstadoDictamen.FIRMADO) {
            return "Subir el dictamen de designación firmado";
        }
        if (!informeFinalRevisado) {
            return "Esperando la conformidad del Jurado Informante";
        }
        if (!Boolean.TRUE.equals(p.getInformeFinalArchivado())) {
            return "Archivar el expediente del Jurado Informante";
        }
        if (expedito == null || expedito.getEstado() == EstadoDictamen.POR_ELABORAR) {
            return "Elaborar el Dictamen de Expedito";
        }
        return "Subir el Dictamen de Expedito firmado";
    }

    @Override
    @Transactional
    public JuradoInformeTramiteResponse detalle(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElse(null);
        Estudiante e = estudiante(tesisId);
        Persona pe = e != null ? e.getPersona() : null;

        List<JuradoItem> jurado = informeRevisorRepository.listarPorProyecto(p.getId()).stream()
                .sorted(Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> JuradoItem.builder()
                        .docenteId(rv.getDocenteId())
                        .docenteNombre(nombreDocente(rv.getDocenteId()))
                        .rol(Boolean.TRUE.equals(rv.getPresidente()) ? "PRESIDENTE" : "MIEMBRO")
                        .orden(rv.getOrden())
                        .build())
                .collect(Collectors.toList());

        DictamenJuradoInforme dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
        DictamenExpedito expedito = expeditoRepository.buscarPorTesisId(tesisId).orElse(null);
        boolean informeFinalRevisado = Boolean.TRUE.equals(p.getInformeFinalRevisado());
        int anio = LocalDate.now().getYear();

        return JuradoInformeTramiteResponse.builder()
                .tesisId(tesisId)
                .estudianteNombre(nombre(pe))
                .codigoSistema(e != null ? e.getCodigoSistema() : null)
                .programaNombre(e != null && e.getPrograma() != null ? e.getPrograma().getNombre() : null)
                .tituloTesis(tesis != null ? tesis.getTitulo() : null)
                .fechaSolicitud(p.getFechaJuradoInformante())
                .expedienteRecibido(Boolean.TRUE.equals(p.getExpedienteInformeRecibido()))
                .fechaRecepcion(p.getFechaRecepcionInforme())
                .jurado(jurado)
                .informeFinalRevisado(informeFinalRevisado)
                .estadoDictamen(dic != null ? dic.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                .dictamenNumero(dic != null ? dic.getNumero() : null)
                .dictamenExpediente(dic != null ? dic.getExpediente() : null)
                .dictamenFechaEmision(dic != null ? dic.getFechaEmision() : null)
                .dictamenElaborado(dic != null && dic.getDatosDictamen() != null && !dic.getDatosDictamen().isBlank())
                .dictamenFirmadoSubido(documentoTesisRepository.existePorTesisYTipo(tesisId, T_DICTAMEN_JURADO_INFORME))
                .numeroSugerido(dic != null ? dic.getNumero() : null)
                .expedienteSugerido(dic != null && dic.getExpediente() != null ? dic.getExpediente() : String.format("EXP-%04d-", anio))
                .informeFinalArchivado(Boolean.TRUE.equals(p.getInformeFinalArchivado()))
                .fechaArchivoInforme(p.getFechaArchivoInforme())
                .estadoExpedito(expedito != null ? expedito.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                .expeditoNumero(expedito != null ? expedito.getNumero() : null)
                .expeditoExpediente(expedito != null ? expedito.getExpediente() : null)
                .expeditoFechaEmision(expedito != null ? expedito.getFechaEmision() : null)
                .expeditoElaborado(expedito != null && expedito.getDatosDictamen() != null && !expedito.getDatosDictamen().isBlank())
                .expeditoFirmadoSubido(documentoTesisRepository.existePorTesisYTipo(tesisId, T_DICTAMEN_EXPEDITO))
                .expeditoNumeroSugerido(expedito != null ? expedito.getNumero() : null)
                .expeditoExpedienteSugerido(expedito != null && expedito.getExpediente() != null ? expedito.getExpediente() : String.format("EXP-%04d-", anio))
                .pendiente(pendiente(p, jurado.size(), informeFinalRevisado, dic, expedito))
                .build();
    }

    @Override
    @Transactional
    public void recepcionar(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getJuradoInformanteSolicitado())) {
            throw new BusinessException("El estudiante aún no ha solicitado el Jurado Informante");
        }
        if (Boolean.TRUE.equals(p.getExpedienteInformeRecibido())) {
            throw new BusinessException("Ese expediente ya fue recepcionado");
        }
        p.setExpedienteInformeRecibido(true);
        p.setFechaRecepcionInforme(LocalDate.now());
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void elaborarDictamen(UUID tesisId, ElaborarDictamenJuradoInformeRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getExpedienteInformeRecibido())) {
            throw new BusinessException("Primero recepciona el expediente");
        }
        List<InformeRevisor> jurado = informeRevisorRepository.listarPorProyecto(p.getId());
        if (jurado.size() < 3) {
            throw new BusinessException("El Coordinador aún no designó a los 3 miembros del Jurado Informante");
        }
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        Estudiante e = estudiante(tesisId);

        DictamenJuradoInforme dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseGet(() -> DictamenJuradoInforme.builder().tesisId(tesisId).estado(EstadoDictamen.POR_ELABORAR).build());
        if (dic.getEstado() == EstadoDictamen.FIRMADO) {
            throw new BusinessException("El dictamen ya fue firmado; no puede re-elaborarse");
        }
        numerarSiCorresponde(dic, e);
        if (req != null && req.getNumero() != null && !req.getNumero().isBlank()) dic.setNumero(req.getNumero().trim());
        if (req != null && req.getExpediente() != null && !req.getExpediente().isBlank()) dic.setExpediente(req.getExpediente().trim());
        dic.setEstado(EstadoDictamen.ELABORADO);

        List<String> nombres = jurado.stream().sorted(Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> nombreDocente(rv.getDocenteId())).filter(n -> n != null && !n.isBlank()).collect(Collectors.toList());
        var datos = resolverPlantilla.resolverDictamenJuradoInforme(dic, tesis, e, nombres, LocalDate.now());
        dic.setDatosDictamen(json(datos));
        dictamenRepository.save(dic);
    }

    private void numerarSiCorresponde(DictamenJuradoInforme dic, Estudiante e) {
        int anio = LocalDate.now().getYear();
        if (dic.getNumero() == null || dic.getAnio() == null || dic.getAnio() != anio) {
            int corr = correlativoRepository.siguienteCorrelativo(anio);
            dic.setAnio(anio);
            dic.setCorrelativo(corr);
            dic.setNumero(formatoNumero(corr, anio, e));
            dic.setExpediente(String.format("EXP-%04d-%06d", anio, corr));
        }
    }

    @Override
    @Transactional
    public byte[] documentoDictamen(UUID tesisId, String formato) {
        DictamenJuradoInforme dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido elaborado"));
        return renderizar(dic.getDatosDictamen(), formato, PlantillaDictamen.JURADO_INFORME);
    }

    @Override
    @Transactional
    public void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        DictamenJuradoInforme dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido elaborado"));
        if (dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            throw new BusinessException("Debes elaborar el dictamen antes de subir el firmado");
        }
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_DICTAMEN_JURADO_INFORME, contenido, nombreOriginal, contentType);
        dic.setEstado(EstadoDictamen.FIRMADO);
        dic.setFechaEmision(LocalDate.now());
        dictamenRepository.save(dic);
    }

    @Override
    @Transactional
    public void archivarExpediente(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getInformeFinalRevisado())) {
            throw new BusinessException("El Jurado Informante aún no dio conformidad al informe final");
        }
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_EXPEDIENTE_INFORME_ARCHIVADO, contenido, nombreOriginal, contentType);
        p.setInformeFinalArchivado(true);
        p.setFechaArchivoInforme(LocalDate.now());
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void elaborarDictamenExpedito(UUID tesisId, ElaborarDictamenExpeditoRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getInformeFinalArchivado())) {
            throw new BusinessException("Primero archiva el expediente del Jurado Informante");
        }
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        Estudiante e = estudiante(tesisId);

        DictamenExpedito dic = expeditoRepository.buscarPorTesisId(tesisId)
                .orElseGet(() -> DictamenExpedito.builder().tesisId(tesisId).estado(EstadoDictamen.POR_ELABORAR).build());
        if (dic.getEstado() == EstadoDictamen.FIRMADO) {
            throw new BusinessException("El Dictamen de Expedito ya fue firmado; no puede re-elaborarse");
        }
        int anio = LocalDate.now().getYear();
        if (dic.getNumero() == null || dic.getAnio() == null || dic.getAnio() != anio) {
            int corr = correlativoRepository.siguienteCorrelativo(anio);
            dic.setAnio(anio);
            dic.setCorrelativo(corr);
            dic.setNumero(formatoNumero(corr, anio, e));
            dic.setExpediente(String.format("EXP-%04d-%06d", anio, corr));
        }
        if (req != null && req.getNumero() != null && !req.getNumero().isBlank()) dic.setNumero(req.getNumero().trim());
        if (req != null && req.getExpediente() != null && !req.getExpediente().isBlank()) dic.setExpediente(req.getExpediente().trim());
        dic.setEstado(EstadoDictamen.ELABORADO);

        List<String> nombresJurado = informeRevisorRepository.listarPorProyecto(p.getId()).stream()
                .sorted(Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> nombreDocente(rv.getDocenteId())).filter(n -> n != null && !n.isBlank()).collect(Collectors.toList());
        var datos = resolverPlantilla.resolverDictamenExpedito(dic, tesis, e, nombresJurado, LocalDate.now());
        dic.setDatosDictamen(json(datos));
        expeditoRepository.save(dic);
    }

    @Override
    @Transactional
    public byte[] documentoExpedito(UUID tesisId, String formato) {
        DictamenExpedito dic = expeditoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El Dictamen de Expedito aún no ha sido elaborado"));
        return renderizar(dic.getDatosDictamen(), formato, PlantillaDictamen.EXPEDITO);
    }

    @Override
    @Transactional
    public void subirDictamenExpeditoFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        DictamenExpedito dic = expeditoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El Dictamen de Expedito aún no ha sido elaborado"));
        if (dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            throw new BusinessException("Debes elaborar el dictamen antes de subir el firmado");
        }
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_DICTAMEN_EXPEDITO, contenido, nombreOriginal, contentType);
        dic.setEstado(EstadoDictamen.FIRMADO);
        dic.setFechaEmision(LocalDate.now());
        expeditoRepository.save(dic);
    }

    // ── utilidades ──
    private byte[] renderizar(String datosDictamenJson, String formato, String plantilla) {
        if (datosDictamenJson == null || datosDictamenJson.isBlank()) {
            throw new BusinessException("El dictamen aún no ha sido elaborado");
        }
        java.util.Map<String, String> datos;
        try {
            datos = objectMapper.readValue(datosDictamenJson,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, String>>() {});
        } catch (Exception ex) {
            throw new BusinessException("No se pudo leer el dictamen elaborado");
        }
        boolean docx = "docx".equalsIgnoreCase(formato != null ? formato.trim() : "pdf");
        return docx ? renderer.docx(plantilla, datos) : renderer.pdf(plantilla, datos);
    }

    private void guardarDocumento(UUID tesisId, String tipo, byte[] contenido, String nombreOriginal, String contentType) {
        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesisId, tipo);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        DocumentoTesis doc = existente.orElseGet(() -> DocumentoTesis.builder().tesisId(tesisId).tipo(tipo).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setFechaCarga(LocalDateTime.now());
        doc.setSubidoPor(securityUtils.getCurrentUserIdAsUUID());
        documentoTesisRepository.save(doc);
    }

    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId).orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    private Estudiante estudiante(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private String formatoNumero(int correlativo, int anio, Estudiante e) {
        String facCod = "UNMSM";
        if (e != null && e.getPrograma() != null && e.getPrograma().getFacultad() != null
                && e.getPrograma().getFacultad().getCodigo() != null) {
            facCod = e.getPrograma().getFacultad().getCodigo();
        }
        return String.format("%06d-%04d-UPG-VDIP-%s/UNMSM", correlativo, anio, facCod);
    }

    private void validarArchivo(byte[] contenido, String contentType) {
        if (contenido == null || contenido.length == 0) throw new ValidationException("El archivo está vacío");
        if (contenido.length > 10 * 1024 * 1024) throw new ValidationException("El archivo supera 10 MB");
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType))
            throw new ValidationException("Formato no permitido; sube un PDF o Word (.docx)");
    }

    private String json(java.util.Map<String, String> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception ex) {
            throw new BusinessException("No se pudo preparar el dictamen");
        }
    }

    private String nombreDocente(UUID personaId) {
        if (personaId == null) return null;
        return personaRepository.buscarPorId(personaId).map(this::nombre).orElse(null);
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }

    private String nz(String s) { return s == null ? "" : s; }
}
