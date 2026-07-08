package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.MiAsesoriaResponse;

public interface MiAsesoriaService {

    String TIPO_SOLICITUD = "SOLICITUD_ASESORIA";
    String TIPO_CARTA = "CARTA_ACEPTACION";

    /** Bandeja del estudiante autenticado (tema, sugeridos, estado, documentos). */
    MiAsesoriaResponse bandeja();

    /**
     * PDF de un documento del estudiante autenticado (validación de propiedad implícita:
     * el estudiante se resuelve del token y solo se generan SUS documentos).
     * @param tipo {@link #TIPO_SOLICITUD} o {@link #TIPO_CARTA}
     */
    /** Documento propio (SOLICITUD_ASESORIA | CARTA_ACEPTACION) en formato "pdf" o "docx". */
    byte[] documentoBytes(String tipo, String formato);

    /** El estudiante sube uno de sus documentos firmados (tipo = solicitud | carta). Permite reemplazar. */
    void subirFirmado(String tipo, byte[] contenido, String nombreOriginal, String contentType);

    /** Descarga el dictamen firmado emitido por la secretaría (valida propiedad). */
    unmsm.edu.pe.tesis.application.dto.ArchivoDescargable descargarDictamenEmitido();
}
