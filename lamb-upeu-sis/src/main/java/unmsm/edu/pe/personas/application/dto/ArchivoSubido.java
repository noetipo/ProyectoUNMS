package unmsm.edu.pe.personas.application.dto;

/**
 * Archivo ya leído en memoria (desacopla el caso de uso del transporte HTTP /
 * del storage real, lo que facilita testearlo).
 */
public record ArchivoSubido(byte[] contenido, String nombreOriginal, String contentType, long tamanio) {
}
