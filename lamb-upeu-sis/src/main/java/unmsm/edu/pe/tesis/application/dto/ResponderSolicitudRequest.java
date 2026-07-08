package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.tesis.domain.enums.DecisionSolicitud;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponderSolicitudRequest {

    @NotNull(message = "La decisión es obligatoria")
    private DecisionSolicitud decision;

    /** Obligatorio cuando la decisión es RECHAZAR. */
    private String motivo;
}
