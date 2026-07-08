package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Detalle para el formulario "Elaborar dictamen" de la secretaría. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictamenDetalle {
    private UUID tesisId;
    private String estudianteNombre;   // "Apellidos, Nombres"
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private String asesorNombre;
    private String asesorGrado;
    private String coasesorNombre;
    private String coasesorGrado;
    private String numero;
    private String expediente;
    private LocalDate fechaSolicitud;
    private String estado;
    private boolean solicitudFirmadaDisponible;
    private boolean cartaFirmadaDisponible;
    private boolean dictamenFirmadoSubido;
}
