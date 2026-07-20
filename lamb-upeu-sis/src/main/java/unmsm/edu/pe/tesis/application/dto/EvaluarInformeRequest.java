package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Evaluación del informe final por un miembro del Jurado Informante. */
@Data
public class EvaluarInformeRequest {
    private Integer puntaje;      // 0..20
    private String comentario;
    private boolean conforme;     // true = CONFORME, false = OBSERVADO
}
