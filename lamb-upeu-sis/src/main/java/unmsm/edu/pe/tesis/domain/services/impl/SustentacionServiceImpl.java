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
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.JuradoItem;
import unmsm.edu.pe.tesis.application.dto.ProgramarSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarActaSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.SustentacionBandejaItem;
import unmsm.edu.pe.tesis.application.dto.SustentacionTramiteResponse;
import unmsm.edu.pe.tesis.domain.entities.DictamenSustentacion;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;
import unmsm.edu.pe.tesis.domain.enums.ModalidadDefensa;
import unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenSustentacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.JuradoSustentacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService;
import unmsm.edu.pe.tesis.domain.services.SustentacionService;
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
public class SustentacionServiceImpl implements SustentacionService {

    public static final String T_DICTAMEN_SUSTENTACION = "DICTAMEN_SUSTENTACION_FIRMADO";
    public static final String T_ACTA_SUSTENTACION = "ACTA_SUSTENTACION_FIRMADA";

    private static final java.util.Set<String> TIPOS_PERMITIDOS = java.util.Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword");

    @Inject SecurityUtils securityUtils;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject JuradoSustentacionRepository juradoRepository;
    @Inject DictamenSustentacionRepository dictamenRepository;
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
    public List<SustentacionBandejaItem> bandeja(String buscar) {
        List<SustentacionBandejaItem> out = new ArrayList<>();
        for (Object[] r : proyectoRepository.bandejaDefensa(buscar, 0, 100)) {
            UUID tesisId = (UUID) r[0];
            ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId).orElse(null);
            if (p == null || !Boolean.TRUE.equals(p.getSustentacionSolicitada())) {
                continue;   // el trámite empieza cuando el estudiante solicita la sustentación
            }
            int numJurado = (int) juradoRepository.contarPorProyecto(p.getId());
            DictamenSustentacion dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);

            out.add(SustentacionBandejaItem.builder()
                    .tesisId(tesisId)
                    .estudianteNombre((asStr(r[2]) + " " + asStr(r[3]) + ", " + asStr(r[4])).trim())
                    .codigoSistema(asStr(r[5]))
                    .programaNombre(asStr(r[6]))
                    .tituloTesis(asStr(r[7]))
                    .fechaSolicitud(p.getFechaSolicitudSustentacion())
                    .expedienteRecibido(Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido()))
                    .numJurado(numJurado)
                    .estadoDictamen(dic != null ? dic.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                    .sustentacionProgramada(Boolean.TRUE.equals(p.getSustentacionProgramada()))
                    .fechaSustentacion(p.getFechaSustentacion())
                    .actaSubida(Boolean.TRUE.equals(p.getActaSustentacionSubida()))
                    .concluida(Boolean.TRUE.equals(p.getTesisConcluida()))
                    .pendiente(pendiente(p, numJurado, dic))
                    .build());
        }
        return out;
    }

    private String pendiente(ProyectoTesis p, int numJurado, DictamenSustentacion dic) {
        if (Boolean.TRUE.equals(p.getTesisConcluida())) {
            return "Concluida";
        }
        if (!Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido())) {
            return "Recepcionar el expediente y comunicar al Coordinador";
        }
        if (numJurado < 3) {
            return "Esperando que el Coordinador designe al Jurado de Sustentación";
        }
        if (dic == null || dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            return "Elaborar el dictamen de designación del Jurado";
        }
        if (dic.getEstado() != EstadoDictamen.FIRMADO) {
            return "Subir el dictamen de designación firmado";
        }
        if (!Boolean.TRUE.equals(p.getSustentacionProgramada())) {
            return "Coordinar modalidad, lugar y fecha de la sustentación";
        }
        if (!Boolean.TRUE.equals(p.getActaSustentacionSubida())) {
            return "Registrar el resultado y subir el Acta de sustentación";
        }
        return "Concluida";
    }

    @Override
    @Transactional
    public SustentacionTramiteResponse detalle(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElse(null);
        Estudiante e = estudiante(tesisId);
        Persona pe = e != null ? e.getPersona() : null;

        List<JuradoItem> jurado = juradoRepository.listarPorProyecto(p.getId()).stream()
                .sorted(Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> JuradoItem.builder()
                        .docenteId(rv.getDocenteId())
                        .docenteNombre(nombreDocente(rv.getDocenteId()))
                        .rol(Boolean.TRUE.equals(rv.getPresidente()) ? "PRESIDENTE" : "MIEMBRO")
                        .orden(rv.getOrden())
                        .build())
                .collect(Collectors.toList());

        DictamenSustentacion dic = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
        int anio = LocalDate.now().getYear();

        return SustentacionTramiteResponse.builder()
                .tesisId(tesisId)
                .estudianteNombre(nombre(pe))
                .codigoSistema(e != null ? e.getCodigoSistema() : null)
                .programaNombre(e != null && e.getPrograma() != null ? e.getPrograma().getNombre() : null)
                .tituloTesis(tesis != null ? tesis.getTitulo() : null)
                .fechaSolicitud(p.getFechaSolicitudSustentacion())
                .expedienteRecibido(Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido()))
                .fechaRecepcion(p.getFechaRecepcionSustentacion())
                .jurado(jurado)
                .estadoDictamen(dic != null ? dic.getEstado().name() : EstadoDictamen.POR_ELABORAR.name())
                .dictamenNumero(dic != null ? dic.getNumero() : null)
                .dictamenExpediente(dic != null ? dic.getExpediente() : null)
                .dictamenFechaEmision(dic != null ? dic.getFechaEmision() : null)
                .dictamenElaborado(dic != null && dic.getDatosDictamen() != null && !dic.getDatosDictamen().isBlank())
                .dictamenFirmadoSubido(documentoTesisRepository.existePorTesisYTipo(tesisId, T_DICTAMEN_SUSTENTACION))
                .numeroSugerido(dic != null ? dic.getNumero() : null)
                .expedienteSugerido(dic != null && dic.getExpediente() != null ? dic.getExpediente() : String.format("EXP-%04d-", anio))
                .sustentacionProgramada(Boolean.TRUE.equals(p.getSustentacionProgramada()))
                .fechaSustentacion(p.getFechaSustentacion())
                .horaSustentacion(p.getHoraSustentacion())
                .lugarSustentacion(p.getLugarSustentacion())
                .modalidad(p.getModalidadSustentacion() != null ? p.getModalidadSustentacion().name() : null)
                .modalidadLabel(p.getModalidadSustentacion() != null ? p.getModalidadSustentacion().etiqueta() : null)
                .enlaceSustentacion(p.getEnlaceSustentacion())
                .actaSubida(Boolean.TRUE.equals(p.getActaSustentacionSubida()))
                .fechaActa(p.getFechaActaSustentacion())
                .resultado(p.getResultadoSustentacion() != null ? p.getResultadoSustentacion().name() : null)
                .resultadoLabel(p.getResultadoSustentacion() != null ? p.getResultadoSustentacion().etiqueta() : null)
                .observacionActa(p.getObservacionActa())
                .concluida(Boolean.TRUE.equals(p.getTesisConcluida()))
                .fechaConclusion(p.getFechaConclusionTesis())
                .pendiente(pendiente(p, jurado.size(), dic))
                .build();
    }

    @Override
    @Transactional
    public void recepcionar(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getSustentacionSolicitada())) {
            throw new BusinessException("El estudiante aún no ha solicitado su Jurado de Sustentación");
        }
        if (Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido())) {
            throw new BusinessException("Ese expediente ya fue recepcionado");
        }
        p.setExpedienteSustentacionRecibido(true);
        p.setFechaRecepcionSustentacion(LocalDate.now());
        proyectoRepository.save(p);
    }

    @Override
    @Transactional
    public void elaborarDictamen(UUID tesisId, ElaborarDictamenSustentacionRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getExpedienteSustentacionRecibido())) {
            throw new BusinessException("Primero recepciona el expediente");
        }
        List<unmsm.edu.pe.tesis.domain.entities.JuradoSustentacion> jurado = juradoRepository.listarPorProyecto(p.getId());
        if (jurado.size() < 3) {
            throw new BusinessException("El Coordinador aún no designó a los 3 miembros del Jurado de Sustentación");
        }
        Tesis tesis = tesisRepository.buscarPorId(tesisId).orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        Estudiante e = estudiante(tesisId);

        DictamenSustentacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseGet(() -> DictamenSustentacion.builder().tesisId(tesisId).estado(EstadoDictamen.POR_ELABORAR).build());
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
        if (req != null && req.getNumero() != null && !req.getNumero().isBlank()) dic.setNumero(req.getNumero().trim());
        if (req != null && req.getExpediente() != null && !req.getExpediente().isBlank()) dic.setExpediente(req.getExpediente().trim());
        dic.setEstado(EstadoDictamen.ELABORADO);

        List<String> nombres = jurado.stream().sorted(Comparator.comparing(rv -> rv.getOrden() != null ? rv.getOrden() : 0))
                .map(rv -> nombreDocente(rv.getDocenteId())).filter(n -> n != null && !n.isBlank()).collect(Collectors.toList());
        var datos = resolverPlantilla.resolverDictamenSustentacion(dic, tesis, e, nombres, LocalDate.now());
        dic.setDatosDictamen(json(datos));
        dictamenRepository.save(dic);
    }

    @Override
    @Transactional
    public byte[] documentoDictamen(UUID tesisId, String formato) {
        DictamenSustentacion dic = dictamenRepository.buscarPorTesisId(tesisId)
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
        return docx ? renderer.docx(PlantillaDictamen.SUSTENTACION, datos)
                : renderer.pdf(PlantillaDictamen.SUSTENTACION, datos);
    }

    @Override
    @Transactional
    public void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType) {
        DictamenSustentacion dic = dictamenRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido elaborado"));
        if (dic.getEstado() == EstadoDictamen.POR_ELABORAR) {
            throw new BusinessException("Debes elaborar el dictamen antes de subir el firmado");
        }
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_DICTAMEN_SUSTENTACION, contenido, nombreOriginal, contentType);
        dic.setEstado(EstadoDictamen.FIRMADO);
        dic.setFechaEmision(LocalDate.now());
        dictamenRepository.save(dic);
    }

    @Override
    @Transactional
    public void programar(UUID tesisId, ProgramarSustentacionRequest req) {
        ProyectoTesis p = proyecto(tesisId);
        if (!documentoTesisRepository.existePorTesisYTipo(tesisId, T_DICTAMEN_SUSTENTACION)) {
            throw new BusinessException("Falta el dictamen de designación del Jurado firmado");
        }
        if (Boolean.TRUE.equals(p.getSustentacionProgramada())) {
            throw new BusinessException("La sustentación de este proyecto ya fue programada");
        }
        if (req == null || req.getFecha() == null) {
            throw new ValidationException("Indica la fecha de la sustentación");
        }
        ModalidadDefensa modalidad = modalidadDe(req.getModalidad());
        String lugar = req.getLugar() != null ? req.getLugar().trim() : "";
        String enlace = req.getEnlace() != null ? req.getEnlace().trim() : "";
        if (modalidad.requiereLugar() && lugar.isEmpty()) {
            throw new ValidationException("Indica el aula o ambiente donde se realizará la sustentación");
        }
        if (modalidad.requiereEnlace() && enlace.isEmpty()) {
            throw new ValidationException("Indica el enlace de la sesión para la sustentación " + modalidad.etiqueta().toLowerCase());
        }

        p.setSustentacionProgramada(true);
        p.setFechaSustentacion(req.getFecha());
        p.setHoraSustentacion(req.getHora());
        p.setLugarSustentacion(lugar.isEmpty() ? null : lugar);
        p.setModalidadSustentacion(modalidad);
        p.setEnlaceSustentacion(enlace.isEmpty() ? null : enlace);
        proyectoRepository.save(p);
    }

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

    @Override
    @Transactional
    public void registrarActa(UUID tesisId, RegistrarActaSustentacionRequest req, byte[] contenido,
                              String nombreOriginal, String contentType) {
        ProyectoTesis p = proyecto(tesisId);
        if (!Boolean.TRUE.equals(p.getSustentacionProgramada())) {
            throw new BusinessException("La sustentación aún no ha sido programada");
        }
        if (Boolean.TRUE.equals(p.getTesisConcluida())) {
            throw new BusinessException("La tesis ya fue concluida");
        }
        ResultadoDefensa resultado = resultadoDe(req != null ? req.getResultado() : null);
        if (resultado != ResultadoDefensa.APROBADO
                && (req.getObservacion() == null || req.getObservacion().isBlank())) {
            throw new ValidationException("Indica las observaciones o el motivo de la desaprobación");
        }
        validarArchivo(contenido, contentType);
        guardarDocumento(tesisId, T_ACTA_SUSTENTACION, contenido, nombreOriginal, contentType);

        p.setActaSustentacionSubida(true);
        p.setFechaActaSustentacion(LocalDate.now());
        p.setResultadoSustentacion(resultado);
        p.setObservacionActa(req.getObservacion() != null ? req.getObservacion().trim() : null);

        if (resultado.favorable()) {
            // El acto concluye el proceso de titulación cuando el resultado es favorable.
            p.setTesisConcluida(true);
            p.setFechaConclusionTesis(LocalDate.now());
            proyectoRepository.save(p);

            tesisRepository.buscarPorId(tesisId).ifPresent(t -> {
                t.setEstado(EstadoTesis.SUSTENTADO);
                tesisRepository.save(t);
            });
        } else {
            // Desaprobada: no concluye la tesis. Se limpia la programación para que Secretaría
            // pueda coordinar un nuevo acto; el jurado y el dictamen de designación siguen vigentes
            // (no cambian), y esta acta queda como historial hasta que se registre la siguiente.
            p.setSustentacionProgramada(false);
            p.setFechaSustentacion(null);
            p.setHoraSustentacion(null);
            p.setLugarSustentacion(null);
            p.setModalidadSustentacion(null);
            p.setEnlaceSustentacion(null);
            proyectoRepository.save(p);
        }
    }

    private ResultadoDefensa resultadoDe(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ValidationException("Indica el resultado de la sustentación");
        }
        try {
            return ResultadoDefensa.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Resultado no válido");
        }
    }

    // ── utilidades ──
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
