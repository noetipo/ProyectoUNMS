package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Datos que la Secretaría escribe al emitir el dictamen. El N° de dictamen y el N° de expediente
 * digital son <b>manuales</b>: los correlativos oficiales los lleva la UPG en su propio registro y
 * no siempre coinciden con los del sistema. Si llegan vacíos se conserva la propuesta automática.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElaborarDictamenRequest {
    @NotNull(message = "La fecha de solicitud es obligatoria")
    private LocalDate fechaSolicitud;
    private String numero;
    private String expediente;
}
