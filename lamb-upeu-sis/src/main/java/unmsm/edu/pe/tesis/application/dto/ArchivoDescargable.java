package unmsm.edu.pe.tesis.application.dto;

/** Archivo almacenado listo para transmitir (descarga de firmados / dictamen). */
public record ArchivoDescargable(byte[] contenido, String contentType, String nombreOriginal) {}
