package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un criterio de la rúbrica + el puntaje que el revisor le dio (si ya evaluó). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricaCriterioItem {
    private String key;
    private String titulo;
    private String descripcion;
    private Integer puntaje;   // 1..4, o null si aún no evaluó
}
