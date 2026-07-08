package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;

/** Un grado académico de la persona (grado + año + universidad + principal). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradoAcademicoRequest {

    @NotNull(message = "El grado es obligatorio")
    private GradoAcademico grado;

    private Integer anio;

    @Size(max = 200, message = "La universidad no puede exceder 200 caracteres")
    private String universidad;

    private Boolean principal;
}
