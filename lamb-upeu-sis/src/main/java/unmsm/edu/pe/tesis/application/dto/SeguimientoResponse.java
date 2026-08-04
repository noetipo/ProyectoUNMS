package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta del tablero de seguimiento: estadística + listado de alumnos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoResponse {
    private SeguimientoResumen resumen;
    private List<SeguimientoAlumnoItem> alumnos;
}
