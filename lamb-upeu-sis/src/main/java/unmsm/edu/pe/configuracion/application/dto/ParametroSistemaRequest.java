package unmsm.edu.pe.configuracion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistemaRequest {
    // La clave solo se usa al crear; en edición es inmutable (se ignora).
    @NotBlank(message = "La clave es obligatoria")
    @Size(max = 60, message = "La clave no puede superar 60 caracteres")
    private String clave;

    @NotBlank(message = "El valor es obligatorio")
    @Size(max = 500, message = "El valor no puede superar 500 caracteres")
    private String valor;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;
}
