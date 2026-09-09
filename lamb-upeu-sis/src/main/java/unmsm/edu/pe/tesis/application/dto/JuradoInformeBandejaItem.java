package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la bandeja del trámite del Jurado Informante: expedientes que el estudiante ya solicitó. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JuradoInformeBandejaItem {
    private UUID tesisId;
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private LocalDate fechaSolicitud;
    private boolean expedienteRecibido;
    private int numJurado;
    private boolean informeFinalRevisado;
    private String estadoDictamen;
    private boolean informeFinalArchivado;
    private String estadoExpedito;
    /** Qué le toca hacer ahora a la Secretaría. */
    private String pendiente;
}
