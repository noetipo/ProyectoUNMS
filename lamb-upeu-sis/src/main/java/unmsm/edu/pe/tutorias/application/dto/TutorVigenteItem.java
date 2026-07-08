package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Tutor vigente de un estudiante (tarjeta del formulario). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorVigenteItem {
    private UUID tutoriaId;
    private UUID tutorId;
    private String tutorNombre;
    private String gradoAcademico;
    private String codigoSistema;
    private int estudiantesActuales;
    private int cupoMaximo;
    private LocalDate fechaInicio;
}
