package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un criterio de la rúbrica oficial + el nivel/puntaje que el revisor le dio (si ya evaluó). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricaCriterioItem {
    private String key;
    private String titulo;
    private String descripcion;
    private Integer cumple;      // puntaje si "Cumple"
    private Integer parcial;     // puntaje si "Cumple parcialmente"
    private Integer noCumple;    // puntaje si "No cumple"
    private String nivel;        // CUMPLE | PARCIAL | NO_CUMPLE, o null si aún no evaluó
    private Integer puntaje;     // puntaje resultante del nivel, o null
    private String observacion;  // observación / sugerencia del revisor para este criterio

    /**
     * Lo que el estudiante escribió al corregir ESTE criterio (último evento de corrección en el
     * ítem del proyecto al que corresponde). Es lo que el revisor tiene que verificar antes de
     * aceptar la corrección; sin esto solo le llegaba una frase global suelta.
     */
    private String correccionEstudiante;
    /** Fecha de esa corrección, formateada (dd/MM/yyyy HH:mm). */
    private String correccionFecha;
}
