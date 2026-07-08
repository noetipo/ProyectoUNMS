package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Línea de investigación seleccionada para el perfil docente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocenteLineaRequest {

    @NotNull(message = "La línea de investigación es obligatoria")
    private UUID lineaInvestigacionId;

    private Boolean esPrincipal;
}
