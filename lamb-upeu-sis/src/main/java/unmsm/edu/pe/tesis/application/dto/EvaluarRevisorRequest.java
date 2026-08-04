package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.util.Map;

/** Evaluación del revisor: nivel + observación por criterio + si da conformidad. */
@Data
public class EvaluarRevisorRequest {
    private Map<String, String> niveles;         // criterio -> CUMPLE | PARCIAL | NO_CUMPLE
    private Map<String, String> observaciones;   // criterio -> observación / sugerencia (opcional)
    private String comentario;                   // nota general (opcional)
    private boolean conforme;                    // true = CONFORME, false = OBSERVADO
}
