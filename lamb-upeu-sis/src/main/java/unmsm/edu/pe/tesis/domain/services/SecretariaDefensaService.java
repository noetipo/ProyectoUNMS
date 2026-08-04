package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ExpedienteBandejaItem;
import unmsm.edu.pe.tesis.application.dto.RubricaBandejaItem;

import java.util.List;
import java.util.UUID;

/** Secretaría · Etapa 5 (Defensa) — paso 1: recibir el expediente y comunicar al Coordinador. */
public interface SecretariaDefensaService {

    PageResponse<ExpedienteBandejaItem> bandeja(String buscar, int page, int size);

    /** La Secretaría recibe el expediente y lo comunica al Coordinador del Programa. */
    void recibir(UUID tesisId);

    /** Proyectos con revisores designados que requieren (o ya tienen) la rúbrica oficial subida. */
    List<RubricaBandejaItem> bandejaRubricas(String buscar);

    /** La Secretaría sube la rúbrica oficial (Word .docx) para habilitar la evaluación de los revisores. */
    /** Enciende/apaga la evaluación con la rúbrica oficial del sistema (reemplaza a subir el Word). */
    void habilitarRubrica(UUID tesisId, boolean habilitar);

    /** Legado: adjuntar un Word propio del expediente. Se mantiene para casos excepcionales. */
    void subirRubrica(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);

    /** Vista previa (texto) de la rúbrica subida, para confirmar que es el documento correcto (no descarga). */
    List<String> previsualizarRubrica(UUID tesisId);

    /** El Word (.docx) crudo de la rúbrica, para renderizarlo en el navegador (vista previa fiel). */
    unmsm.edu.pe.tesis.application.dto.ArchivoDescargable documentoRubrica(UUID tesisId);

    // ── Programación de la defensa (la realiza la Secretaría) ──

    /** Info de la defensa: si está programada, fecha/hora/lugar, dictamen y jurado. */
    unmsm.edu.pe.tesis.application.dto.DefensaInfo defensa(UUID tesisId);

    /** Docentes de la línea de la tesis, seleccionables como Jurado Examinador. */
    List<unmsm.edu.pe.tesis.application.dto.DocenteOpcion> docentesDefensa(UUID tesisId);

    /** Programa la defensa: Jurado Examinador (presidente + 2 miembros + asesor) + fecha/hora/lugar. */
    void programarDefensa(UUID tesisId, unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest req);
}
