package unmsm.edu.pe.tutorias.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarIndividualRequest {

    @NotNull(message = "El estudiante es obligatorio")
    private UUID estudianteId;

    @NotNull(message = "El tutor es obligatorio")
    private UUID tutorId;

    private String motivoCambio;
}
