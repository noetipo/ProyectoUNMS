package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.PlantillaRubricaItem;

import java.util.List;
import java.util.UUID;

/** Rúbricas oficiales de los revisores: una por enfoque, versionadas por año. */
public interface PlantillaRubricaService {

    /** Las dos rúbricas (cuantitativa/mixta y cualitativa) con su versión vigente e historial. */
    List<PlantillaRubricaItem> listar();

    /** Publica una versión nueva del enfoque; la anterior deja de ser vigente pero se conserva. */
    PlantillaRubricaItem publicar(String enfoque, String version, byte[] contenido,
                                  String nombreOriginal, String contentType);

    /** Word de una versión concreta (para la vista previa y la descarga). */
    ArchivoDescargable documento(UUID plantillaId);

    /** Word de la versión vigente del enfoque; es la que ve el revisor. */
    ArchivoDescargable documentoVigente(String enfoque);
}
