package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Una evaluación de avance registrada por el asesor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvanceItem {
    private UUID id;
    private LocalDate fechaEvaluacion;
    private Integer puntajeEjecucion;
    private Integer puntajeDatos;
    private Integer puntajeAnalisis;
    private Integer puntajeInterpretacion;
    private Integer puntajeTotal;
    private Integer porcentajePlan;
    private String comentario;
}
