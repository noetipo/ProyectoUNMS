package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObservarDictamenRequest {
    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
}
