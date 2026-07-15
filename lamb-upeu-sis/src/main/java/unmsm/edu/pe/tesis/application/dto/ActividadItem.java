package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Actividad del cronograma / plan de actividades. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadItem {
    private UUID id;
    private String nombre;
    private String fase;        // FaseActividad
    private Integer mesInicio;
    private Integer mesFin;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;      // EstadoActividad
    private Integer orden;
}
