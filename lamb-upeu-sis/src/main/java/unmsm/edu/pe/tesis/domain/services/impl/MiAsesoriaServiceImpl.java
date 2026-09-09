package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.shared.utils.EstadoDerivado;
import unmsm.edu.pe.tesis.application.dto.DatosDocumentoAsesoria;
import unmsm.edu.pe.tesis.application.dto.MiAsesoriaResponse;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;
import unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.MiAsesoriaService;
import unmsm.edu.pe.tesis.domain.services.SugerenciaAsesorService;
import unmsm.edu.pe.tesis.infrastructure.export.AsesoriaPdfExporter;
import unmsm.edu.pe.personas.domain.entities.Docente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class MiAsesoriaServiceImpl implements MiAsesoriaService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject SolicitudAsesoriaRepository solicitudRepository;
    @Inject SugerenciaAsesorService sugerenciaService;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository documentoTesisRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository dictamenRepository;
    @Inject unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos almacenamiento;
    @Inject unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService resolverPlantilla;
    @Inject unmsm.edu.pe.tesis.infrastructure.export.DocumentoAsesoriaRenderer renderer;
    @Inject com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoRepository;
    @Inject unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository tutoriaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository asesoriaRepository;
    @Inject unmsm.edu.pe.personas.domain.repositories.DocenteRepository docenteRepository;

    @Override
    @Transactional
    public MiAsesoriaResponse bandeja() {
        Estudiante estudiante = estudianteActual();
        UUID estudianteId = estudiante.getPersonaId();

        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(estudianteId).orElse(null);
        UUID tesisId = tesis != null ? tesis.getId() : null;
        boolean tieneAsesor = tesisId != null && tesisRepository.tieneAsesor(tesisId);
        String estadoTesis = tesis != null && tesis.getEstado() != null ? tesis.getEstado().name() : null;

        // El proceso lo marca la solicitud del ASESOR; la de co-asesoría se informa aparte.
        SolicitudAsesoria sol = solicitudPrincipal(estudianteId).orElse(null);
        SolicitudAsesoria solCo = solicitudRepository
                .ultimaDeEstudiantePorTipo(estudianteId, TipoAsesoria.COASESOR).orElse(null);

        MiAsesoriaResponse.MiAsesoriaResponseBuilder b = MiAsesoriaResponse.builder()
                .conTema(tesis != null)
                .tesisId(tesisId)
                .temaTitulo(tesis != null ? tesis.getTitulo() : null)
                .lineaNombre(tesis != null && tesis.getLineaInvestigacion() != null ? tesis.getLineaInvestigacion().getNombre() : null)
                .nivel(tesis != null && tesis.getNivel() != null ? tesis.getNivel().name() : null)
                .estadoDerivado(EstadoDerivado.resolver(tesisId, estadoTesis, tieneAsesor))
                .sugeridos(sugerenciaService.listar(estudianteId));

        // Designación vigente: un asesor y, como máximo, un co-asesor.
        String asesorNombre = nombreDocenteDeAsesoria(tesisId, "ASESOR");
        String coasesorNombre = nombreDocenteDeAsesoria(tesisId, "COASESOR");
        boolean coasesorPendiente = solCo != null && solCo.getEstado() == EstadoSolicitud.PENDIENTE;
        b.asesorNombre(asesorNombre)
                .coasesorNombre(coasesorNombre)
                .asesorDocenteId(docenteDeAsesoria(tesisId, "ASESOR"))
                .coasesorDocenteId(docenteDeAsesoria(tesisId, "COASESOR"))
                .puedeSolicitarCoasesor(asesorNombre != null && coasesorNombre == null && !coasesorPendiente);
        if (solCo != null) {
            b.coasesorSolicitudId(solCo.getId())
                    .coasesorSolicitudEstado(solCo.getEstado().name())
                    .coasesorSolicitadoNombre(solCo.getDocente() != null ? nombre(solCo.getDocente().getPersona()) : null)
                    .coasesorMotivoRespuesta(solCo.getMotivoRespuesta());
        }

        // Tutor asignado (tutoría vigente): el estudiante debe ver quién es su tutor.
        tutoriaRepository.findVigenteByEstudiante(estudianteId).ifPresent(t -> {
            Docente td = t.getDocente();
            if (td != null) {
                b.tutorId(td.getPersonaId())
                        .tutorNombre(nombre(td.getPersona()))
                        .tutorGrado(personaGradoRepository.gradoPrincipal(td.getPersonaId()));
            }
        });

        b
                .solicitudEstado(sol != null ? sol.getEstado().name() : "SIN_SOLICITUD")
                // Los documentos firmados son un paquete post-aceptación: la solicitud y la carta
                // solo se descargan/firman/suben cuando el ASESOR ha aceptado la asesoría (una
                // solicitud de co-asesoría en curso no los bloquea: el co-asesor es opcional).
                .solicitudPdfDisponible(sol != null && sol.getEstado() == EstadoSolicitud.ACEPTADA)
                .cartaPdfDisponible(sol != null && sol.getEstado() == EstadoSolicitud.ACEPTADA);

        if (sol != null) {
            b.solicitudId(sol.getId());
        }
        if (sol != null && sol.getDocente() != null) {
            Docente d = sol.getDocente();
            b.docenteSolicitadoId(d.getPersonaId())
                    .docenteSolicitadoNombre(nombre(d.getPersona()))
                    .fechaSolicitud(sol.getFechaSolicitud())
                    .fechaRespuesta(sol.getFechaRespuesta())
                    .motivoRespuesta(sol.getMotivoRespuesta());
        }

        // Estado de los firmados subidos + dictamen (sobre la tesis activa).
        if (tesisId != null) {
            boolean ambosSubidos = documentoTesisRepository.existePorTesisYTipo(tesisId, "SOLICITUD_ASESORIA_FIRMADA")
                    && documentoTesisRepository.existePorTesisYTipo(tesisId, "CARTA_ACEPTACION_FIRMADA");
            b.solicitudFirmadaSubida(documentoTesisRepository.existePorTesisYTipo(tesisId, "SOLICITUD_ASESORIA_FIRMADA"));
            b.cartaFirmadaSubida(documentoTesisRepository.existePorTesisYTipo(tesisId, "CARTA_ACEPTACION_FIRMADA"));
            b.dictamenEmitido(documentoTesisRepository.existePorTesisYTipo(tesisId, "DICTAMEN_DESIGNACION_FIRMADO"));
            var dic = dictamenRepository.buscarPorTesisId(tesisId);
            dic.ifPresent(d -> b
                    .dictamenEstado(d.getEstado() != null ? d.getEstado().name() : null)
                    .dictamenNumero(d.getNumero())
                    .dictamenFechaEmision(d.getFechaEmision())
                    .dictamenMotivoObservacion(d.getMotivoObservacion()));
            // Falta enviar cuando no hay dictamen todavía, o cuando Secretaría lo observó y el
            // estudiante ya volvió a subir lo corregido: en ambos casos espera su confirmación.
            b.listoParaEnviar(ambosSubidos
                    && (dic.isEmpty() || dic.get().getEstado() == unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.OBSERVADO));
        }
        return b.build();
    }

    @Override
    @Transactional
    public byte[] documentoBytes(String tipo, String formato) {
        Estudiante estudiante = estudianteActual(); // valida propiedad: solo el estudiante autenticado
        SolicitudAsesoria sol = solicitudPrincipal(estudiante.getPersonaId())
                .orElseThrow(() -> new BusinessException("Aún no has solicitado asesoría"));

        String t = tipo != null ? tipo.trim().toUpperCase() : "";
        boolean docx = "docx".equalsIgnoreCase(formato != null ? formato.trim() : "pdf");

        String plantilla;
        java.util.Map<String, String> datos;
        if (TIPO_SOLICITUD.equals(t)) {
            if (sol.getEstado() != EstadoSolicitud.ACEPTADA) {
                throw new BusinessException("La solicitud estará disponible para firma cuando el asesor acepte la asesoría");
            }
            plantilla = unmsm.edu.pe.tesis.infrastructure.export.PlantillaAsesoria.SOLICITUD;
            LocalDate f = sol.getFechaSolicitud() != null ? sol.getFechaSolicitud().toLocalDate() : LocalDate.now();
            datos = leerSnapshot(sol.getDatosSolicitud(), () -> resolverPlantilla.resolverSolicitud(sol, f));
        } else if (TIPO_CARTA.equals(t)) {
            if (sol.getEstado() != EstadoSolicitud.ACEPTADA) {
                throw new BusinessException("La carta de aceptación estará disponible cuando el asesor acepte");
            }
            plantilla = unmsm.edu.pe.tesis.infrastructure.export.PlantillaAsesoria.CARTA;
            LocalDate f = sol.getFechaRespuesta() != null ? sol.getFechaRespuesta().toLocalDate() : LocalDate.now();
            datos = leerSnapshot(sol.getDatosCarta(), () -> resolverPlantilla.resolverCarta(sol, f));
        } else {
            throw new ValidationException("Tipo de documento inválido: " + tipo);
        }

        return docx ? renderer.docx(plantilla, datos) : renderer.pdf(plantilla, datos);
    }

    /** Lee el snapshot JSON; si no existe (solicitud previa), lo resuelve al vuelo como respaldo. */
    private java.util.Map<String, String> leerSnapshot(String json, java.util.function.Supplier<java.util.Map<String, String>> fallback) {
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, String>>() {});
            } catch (Exception ignored) {
                // snapshot corrupto → se recalcula
            }
        }
        return fallback.get();
    }

    @Override
    @Transactional
    public void subirFirmado(String tipo, byte[] contenido, String nombreOriginal, String contentType) {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(est.getPersonaId())
                .orElseThrow(() -> new BusinessException("No tienes una tesis activa"));
        // El paquete de firmados (solicitud y carta) solo se sube tras la aceptación del asesor.
        SolicitudAsesoria sol = solicitudPrincipal(est.getPersonaId())
                .orElseThrow(() -> new BusinessException("Aún no has solicitado asesoría"));
        if (sol.getEstado() != EstadoSolicitud.ACEPTADA) {
            throw new BusinessException("Podrás subir los documentos firmados cuando el asesor acepte la asesoría");
        }
        // Ya enviados a Secretaría (cualquier estado salvo OBSERVADO): no se pueden reemplazar
        // por debajo mientras los están revisando o ya se resolvió el trámite.
        var dicVigente = dictamenRepository.buscarPorTesisId(tesis.getId());
        if (dicVigente.isPresent() && dicVigente.get().getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.OBSERVADO) {
            throw new BusinessException("Ya enviaste tus documentos a Secretaría; espera su respuesta");
        }
        String t = tipoFirmado(tipo);
        validarArchivo(contenido, contentType);
        String hash = sha256(contenido);
        String key = almacenamiento.guardar(contenido, nombreOriginal, contentType);

        var existente = documentoTesisRepository.buscarPorTesisYTipo(tesis.getId(), t);
        if (existente.isPresent() && existente.get().getStorageKey() != null) {
            almacenamiento.eliminar(existente.get().getStorageKey()); // reemplazo: borra el anterior
        }
        unmsm.edu.pe.tesis.domain.entities.DocumentoTesis doc = existente.orElseGet(
                () -> unmsm.edu.pe.tesis.domain.entities.DocumentoTesis.builder().tesisId(tesis.getId()).tipo(t).build());
        doc.setNombreOriginal(nombreOriginal);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanioBytes((long) contenido.length);
        doc.setHashSha256(hash);
        doc.setFechaCarga(java.time.LocalDateTime.now());
        doc.setSubidoPor(est.getPersonaId());
        documentoTesisRepository.save(doc);
        // Subir ambos firmados los deja listos, pero no los envía: el estudiante confirma el
        // envío aparte (ver enviarDocumentosFirmados), para no notificar a Secretaría en silencio.
    }

    @Override
    @Transactional
    public void enviarDocumentosFirmados() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(est.getPersonaId())
                .orElseThrow(() -> new BusinessException("No tienes una tesis activa"));
        if (!documentoTesisRepository.tieneFirmadosCompletos(tesis.getId())) {
            throw new BusinessException("Sube la solicitud y la carta de aceptación firmadas antes de enviarlas");
        }
        var dic = dictamenRepository.buscarPorTesisId(tesis.getId());
        if (dic.isPresent() && dic.get().getEstado() != unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.OBSERVADO) {
            throw new BusinessException("Ya enviaste tus documentos a la Secretaría");
        }
        if (dic.isEmpty()) {
            dictamenRepository.save(unmsm.edu.pe.tesis.domain.entities.DictamenDesignacion.builder()
                    .tesisId(tesis.getId())
                    .estado(unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.POR_ELABORAR)
                    .build());
        } else {
            var d = dic.get();
            d.setEstado(unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.POR_ELABORAR);
            d.setMotivoObservacion(null);
            dictamenRepository.save(d);
        }
    }

    @Override
    @Transactional
    public unmsm.edu.pe.tesis.application.dto.ArchivoDescargable descargarDictamenEmitido() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(est.getPersonaId())
                .orElseThrow(() -> new BusinessException("No tienes una tesis activa"));
        var doc = documentoTesisRepository.buscarPorTesisYTipo(tesis.getId(), "DICTAMEN_DESIGNACION_FIRMADO")
                .orElseThrow(() -> new BusinessException("El dictamen aún no ha sido emitido"));
        byte[] bytes = almacenamiento.obtener(doc.getStorageKey());
        return new unmsm.edu.pe.tesis.application.dto.ArchivoDescargable(bytes, doc.getContentType(), doc.getNombreOriginal());
    }

    private String tipoFirmado(String tipo) {
        String t = tipo != null ? tipo.trim().toLowerCase() : "";
        return switch (t) {
            case "solicitud", "solicitud_asesoria" -> "SOLICITUD_ASESORIA_FIRMADA";
            case "carta", "carta_aceptacion" -> "CARTA_ACEPTACION_FIRMADA";
            default -> throw new ValidationException("Tipo de documento firmado inválido: " + tipo);
        };
    }

    private static final java.util.Set<String> TIPOS_PERMITIDOS = java.util.Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword");

    private void validarArchivo(byte[] contenido, String contentType) {
        if (contenido == null || contenido.length == 0) {
            throw new ValidationException("El archivo está vacío");
        }
        if (contenido.length > 10 * 1024 * 1024) {
            throw new ValidationException("El archivo supera el tamaño máximo (10 MB)");
        }
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new ValidationException("Formato no permitido; sube un PDF o Word (.docx)");
        }
    }

    private String sha256(byte[] contenido) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(contenido);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ── helpers ──
    /**
     * Solicitud que representa la asesoría del estudiante: la del <b>asesor</b>. El co-asesor es
     * opcional y su solicitud llega después, así que tomar "la última" haría que una co-asesoría
     * pendiente escondiera los documentos de un asesor que ya aceptó. Si aún no hay ninguna
     * solicitud de asesor (datos antiguos), se cae a la última registrada.
     */
    private java.util.Optional<SolicitudAsesoria> solicitudPrincipal(UUID estudianteId) {
        return solicitudRepository.ultimaDeEstudiantePorTipo(estudianteId, TipoAsesoria.ASESOR)
                .or(() -> solicitudRepository.ultimaDeEstudiante(estudianteId));
    }

    private DatosDocumentoAsesoria datos(Estudiante e, SolicitudAsesoria sol, LocalDate fecha) {
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(e.getPersonaId()).orElse(null);
        Docente asesor = sol.getDocente();
        return DatosDocumentoAsesoria.builder()
                .estudianteNombre(nombre(e.getPersona()))
                .estudianteCodigo(e.getCodigoSistema() != null ? e.getCodigoSistema() : e.getCodMatricula())
                .programaNombre(e.getPrograma() != null ? e.getPrograma().getNombre() : null)
                .temaTitulo(tesis != null ? tesis.getTitulo() : null)
                .lineaNombre(tesis != null && tesis.getLineaInvestigacion() != null ? tesis.getLineaInvestigacion().getNombre() : null)
                .nivel(tesis != null && tesis.getNivel() != null ? tesis.getNivel().name() : null)
                .asesorNombre(asesor != null ? nombre(asesor.getPersona()) : null)
                .asesorGrado(asesor != null ? personaGradoRepository.gradoPrincipal(asesor.getPersonaId()) : null)
                .fecha(fecha)
                .build();
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

    /** Id del docente de la asesoría vigente del tipo pedido (ASESOR | COASESOR), o null. */
    private UUID docenteDeAsesoria(UUID tesisId, String tipo) {
        if (tesisId == null) {
            return null;
        }
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, tipo)
                .map(a -> a.getDocenteId()).orElse(null);
    }

    /** Nombre del docente de la asesoría vigente del tipo pedido (ASESOR | COASESOR), o null. */
    private String nombreDocenteDeAsesoria(UUID tesisId, String tipo) {
        if (tesisId == null) {
            return null;
        }
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, tipo)
                .map(a -> docenteRepository.findByPersonaId(a.getDocenteId()).orElse(null))
                .map(d -> nombre(d.getPersona()))
                .orElse(null);
    }

    private String nombre(Persona p) {
        if (p == null) {
            return null;
        }
        String ap = ((p.getApellidoPaterno() != null ? p.getApellidoPaterno() : "") + " "
                + (p.getApellidoMaterno() != null ? p.getApellidoMaterno() : "")).trim();
        return (ap + ", " + (p.getNombres() != null ? p.getNombres() : "")).trim();
    }
}
