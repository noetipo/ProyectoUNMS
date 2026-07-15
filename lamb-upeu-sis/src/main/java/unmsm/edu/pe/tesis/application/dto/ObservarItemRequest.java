package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** El asesor observa un campo/ítem del proyecto. */
@Data
public class ObservarItemRequest {
    @NotBlank(message = "El campo a observar es obligatorio")
    private String campo;

    @NotBlank(message = "El texto de la observación es obligatorio")
    private String texto;
}
