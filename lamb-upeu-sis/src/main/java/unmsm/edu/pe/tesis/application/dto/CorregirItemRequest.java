package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** El estudiante confirma la corrección de un ítem observado. */
@Data
public class CorregirItemRequest {
    @NotBlank(message = "El campo a corregir es obligatorio")
    private String campo;

    /** Nota/respuesta del estudiante sobre la corrección realizada. */
    private String respuesta;
}
