package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.EvaluarInformeRequest;
import unmsm.edu.pe.tesis.application.dto.InformeEvaluacionResponse;
import unmsm.edu.pe.tesis.application.dto.InformeJuradoBandejaItem;

import java.util.List;
import java.util.UUID;

/** Casos de uso del Jurado Informante sobre el informe final (Etapa 7). */
public interface InformeJuradoService {

    /** Informes finales asignados al docente (miembro del Jurado Informante). */
    List<InformeJuradoBandejaItem> bandeja();

    /** Detalle del informe final + la evaluación del jurado. */
    InformeEvaluacionResponse detalle(UUID tesisId);

    /** Registra la evaluación del informe final (observa o da conformidad). */
    void evaluar(UUID tesisId, EvaluarInformeRequest req);
}
