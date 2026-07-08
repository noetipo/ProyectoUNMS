package unmsm.edu.pe.personas.domain.storage;

/**
 * Puerto de almacenamiento de archivos. El dominio depende solo de esta
 * abstracción; las implementaciones (local, S3) viven en infraestructura y se
 * seleccionan por configuración ({@code storage.provider}).
 */
public interface AlmacenamientoArchivos {

    /**
     * Guarda el contenido y devuelve la clave de almacenamiento (storage_key)
     * con la que luego se puede recuperar o eliminar.
     */
    String guardar(byte[] contenido, String nombreOriginal, String contentType);

    /** Recupera el contenido a partir de su storage_key. */
    byte[] obtener(String storageKey);

    /** Elimina el archivo (best-effort). */
    void eliminar(String storageKey);
}
