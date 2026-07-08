package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de la bandeja del docente. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudBandejaItem {
    private UUID id;
    private UUID estudianteId;
    private String estudianteNombres;
    private String estudianteApellidos;
    private String codigoSistema;
    private String programaNombre;
    private String lineaNombre;
    private String tituloTentativo;
    private String mensaje;
    private String tipo;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private String motivoRespuesta;
}
