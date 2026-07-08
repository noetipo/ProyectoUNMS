package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudResponse {
    private UUID id;
    private UUID estudianteId;
    private UUID docenteId;
    private UUID lineaInvestigacionId;
    private String tituloTentativo;
    private String mensaje;
    private String tipo;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private String motivoRespuesta;
}
