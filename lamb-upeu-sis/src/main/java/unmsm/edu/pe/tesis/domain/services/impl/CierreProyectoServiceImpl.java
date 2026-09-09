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
import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.CierreBandejaItem;
import unmsm.edu.pe.tesis.application.dto.CierreProyectoResponse;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenAprobacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarResultadoDefensaRequest;
import unmsm.edu.pe.tesis.application.dto.RubricaDefensaItem;
import unmsm.edu.pe.tesis.domain.entities.DictamenAprobacion;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.RubricaDefensa;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;
import unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa;
import unmsm.edu.pe.tesis.domain.repositories.DictamenAprobacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRubricaPuntajeRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.RubricaDefensaRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.CierreProyectoService;
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
public class CierreProyectoServiceImpl implements CierreProyectoService {

    /** Documentos del cierre en el expediente. */
    public static final String T_DICTAMEN_APROBACION = "DICTAMEN_APROBACION_FIRMADO";
    public static final String T_PROYECTO_ARCHIVADO = "PROYECTO_FINAL_APROBADO";
    private static final String T_PROYECTO_FINAL = "PROYECTO_VERSION_FINAL";

    /** Años de vigencia del proyecto aprobado para ejecutarse y sustentarse. */
    private static final int ANIOS_VIGENCIA = 4;

    private static final java.util.Set<String> TIPOS_PERMITIDOS = java.util.Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword");

    @Inject SecurityUtils securityUtils;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject ProyectoRubricaPuntajeRepository puntajeRepository;
    @Inject RubricaDefensaRepository rubricaRepository;
    @Inject DictamenAprobacionRepository dictamenRepository;
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

    // ── Bandeja ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<CierreBandejaItem> bandeja(String buscar) {
        List<CierreBandejaItem> out = new ArrayList<>();
        for (Object[] r : proyectoRepository.bandejaDefensa(buscar, 0, 100)) {
            boolean programada = r.length > 11 && Boolean.TRUE.equals(r[11]);
            if (!programada) {
                continue;   // el cierre empieza cuando la defensa ya tiene fecha
            }
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
            if (p == null) {
                continue;
            }
            int esperadas = (int) revisorRepository.contarPorProyecto(proyectoId);
            int recibidas = rubricaRepository.porTesis(tesisId).size();
            DictamenAprobacion dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
            boolean archivado = documentoTesisRepository.existePorTesisYTipo(tesisId, T_PROYECTO_ARCHIVADO);

            out.add(CierreBandejaItem.builder()
                    .tesisId(tesisId)
                    .estudianteNombre((asStr(r[2]) + " " + asStr(r[3]) + ", " + asStr(r[4])).trim())
                    .codigoSistema(asStr(r[5]))
                    .programaNombre(asStr(r[6]))
                    .tituloTesis(asStr(r[7]))
                    .fechaDefensa(p.getFechaDefensa())
                    .horaDefensa(p.getHoraDefensa())
                    .modalidad(p.getModalidadDefensa() != null ? p.getModalidadDefensa().name() : null)
                    .modalidadLabel(p.getModalidadDefensa() != null ? p.getModalidadDefensa().etiqueta() : null)
                    .rubricasRecibidas(recibidas)
                    .rubricasEsperadas(esperadas)
                    .defensaRealizada(Boolean.TRUE.equals(p.getDefensaRealizada()))
                    .resultado(p.getResultadoDefensa() != null ? p.getResultadoDefensa().name() : null)
                    .resultadoLabel(p.getResultadoDefensa() != null ? p.getResultadoDefensa().etiqueta() : null)
                    .estadoDictamen(dic != null ? dic.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                    .dictamenNumero(dic != null ? dic.getNumero() : null)
                    .proyectoFinalArchivado(archivado)
                    .cerrado(Boolean.TRUE.equals(p.getProyectoAprobado()))
                    .pendiente(pendiente(p, recibidas, esperadas, dic, archivado))
                    .build());
        }
        return out;
    }

    /** Una sola frase con lo que le toca hacer a la Secretaría en este expediente. */
    private String pendiente(ProyectoTesis p, int recibidas, int esperadas, DictamenAprobacion dic, boolean archivado) {
        if (Boolean.TRUE.equals(p.getProyectoAprobado())) {
            return "Cerrado";
        }
        if (recibidas < esperadas) {
            return "Recepcionar las rúbricas de la defensa (" + recibidas + " de " + esperadas + ")";
        }
        if (!Boolean.TRUE.equals(p.getDefensaRealizada())) {
            return "Registrar el resultado de la defensa";
        }
        if (p.getResultadoDefensa() == ResultadoDefensa.DESAPROBADO) {
            return "Proyecto desaprobado en la defensa";
        }
        if (dic == null || dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            return "Elaborar el dictamen de aprobación";
        }
        if (dic.getEstado() != EstadoDictamen.FIRMADO) {
            return "Subir el dictamen firmado";
        }
        if (!archivado) {
            return "Archivar el proyecto final";
        }
        return "Cerrado";
    }

    // ── Estado del cierre ──────────────────────────────────────────────────

    @Override
    @Transactional
    public CierreProyectoResponse estado(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElse(null);
        Estudiante e = estudiante(tesisId);
        Persona pe = e != null ? e.getPersona() : null;

        List<RubricaDefensaItem> rubricas = rubricasDe(tesisId, p.getId());
        int recibidas = (int) rubricas.stream().filter(RubricaDefensaItem::isRecibida).count();
        DictamenAprobacion dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
        boolean firmadoSubido = documentoTesisRepository.existePorTesisYTipo(tesisId, T_DICTAMEN_APROBACION);
        var archivado = documentoTesisRepository.buscarPorTesisYTipo(tesisId, T_PROYECTO_ARCHIVADO);

        int anio = LocalDate.now().getYear();
        return CierreProyectoResponse.builder()
                .tesisId(tesisId)
                .estudianteNombre(nombre(pe))
                .codigoSistema(e != null ? e.getCodigoSistema() : null)
                .programaNombre(e != null && e.getPrograma() != null ? e.getPrograma().getNombre() : null)
                .tituloTesis(tesis != null ? tesis.getTitulo() : null)
                .fechaDefensa(p.getFechaDefensa())
                .horaDefensa(p.getHoraDefensa())
                .lugarDefensa(p.getLugarDefensa())
                .modalidad(p.getModalidadDefensa() != null ? p.getModalidadDefensa().name() : null)
                .modalidadLabel(p.getModalidadDefensa() != null ? p.getModalidadDefensa().etiqueta() : null)
                .enlaceDefensa(p.getEnlaceDefensa())
                .defensaRealizada(Boolean.TRUE.equals(p.getDefensaRealizada()))
                .resultado(p.getResultadoDefensa() != null ? p.getResultadoDefensa().name() : null)
                .resultadoLabel(p.getResultadoDefensa() != null ? p.getResultadoDefensa().etiqueta() : null)
                .fechaResultado(p.getFechaResultadoDefensa())
                .observacionDefensa(p.getObservacionDefensa())
                .rubricas(rubricas)
                .estadoDictamen(dic != null ? dic.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                .dictamenNumero(dic != null ? dic.getNumero() : null)
                .dictamenExpediente(dic != null ? dic.getExpediente() : null)
                .dictamenFechaEmision(dic != null ? dic.getFechaEmision() : null)
                .dictamenVigenciaHasta(dic != null ? dic.getVigenciaHasta() : null)
                .dictamenElaborado(dic != null && dic.getDatosDictamen() != null && !dic.getDatosDictamen().isBlank())
                .dictamenFirmadoSubido(firmadoSubido)
                .numeroSugerido(dic != null && dic.getNumero() != null ? dic.getNumero() : null)
                .expedienteSugerido(dic != null && dic.getExpediente() != null
                        ? dic.getExpediente() : String.format("EXP-%04d-", anio))
                .proyectoFinalDelEstudiante(documentoTesisRepository.existePorTesisYTipo(tesisId, T_PROYECTO_FINAL))
                .proyectoFinalArchivado(archivado.isPresent())
                .proyectoFinalNombre(archivado.map(DocumentoTesis::getNombreOriginal).orElse(null))
                .cerrado(Boolean.TRUE.equals(p.getProyectoAprobado()))
                .fechaCierre(p.getFechaAprobacionProyecto())
                .pendiente(pendiente(p, recibidas, rubricas.size(), dic, archivado.isPresent()))
                .build();
    }

    /** Un renglón por revisor designado: recibida o por recibir. */
    private List<RubricaDefensaItem> rubricasDe(UUID tesisId, UUID proyectoId) {
        return revisorRepository.listarPorProyecto(proyectoId).stream()
                .sorted(Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> {
                    var r = rubricaRepository.buscarPorTesisYDocente(tesisId, rv.getDocenteId()).orElse(null);
                    return RubricaDefensaItem.builder()
                            .docenteId(rv.getDocenteId())
                            .docenteNombre(nombreDocente(rv.getDocenteId()))
                            .recibida(r != null)
                            .puntaje(r != null ? r.getPuntaje() : null)
                            .nombreArchivo(r != null ? r.getNombreOriginal() : null)
                            .fechaCarga(r != null && r.getFechaCarga() != null ? r.getFechaCarga().toLocalDate() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Paso 1 · rúbricas y resultado ──────────────────────────────────────

    @Override
    @Transactional
    public void recepcionarRubrica(UUID tesisId, UUID docenteId, Integer puntaje,
                                   byte[] contenido, String nombreOriginal, String contentType) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getDefensaProgramada())) {
            throw new BusinessException("La defensa de este proyecto aún no ha sido programada");
        }
        if (revisorRepository.buscarPorProyectoYDocente(p.getId(), docenteId).isEmpty()) {
            throw new ValidationException("Ese docente no es revisor de este proyecto");
        }
        if (puntaje != null && (puntaje < 0 || puntaje > 100)) {
            throw new ValidationException("La nota debe estar entre 0 y 100");
        }
        validarArchivo(contenido, contentType);

        var existente = rubricaRepository.buscarPorTesisYDocente(tesisId, docenteId);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        RubricaDefensa r = existente.orElseGet(() -> RubricaDefensa.builder()
                .tesisId(tesisId).docenteId(docenteId).build());
        r.setPuntaje(puntaje);
        r.setNombreOriginal(nombreOriginal);
        r.setStorageKey(key);
        r.setContentType(contentType);
        r.setTamanioBytes((long) contenido.length);
        r.setFechaCarga(LocalDateTime.now());
        r.setSubidoPor(securityUtils.getCurrentUserIdAsUUID());
        rubricaRepository.save(r);
    }

    @Override
    @Transactional
    public ArchivoDescargable documentoRubrica(UUID tesisId, UUID docenteId) {
        RubricaDefensa r = rubricaRepository.buscarPorTesisYDocente(tesisId, docenteId)
                .orElseThrow(() -> new BusinessException("Aún no se ha recepcionado la rúbrica de ese revisor"));
        return new ArchivoDescargable(almacenamiento.obtener(r.getStorageKey()), r.getContentType(), r.getNombreOriginal());
    }

    @Override
    @Transactional
    public void registrarResultado(UUID tesisId, RegistrarResultadoDefensaRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getDefensaProgramada())) {
            throw new BusinessException("La defensa de este proyecto aún no ha sido programada");
        }
        if (Boolean.TRUE.equals(p.getProyectoAprobado())) {
            throw new BusinessException("El proyecto ya fue aprobado y archivado");
        }
        ResultadoDefensa resultado = resultadoDe(req != null ? req.getResultado() : null);
        if (resultado != ResultadoDefensa.APROBADO
                && (req.getObservacion() == null || req.getObservacion().isBlank())) {
            throw new ValidationException("Indica las observaciones o el motivo de la desaprobación");
        }
        p.setDefensaRealizada(true);
        p.setResultadoDefensa(resultado);
        p.setFechaResultadoDefensa(req.getFecha() != null ? req.getFecha()
                : (p.getFechaDefensa() != null ? p.getFechaDefensa() : LocalDate.now()));
        p.setObservacionDefensa(req.getObservacion() != null ? req.getObservacion().trim() : null);
        if (!resultado.favorable()) {
            reabrirRevisionTrasDesaprobacion(p);
        }
        proyectoRepository.save(p);
    }

    /**
     * El proyecto desaprobado en la defensa regresa a los revisores en vez de quedar detenido: se
     * limpia la programación (Secretaría podrá reprogramar la defensa recién cuando los revisores
     * vuelvan a dar conformidad) y cada revisor vuelve a {@code DESIGNADO} para reevaluar el
     * proyecto ya corregido. El resultado de esta defensa (DESAPROBADO) y su fecha/observación se
     * conservan como historial hasta que se registre el resultado de la próxima.
     */
    private void reabrirRevisionTrasDesaprobacion(ProyectoTesis p) {
        p.setDefensaProgramada(false);
        p.setFechaDefensa(null);
        p.setHoraDefensa(null);
        p.setLugarDefensa(null);
        p.setModalidadDefensa(null);
        p.setEnlaceDefensa(null);
        p.setRevisoresConformes(false);
        p.setFechaRevisoresConformes(null);
        for (ProyectoRevisor rv : revisorRepository.listarPorProyecto(p.getId())) {
            rv.setEstado(EstadoRevisor.DESIGNADO);
            rv.setComentario(null);
            rv.setPuntajeTotal(null);
            rv.setRespuestaEstudiante(null);
            rv.setFechaRespuesta(null);
            rv.setFechaConformidad(null);
            revisorRepository.save(rv);
            puntajeRepository.eliminarPorRevisor(rv.getId());
        }
    }

    private ResultadoDefensa resultadoDe(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("Indica el resultado de la defensa");
        }
        try {
            return ResultadoDefensa.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Resultado no válido");
        }
    }

    // ── Paso 2 · dictamen de aprobación ────────────────────────────────────

    @Override
    @Transactional
    public void elaborarDictamen(UUID tesisId, ElaborarDictamenAprobacionRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getDefensaRealizada()) || p.getResultadoDefensa() == null) {
            throw new BusinessException("Primero registra el resultado de la defensa");
        }
        if (!p.getResultadoDefensa().favorable()) {
            throw new BusinessException("El proyecto fue desaprobado en la defensa: no corresponde el dictamen de aprobación");
        }
        List<RubricaDefensaItem> rubricas = rubricasDe(tesisId, p.getId());
        if (rubricas.stream().anyMatch(r -> !r.isRecibida())) {
            throw new BusinessException("Faltan rúbricas de la defensa por recepcionar");
        }

        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        Estudiante e = estudiante(tesisId);

        DictamenAprobacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseGet(() -> DictamenAprobacion.builder()
                        .tesisId(tesisId).estado(EstadoDictamen.POR_ELABORAR).build());
        if (dic.getEstado() == EstadoDictamen.FIRMADO) {
            throw new BusinessException("El dictamen ya fue firmado; no puede re-elaborarse");
        }

        int anio = LocalDate.now().getYear();
        if (dic.getNumero() == null || dic.getAnio() == null || dic.getAnio() != anio) {
            int corr = correlativoRepository.siguienteCorrelativo(anio);
            dic.setAnio(anio);
            dic.setCorrelativo(corr);
            dic.setNumero(formatoNumero(corr, anio, e));
            dic.setExpediente(String.format("EXP-%04d-%06d", anio, corr));
        }
        // La numeración de la UPG manda: lo que escriba la Secretaría reemplaza a la propuesta.
        if (req != null && req.getNumero() != null && !req.getNumero().isBlank()) {
            dic.setNumero(req.getNumero().trim());
        }
        if (req != null && req.getExpediente() != null && !req.getExpediente().isBlank()) {
            dic.setExpediente(req.getExpediente().trim());
        }
        LocalDate base = p.getFechaResultadoDefensa() != null ? p.getFechaResultadoDefensa() : LocalDate.now();
        dic.setVigenciaHasta(req != null && req.getVigenciaHasta() != null
                ? req.getVigenciaHasta() : base.plusYears(ANIOS_VIGENCIA));
        dic.setEstado(EstadoDictamen.ELABORADO);

        List<String> revisores = rubricas.stream()
                .map(RubricaDefensaItem::getDocenteNombre)
                .filter(n -> n != null && !n.isBlank())
                .collect(Collectors.toList());
        var datos = resolverPlantilla.resolverDictamenAprobacion(dic, p, tesis, e, revisores, LocalDate.now());
        dic.setDatosDictamen(json(datos));
        dictamenRepository.save(dic);
    }

    @Override
    @Transactional
    public byte[] documentoDictamen(UUID tesisId, String formato) {
        DictamenAprobacion dic = dictamenRepository.buscarPorTesisId(tesisId)
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
        return docx ? renderer.docx(PlantillaDictamen.APROBACION, datos)
                : renderer.pdf(PlantillaDictamen.APROBACION, datos);
    }

    @Override
    @Transactional
    public void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        DictamenAprobacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido elaborado"));
        if (dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            throw new BusinessException("Debes elaborar el dictamen antes de subir el firmado");
        }
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_DICTAMEN_APROBACION, contenido, nombreOriginal, contentType);
        dic.setEstado(EstadoDictamen.FIRMADO);
        dic.setFechaEmision(LocalDate.now());
        dictamenRepository.save(dic);
    }

    // ── Paso 3 · archivo del expediente ────────────────────────────────────

    @Override
    @Transactional
    public void archivarProyectoFinal(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        exigirDictamenFirmado(tesisId);
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_PROYECTO_ARCHIVADO, contenido, nombreOriginal, contentType);
        cerrar(tesisId);
    }

    @Override
    @Transactional
    public void archivarProyectoDelEstudiante(UUID tesisId) {
        exigirDictamenFirmado(tesisId);
        DocumentoTesis original = documentoTesisRepository.buscarPorTesisYTipo(tesisId, T_PROYECTO_FINAL)
                .orElseThrow(() -> new BusinessException("El doctorando no subió el proyecto en versión final"));
        byte[] contenido = almacenamiento.obtener(original.getStorageKey());
        guardarDocumento(tesisId, T_PROYECTO_ARCHIVADO, contenido,
                original.getNombreOriginal(), original.getContentType());
        cerrar(tesisId);
    }

    private void exigirDictamenFirmado(UUID tesisId) {
        DictamenAprobacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen de aprobación aún no ha sido elaborado"));
        if (dic.getEstado() != EstadoDictamen.FIRMADO) {
            throw new BusinessException("Sube primero el dictamen firmado por el Director");
        }
    }

    /** Cierra la Etapa 5: el proyecto queda aprobado y el proceso pasa a la ejecución (Etapa 6). */
    private void cerrar(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        p.setProyectoAprobado(true);
        p.setFechaAprobacionProyecto(LocalDate.now());
        proyectoRepository.save(p);

        tesisRepository.buscarPorId(tesisId).ifPresent(t -> {
            t.setEstado(EstadoTesis.PROYECTO_APROBADO);
            tesisRepository.save(t);
        });
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private void guardarDocumento(UUID tesisId, String tipo, byte[] contenido,
                                  String nombreOriginal, String contentType) {
        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesisId, tipo);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey());
        }
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);
        DocumentoTesis doc = existente.orElseGet(
                () -> DocumentoTesis.builder().tesisId(tesisId).tipo(tipo).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setHashSha256(sha256(contenido));
        doc.setFechaCarga(LocalDateTime.now());
        doc.setSubidoPor(securityUtils.getCurrentUserIdAsUUID());
        documentoTesisRepository.save(doc);
    }

    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
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

    private String nombreDocente(UUID personaId) {
        if (personaId == null) return null;
        return personaRepository.buscarPorId(personaId).map(this::nombre).orElse(null);
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : "";
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
