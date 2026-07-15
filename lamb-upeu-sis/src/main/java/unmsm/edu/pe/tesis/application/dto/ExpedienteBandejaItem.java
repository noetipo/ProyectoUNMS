package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la bandeja de la Secretaría (Etapa 5): expediente con solicitud de aprobación enviada. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteBandejaItem {
    private UUID tesisId;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private LocalDate fechaSolicitud;
    private boolean recibido;
}
