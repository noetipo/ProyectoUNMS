package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.CierreBandejaItem;
import unmsm.edu.pe.tesis.application.dto.CierreProyectoResponse;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenAprobacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarResultadoDefensaRequest;

import java.util.List;
import java.util.UUID;

/**
 * Secretaría · Etapa 5, tramo final: lo que ocurre después de la defensa oral.
 * Recepción de las rúbricas de los revisores, registro del resultado, dictamen de
 * aprobación del proyecto y archivo del expediente (que abre la Etapa 6).
 */
public interface CierreProyectoService {

    /** Proyectos con la defensa programada, con lo que falta en cada uno. */
    List<CierreBandejaItem> bandeja(String buscar);

    /** Estado completo del cierre de un proyecto (los tres pasos de la pantalla). */
    CierreProyectoResponse estado(UUID tesisId);

    // ── Paso 1 · resultado de la defensa y rúbricas de los revisores ──

    /** Recepciona la rúbrica firmada de un revisor, con la nota que consignó. */
    void recepcionarRubrica(UUID tesisId, UUID docenteId, Integer puntaje,
                            byte[] contenido, String nombreOriginal, String contentType);

    /** El .docx/.pdf de una rúbrica recepcionada, para verla sin descargarla. */
    ArchivoDescargable documentoRubrica(UUID tesisId, UUID docenteId);

    /** Registra el resultado del acto de defensa (aprobado, con observaciones o desaprobado). */
    void registrarResultado(UUID tesisId, RegistrarResultadoDefensaRequest req);

    // ── Paso 2 · dictamen de aprobación ──

    /** Elabora el dictamen: numeración manual, vigencia y snapshot de los marcadores. */
    void elaborarDictamen(UUID tesisId, ElaborarDictamenAprobacionRequest req);

    /** Render del dictamen elaborado en "pdf" | "docx". */
    byte[] documentoDictamen(UUID tesisId, String formato);

    /** Sube el dictamen firmado por el Director. */
    void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);

    // ── Paso 3 · archivo del expediente ──

    /** Archiva el proyecto final aprobado y cierra la etapa (el proceso pasa a la Etapa 6). */
    void archivarProyectoFinal(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);

    /** Archiva el proyecto final tomando el que ya subió el estudiante. */
    void archivarProyectoDelEstudiante(UUID tesisId);
}
