package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Vista del revisor: proyecto (solo lectura) + rúbrica oficial con su evaluación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionRevisorResponse {
    private ProyectoEditorResponse proyecto;       // contenido del proyecto (solo lectura)
    private String rubricaEnfoque;                 // CUALITATIVO | CUANTITATIVO
    private String rubricaTitulo;                  // título oficial de la rúbrica
    private List<RubricaSeccionItem> secciones;    // secciones → criterios (con mi nivel/puntaje)
    private String miEstado;                       // DESIGNADO | OBSERVADO | CONFORME
    private String miComentario;
    private Integer miPuntajeTotal;                // suma obtenida
    private int puntajeMaximo;                     // 100
    private int aprobadoMin;                        // 65
    private Boolean aprobado;                       // total ≥ 65 (null si aún no evaluó)
    private boolean cerrada;                        // ya di conformidad o etapa avanzada → no editable
    private boolean rubricaDisponible;             // Secretaría ya subió la rúbrica oficial → habilita evaluar
    private String rubricaNombreArchivo;           // nombre del Excel subido (para descargar)
    private String respuestaEstudiante;            // levantamiento del estudiante a mis observaciones
}
