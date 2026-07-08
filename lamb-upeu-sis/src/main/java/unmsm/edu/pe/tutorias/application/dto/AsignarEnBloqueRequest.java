package unmsm.edu.pe.tutorias.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarEnBloqueRequest {

    @NotNull(message = "El tutor es obligatorio")
    private UUID tutorId;

    @NotEmpty(message = "Debe seleccionar al menos un estudiante")
    private List<UUID> estudianteIds;

    private String motivoCambio;
}
