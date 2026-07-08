package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Registro/edición de tema por el coordinador. El {@code nivel}, la {@code fecha}
 * y el {@code creador} los resuelve el servidor; NO se piden aquí.
 */
@Data
public class RegistrarTemaRequest {

    @NotNull(message = "La línea de investigación es obligatoria")
    private UUID lineaInvestigacionId;

    @NotBlank(message = "El título del tema es obligatorio")
    @Size(max = 500, message = "El título no puede exceder 500 caracteres")
    private String titulo;

    @Size(max = 2000, message = "El resumen no puede exceder 2000 caracteres")
    private String resumen;
}
