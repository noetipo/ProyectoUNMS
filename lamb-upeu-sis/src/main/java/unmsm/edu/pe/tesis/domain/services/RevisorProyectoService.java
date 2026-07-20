package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.EvaluacionRevisorResponse;
import unmsm.edu.pe.tesis.application.dto.EvaluarRevisorRequest;
import unmsm.edu.pe.tesis.application.dto.RevisorBandejaItem;

import java.util.List;
import java.util.UUID;

/** Casos de uso del REVISOR (Jurado Informante) sobre el proyecto (Etapa 5, paso 3). */
public interface RevisorProyectoService {

    /** Proyectos asignados al docente revisor actual. */
    List<RevisorBandejaItem> bandeja();

    /** Detalle del proyecto (solo lectura) + rúbrica con la evaluación del revisor. */
    EvaluacionRevisorResponse detalle(UUID tesisId);

    /** Registra la evaluación con rúbrica (observa o da conformidad). */
    void evaluar(UUID tesisId, EvaluarRevisorRequest req);
}
