package unmsm.edu.pe.personas.infrastructure.storage;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;

import java.util.Optional;

/**
 * Almacenamiento en S3. Activo cuando {@code storage.provider=s3}.
 *
 * NOTA: implementación pendiente (stub seleccionable por flag). Para completarla,
 * agregar la extensión {@code quarkus-amazon-s3} (o AWS SDK v2) e inyectar el
 * cliente S3, usando {@code storage.s3.bucket}/{@code storage.s3.region}. Las
 * credenciales deben venir por variables de entorno / IAM, nunca hardcodeadas.
 * El resto del código depende solo del puerto {@link AlmacenamientoArchivos},
 * por lo que cambiar a S3 no requiere tocar la lógica de negocio.
 */
@IfBuildProperty(name = "storage.provider", stringValue = "s3")
@ApplicationScoped
public class S3Storage implements AlmacenamientoArchivos {

    @ConfigProperty(name = "storage.s3.bucket", defaultValue = "")
    Optional<String> bucket;

    @ConfigProperty(name = "storage.s3.region", defaultValue = "us-east-1")
    String region;

    @Override
    public String guardar(byte[] contenido, String nombreOriginal, String contentType) {
        throw noImplementado();
    }

    @Override
    public byte[] obtener(String storageKey) {
        throw noImplementado();
    }

    @Override
    public void eliminar(String storageKey) {
        throw noImplementado();
    }

    private UnsupportedOperationException noImplementado() {
        return new UnsupportedOperationException(
                "Almacenamiento S3 aún no implementado (storage.provider=s3). " +
                        "Agrega la extensión quarkus-amazon-s3 y completa S3Storage, o usa storage.provider=local.");
    }
}
