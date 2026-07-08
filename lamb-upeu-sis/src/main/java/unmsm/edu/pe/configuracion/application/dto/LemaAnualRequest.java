package unmsm.edu.pe.configuracion.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LemaAnualRequest {
    @NotNull(message = "El año es obligatorio")
    @Min(value = 1900, message = "El año no es válido")
    @Max(value = 2100, message = "El año no es válido")
    private Integer anio;

    @NotBlank(message = "El texto es obligatorio")
    @Size(max = 300, message = "El texto no puede superar 300 caracteres")
    private String texto;
}
