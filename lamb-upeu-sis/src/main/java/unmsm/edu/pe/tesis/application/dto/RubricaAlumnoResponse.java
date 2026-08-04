package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * La rúbrica con la que se evaluará el proyecto, para que el doctorando sepa <b>cómo lo miden</b>
 * antes de que lo midan. Es la misma definición que usa el revisor, sin ningún puntaje: solo los
 * criterios y cuánto vale cada nivel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricaAlumnoResponse {
    private String enfoque;          // CUANTITATIVO | CUALITATIVO
    private String enfoqueLabel;     // "Cuantitativa / mixta" | "Cualitativa"
    private String titulo;           // título de la rúbrica oficial
    private String version;          // versión vigente publicada (null si aún no hay Word)
    private boolean documentoDisponible;
    private int puntajeTotal;
    private int puntajeAprobacion;
    private List<RubricaSeccionItem> secciones;
}
