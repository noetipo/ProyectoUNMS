package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.util.Map;

/** Registro de una evaluación de avance por el asesor (rúbrica de avances + comentario). */
@Data
public class RegistrarAvanceRequest {
    private Map<String, Integer> puntajes;   // ejecucion/datos/analisis/interpretacion -> 1..4
    private String comentario;
}
