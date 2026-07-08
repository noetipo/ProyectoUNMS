package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila del historial de tutorías de un estudiante. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutoriaHistorialItem {
    private UUID id;
    private UUID docenteId;
    private String docenteNombre;
    private String gradoAcademico;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean actual;
    private String motivoCambio;
}
