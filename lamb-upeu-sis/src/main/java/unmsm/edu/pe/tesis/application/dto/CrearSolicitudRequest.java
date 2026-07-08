package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudRequest {

    @NotNull(message = "El docente es obligatorio")
    private UUID docenteId;

    @NotNull(message = "La línea de investigación es obligatoria")
    private UUID lineaInvestigacionId;

    private String tituloTentativo;
    private String mensaje;

    /** ASESOR (por defecto) o COASESOR. */
    private TipoAsesoria tipo;
}
