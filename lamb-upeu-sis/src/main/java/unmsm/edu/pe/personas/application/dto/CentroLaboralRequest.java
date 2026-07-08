package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentroLaboralRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    private String descripcion;
}
