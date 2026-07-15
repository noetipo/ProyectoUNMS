package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Cambio de enfoque del proyecto (CUANTITATIVO | CUALITATIVO). */
@Data
public class EnfoqueRequest {
    @NotBlank(message = "El enfoque es obligatorio")
    private String enfoque;
}
