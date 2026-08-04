package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Una sección (rubro) de la rúbrica oficial con sus criterios y subtotal. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricaSeccionItem {
    private String key;
    private String titulo;
    private Integer subtotalMaximo;              // suma de los "cumple" de la sección
    private Integer subtotalObtenido;            // suma de los puntajes obtenidos en la sección
    private List<RubricaCriterioItem> criterios;
}
