package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Vista del revisor: proyecto (solo lectura) + rúbrica con su evaluación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionRevisorResponse {
    private ProyectoEditorResponse proyecto;     // contenido del proyecto (solo lectura)
    private List<RubricaCriterioItem> rubrica;   // criterios + mi puntaje
    private String miEstado;                     // DESIGNADO | OBSERVADO | CONFORME
    private String miComentario;
    private Integer miPuntajeTotal;
    private int puntajeMaximo;
    private boolean cerrada;                     // ya di conformidad → no editable
    private String respuestaEstudiante;          // levantamiento del estudiante a mis observaciones
}
