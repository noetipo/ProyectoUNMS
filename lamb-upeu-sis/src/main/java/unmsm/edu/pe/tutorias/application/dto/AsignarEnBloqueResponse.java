package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarEnBloqueResponse {
    /** Estudiantes asignados (nuevos + reemplazos de tutor). */
    private int asignados;
    /** Estudiantes que ya tenían a este tutor (idempotencia, sin cambios). */
    private int sinCambio;
    /** De los asignados, cuántos tenían otro tutor que se cerró (reemplazos). */
    private int reemplazos;
    private int cupoMaximo;
    private int cupoAntes;
    private int cupoDespues;
    private String mensaje;
}
