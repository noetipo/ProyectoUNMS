package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de "mis solicitudes" del estudiante. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudMiaItem {
    private UUID id;
    private UUID docenteId;
    private String docenteNombres;
    private String docenteApellidos;
    private String codigoSistema;
    private String gradoAcademico;
    private String lineaNombre;
    private String tituloTentativo;
    private String mensaje;
    private String tipo;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private String motivoRespuesta;
}
