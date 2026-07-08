package unmsm.edu.pe.personas.application.dto;

/**
 * Contenido de un documento recuperado del almacenamiento, listo para servir por
 * HTTP. Desacopla el caso de uso del transporte y del storage real.
 */
public record DocumentoDescarga(byte[] contenido, String contentType, String nombreOriginal) {
}
