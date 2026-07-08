package unmsm.edu.pe.personas.infrastructure.storage;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.shared.exceptions.BusinessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Almacenamiento local en una carpeta externa configurable.
 * Activo cuando {@code storage.provider=local} (por defecto).
 */
@IfBuildProperty(name = "storage.provider", stringValue = "local", enableIfMissing = true)
@ApplicationScoped
public class LocalStorage implements AlmacenamientoArchivos {

    private static final Logger LOG = Logger.getLogger(LocalStorage.class);

    @ConfigProperty(name = "storage.local.base-path")
    String basePath;

    @Override
    public String guardar(byte[] contenido, String nombreOriginal, String contentType) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String safeName = sanitizar(nombreOriginal);
        String key = fecha + "/" + UUID.randomUUID() + "_" + safeName;
        Path destino = Paths.get(basePath, key);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
            return key;
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] obtener(String storageKey) {
        try {
            return Files.readAllBytes(Paths.get(basePath, storageKey));
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo: " + storageKey, e);
        }
    }

    @Override
    public void eliminar(String storageKey) {
        try {
            Files.deleteIfExists(Paths.get(basePath, storageKey));
        } catch (IOException e) {
            LOG.warn("No se pudo eliminar el archivo " + storageKey + ": " + e.getMessage());
        }
    }

    private String sanitizar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "archivo";
        }
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
