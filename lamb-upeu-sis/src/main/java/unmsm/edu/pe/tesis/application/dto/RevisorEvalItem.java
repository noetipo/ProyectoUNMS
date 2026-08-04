package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Evaluación de un revisor, como la ve el estudiante (Etapa 5, paso 4). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisorEvalItem {
    private UUID revisorId;
    private Integer orden;          // Revisor 1, Revisor 2
    private String docenteNombre;   // nombre del revisor designado
    private String docenteCategoria;// categoría docente (ej. Docente Asociado)
    private String docenteLinea;    // línea de investigación
    private String estado;          // DESIGNADO | OBSERVADO | CONFORME
    private String comentario;      // observación del revisor
    private String respuesta;       // levantamiento del estudiante
    private Integer puntajeTotal;   // puntaje obtenido (sobre 100)
    private Integer puntajeMaximo;  // 100
    private Boolean aprobado;       // puntajeTotal ≥ 65 (null si aún no evaluó)
}
