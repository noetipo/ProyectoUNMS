package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.DictamenBandejaItem;
import unmsm.edu.pe.tesis.application.dto.DictamenDetalle;
import unmsm.edu.pe.tesis.application.dto.DictamenResponse;
import unmsm.edu.pe.tesis.application.dto.DictamenResumen;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenRequest;

import java.util.UUID;

public interface SecretariaDictamenService {
    PageResponse<DictamenBandejaItem> bandeja(String estado, UUID facultadId, UUID programaId, String buscar, int page, int size);
    DictamenResumen resumen();
    DictamenDetalle detalle(UUID tesisId);
    /** Descarga un firmado subido por el estudiante (tipo = solicitud | carta). */
    ArchivoDescargable descargarFirmadoEstudiante(UUID tesisId, String tipo);
    /** Genera/elabora el dictamen: asigna correlativo y snapshot; estado ELABORADO. */
    DictamenResponse elaborar(UUID tesisId, ElaborarDictamenRequest req);
    /** Render del dictamen elaborado en "pdf" | "docx". */
    byte[] documentoDictamen(UUID tesisId, String formato);
    /** Sube el dictamen firmado por el director; estado FIRMADO + fecha de emisión. */
    void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);
    /** Observa los documentos: estado OBSERVADO; el estudiante debe re-subir. */
    void observar(UUID tesisId, String motivo);
}
