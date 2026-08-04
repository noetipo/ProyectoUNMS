package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.EvaluacionRevisorResponse;
import unmsm.edu.pe.tesis.application.dto.EvaluarRevisorRequest;
import unmsm.edu.pe.tesis.application.dto.ObservarItemRequest;
import unmsm.edu.pe.tesis.application.dto.RevisorBandejaItem;

import java.util.List;
import java.util.UUID;

/** Casos de uso del REVISOR (Jurado Informante) sobre el proyecto (Etapa 5, paso 3). */
public interface RevisorProyectoService {

    /** Proyectos asignados al docente revisor actual. */
    List<RevisorBandejaItem> bandeja();

    /** Detalle del proyecto (solo lectura) + rúbrica con la evaluación del revisor. */
    EvaluacionRevisorResponse detalle(UUID tesisId);

    /** Descarga la rúbrica oficial (Word) que subió Secretaría, solo para referencia del revisor. */
    ArchivoDescargable descargarRubrica(UUID tesisId);

    /** Descarga la rúbrica ya llenada (Word) con los niveles y puntajes que el revisor registró. */
    ArchivoDescargable descargarRubricaLlenada(UUID tesisId);

    /** Genera el PDF del proyecto para el visor del revisor (solo lectura). */
    ArchivoDescargable descargarProyectoPdf(UUID tesisId);

    /** Observa un ítem/campo del proyecto (queda en el historial del estudiante como informe de revisor). */
    void observarItem(UUID tesisId, ObservarItemRequest req);

    /** Valida la corrección de un ítem: lo da por conforme (el estudiante ya no debe corregirlo). */
    void darConformidadItem(UUID tesisId, String campo);

    /** Registra la evaluación con rúbrica (observa o da conformidad). */
    void evaluar(UUID tesisId, EvaluarRevisorRequest req);
}
