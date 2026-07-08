package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.DocumentoDescarga;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoData;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoResponse;
import unmsm.edu.pe.personas.domain.entities.*;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.repositories.*;
import unmsm.edu.pe.personas.domain.services.GuardarPerfilCompletoService;
import unmsm.edu.pe.personas.domain.services.PersonaService;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class GuardarPerfilCompletoServiceImpl implements GuardarPerfilCompletoService {

    private static final Logger LOG = Logger.getLogger(GuardarPerfilCompletoServiceImpl.class);
    private static final List<TipoDocumento> REQUERIDOS = List.of(TipoDocumento.DNI, TipoDocumento.PARTIDA_NACIMIENTO);

    @Inject PersonaRepository personaRepository;
    @Inject CargoRepository cargoRepository;
    @Inject CentroLaboralRepository centroLaboralRepository;
    @Inject PersonaCargoRepository personaCargoRepository;
    @Inject PersonaCentroLaboralRepository personaCentroLaboralRepository;
    @Inject DocumentoPersonaRepository documentoRepository;
    @Inject AlmacenamientoArchivos almacenamiento;
    @Inject PersonaService personaService;

    @ConfigProperty(name = "storage.max-size-bytes", defaultValue = "10485760")
    long maxSizeBytes;

    @ConfigProperty(name = "storage.allowed-content-types", defaultValue = "application/pdf,image/jpeg,image/png")
    String allowedContentTypes;

    @Override
    public void aplicar(Persona persona, PerfilCompletoData data,
                        Map<TipoDocumento, ArchivoSubido> archivos, boolean validarRequeridos) {
        UUID personaId = persona.getId();
        Map<TipoDocumento, ArchivoSubido> docs = archivos != null ? archivos : Map.of();

        // 1) Validación de documentos obligatorios (fail fast, antes de subir nada)
        if (validarRequeridos) {
            for (TipoDocumento req : REQUERIDOS) {
                boolean presente = docs.containsKey(req)
                        || documentoRepository.existsByPersonaIdAndTipo(personaId, req);
                if (!presente) {
                    throw new ValidationException("Falta el documento obligatorio: " + req.name());
                }
            }
        }

        // 2) Regla "un solo actual"
        validarUnSoloActual(data != null ? cargosActuales(data) : 0, "cargo");
        validarUnSoloActual(data != null ? centrosActuales(data) : 0, "centro laboral");

        // 3) Reemplazo de historiales (replace-all)
        if (data != null && data.getCargos() != null) {
            personaCargoRepository.deleteByPersonaId(personaId);
            List<PersonaCargo> nuevos = data.getCargos().stream().map(h -> {
                Cargo cargo = cargoRepository.buscarPorId(h.getCargoId())
                        .orElseThrow(() -> new BusinessException("Cargo no encontrado: " + h.getCargoId()));
                return PersonaCargo.builder()
                        .persona(persona).cargo(cargo)
                        .fechaInicio(h.getFechaInicio()).fechaFin(h.getFechaFin())
                        .actual(Boolean.TRUE.equals(h.getActual()))
                        .build();
            }).collect(Collectors.toList());
            if (!nuevos.isEmpty()) personaCargoRepository.saveAll(nuevos);
        }
        if (data != null && data.getCentrosLaborales() != null) {
            personaCentroLaboralRepository.deleteByPersonaId(personaId);
            List<PersonaCentroLaboral> nuevos = data.getCentrosLaborales().stream().map(h -> {
                CentroLaboral centro = centroLaboralRepository.buscarPorId(h.getCentroLaboralId())
                        .orElseThrow(() -> new BusinessException("Centro laboral no encontrado: " + h.getCentroLaboralId()));
                return PersonaCentroLaboral.builder()
                        .persona(persona).centroLaboral(centro)
                        .fechaInicio(h.getFechaInicio()).fechaFin(h.getFechaFin())
                        .actual(Boolean.TRUE.equals(h.getActual()))
                        .build();
            }).collect(Collectors.toList());
            if (!nuevos.isEmpty()) personaCentroLaboralRepository.saveAll(nuevos);
        }

        // 4) Documentos: guardar archivo (storage) -> fila (BD). Cleanup en caso de error.
        List<String> nuevasKeys = new ArrayList<>();
        List<String> viejasKeys = new ArrayList<>();
        try {
            for (Map.Entry<TipoDocumento, ArchivoSubido> e : docs.entrySet()) {
                TipoDocumento tipo = e.getKey();
                ArchivoSubido archivo = e.getValue();
                validarArchivo(tipo, archivo);

                String hash = sha256(archivo.contenido());
                String key = almacenamiento.guardar(archivo.contenido(), archivo.nombreOriginal(), archivo.contentType());
                nuevasKeys.add(key);

                Optional<DocumentoPersona> existente = documentoRepository.findByPersonaIdAndTipo(personaId, tipo);
                DocumentoPersona doc = existente.orElseGet(() -> DocumentoPersona.builder()
                        .persona(persona).tipoDocumento(tipo).build());
                if (existente.isPresent() && existente.get().getStorageKey() != null) {
                    viejasKeys.add(existente.get().getStorageKey());
                }
                doc.setNombreOriginal(archivo.nombreOriginal());
                doc.setStorageKey(key);
                doc.setContentType(archivo.contentType());
                doc.setTamanioBytes(archivo.tamanio());
                doc.setHashSha256(hash);
                doc.setFechaCarga(LocalDateTime.now());
                documentoRepository.save(doc);
            }
        } catch (RuntimeException ex) {
            // limpiar archivos recién subidos; el rollback de la tx revierte la BD
            nuevasKeys.forEach(this::eliminarSilencioso);
            throw ex;
        }

        // Éxito: borrar archivos viejos reemplazados (best-effort)
        viejasKeys.forEach(this::eliminarSilencioso);
    }

    @Override
    @Transactional
    public PerfilCompletoResponse guardar(UUID personaId, PerfilCompletoData data,
                                          Map<TipoDocumento, ArchivoSubido> archivos, boolean validarRequeridos) {
        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada: " + personaId));
        aplicar(persona, data, archivos, validarRequeridos);
        return obtenerPerfilCompleto(personaId);
    }

    @Override
    @Transactional
    public PerfilCompletoResponse obtenerPerfilCompleto(UUID personaId) {
        if (personaRepository.buscarPorId(personaId).isEmpty()) {
            throw new NotFoundException("Persona no encontrada: " + personaId);
        }
        List<PerfilCompletoResponse.CargoHistorialItem> cargos = personaCargoRepository.findByPersonaId(personaId).stream()
                .map(pc -> PerfilCompletoResponse.CargoHistorialItem.builder()
                        .id(pc.getId())
                        .cargoId(pc.getCargo() != null ? pc.getCargo().getId() : null)
                        .cargoNombre(pc.getCargo() != null ? pc.getCargo().getNombre() : null)
                        .fechaInicio(pc.getFechaInicio()).fechaFin(pc.getFechaFin())
                        .actual(pc.getActual())
                        .build())
                .collect(Collectors.toList());

        List<PerfilCompletoResponse.CentroHistorialItem> centros = personaCentroLaboralRepository.findByPersonaId(personaId).stream()
                .map(pc -> PerfilCompletoResponse.CentroHistorialItem.builder()
                        .id(pc.getId())
                        .centroLaboralId(pc.getCentroLaboral() != null ? pc.getCentroLaboral().getId() : null)
                        .centroNombre(pc.getCentroLaboral() != null ? pc.getCentroLaboral().getNombre() : null)
                        .fechaInicio(pc.getFechaInicio()).fechaFin(pc.getFechaFin())
                        .actual(pc.getActual())
                        .build())
                .collect(Collectors.toList());

        List<PerfilCompletoResponse.DocumentoItem> documentos = documentoRepository.findByPersonaId(personaId).stream()
                .map(d -> PerfilCompletoResponse.DocumentoItem.builder()
                        .id(d.getId())
                        .tipoDocumento(d.getTipoDocumento() != null ? d.getTipoDocumento().name() : null)
                        .nombreOriginal(d.getNombreOriginal())
                        .contentType(d.getContentType())
                        .tamanioBytes(d.getTamanioBytes())
                        .fechaCarga(d.getFechaCarga())
                        .build())
                .collect(Collectors.toList());

        return PerfilCompletoResponse.builder()
                .persona(personaService.obtener(personaId))
                .cargos(cargos)
                .centrosLaborales(centros)
                .documentos(documentos)
                .build();
    }

    @Override
    @Transactional
    public DocumentoDescarga descargarDocumento(UUID personaId, UUID documentoId) {
        DocumentoPersona doc = documentoRepository.buscarPorId(documentoId)
                .filter(d -> d.getPersona() != null && personaId.equals(d.getPersona().getId()))
                .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));
        byte[] contenido = almacenamiento.obtener(doc.getStorageKey());
        return new DocumentoDescarga(contenido, doc.getContentType(), doc.getNombreOriginal());
    }

    // ── helpers ──

    private long cargosActuales(PerfilCompletoData data) {
        return data.getCargos() == null ? 0 :
                data.getCargos().stream().filter(c -> Boolean.TRUE.equals(c.getActual())).count();
    }

    private long centrosActuales(PerfilCompletoData data) {
        return data.getCentrosLaborales() == null ? 0 :
                data.getCentrosLaborales().stream().filter(c -> Boolean.TRUE.equals(c.getActual())).count();
    }

    private void validarUnSoloActual(long actuales, String etiqueta) {
        if (actuales > 1) {
            throw new ValidationException("Solo puede haber un " + etiqueta + " marcado como actual");
        }
    }

    private void validarArchivo(TipoDocumento tipo, ArchivoSubido archivo) {
        if (archivo.tamanio() > maxSizeBytes) {
            throw new ValidationException("El archivo de " + tipo.name() + " supera el tamaño máximo permitido");
        }
        List<String> permitidos = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim).collect(Collectors.toList());
        if (archivo.contentType() == null || !permitidos.contains(archivo.contentType())) {
            throw new ValidationException("Tipo de archivo no permitido para " + tipo.name()
                    + " (permitidos: " + allowedContentTypes + ")");
        }
    }

    private String sha256(byte[] contenido) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(contenido);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void eliminarSilencioso(String key) {
        try {
            almacenamiento.eliminar(key);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo eliminar archivo " + key + ": " + e.getMessage());
        }
    }
}
