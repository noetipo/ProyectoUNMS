package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** El tutor sugiere un docente asesor a su tutorando. */
@Data
public class SugerirAsesorRequest {

    @NotNull(message = "El docente asesor es obligatorio")
    private UUID asesorDocenteId;

    @Size(max = 300, message = "La nota no puede exceder 300 caracteres")
    private String nota;
}
