package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;

import java.util.UUID;

/** El tutor sugiere un docente asesor a su tutorando. */
@Data
public class SugerirAsesorRequest {

    @NotNull(message = "El docente asesor es obligatorio")
    private UUID asesorDocenteId;

    /** Puesto para el que se sugiere: ASESOR (por defecto) o COASESOR. */
    private TipoAsesoria tipo;

    @Size(max = 300, message = "La nota no puede exceder 300 caracteres")
    private String nota;
}
