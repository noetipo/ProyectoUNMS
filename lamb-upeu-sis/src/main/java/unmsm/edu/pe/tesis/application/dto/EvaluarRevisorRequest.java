package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.util.Map;

/** Evaluación del revisor: puntajes por criterio + comentario + si da conformidad. */
@Data
public class EvaluarRevisorRequest {
    private Map<String, Integer> puntajes;   // criterio -> 1..4
    private String comentario;
    private boolean conforme;                // true = CONFORME, false = OBSERVADO
}
