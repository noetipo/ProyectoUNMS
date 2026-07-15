package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.time.LocalDate;

/** Alta/edición de una actividad del cronograma. */
@Data
public class ActividadRequest {
    private String nombre;
    private String fase;        // FaseActividad
    private Integer mesInicio;
    private Integer mesFin;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;      // EstadoActividad
    private Integer orden;
}
