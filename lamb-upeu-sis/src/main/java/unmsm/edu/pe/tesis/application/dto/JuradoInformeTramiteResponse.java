package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detalle del trámite del Jurado Informante para la Secretaría: recepción, jurado designado,
 * dictamen de designación, archivo del expediente y Dictamen de Expedito — en una sola pantalla,
 * porque son pasos consecutivos del mismo expediente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JuradoInformeTramiteResponse {
    private UUID tesisId;
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;

    private LocalDate fechaSolicitud;
    private boolean expedienteRecibido;
    private LocalDate fechaRecepcion;

    private List<JuradoItem> jurado;
    private boolean informeFinalRevisado;

    // Dictamen de designación del Jurado Informante
    private String estadoDictamen;
    private String dictamenNumero;
    private String dictamenExpediente;
    private LocalDate dictamenFechaEmision;
    private boolean dictamenElaborado;
    private boolean dictamenFirmadoSubido;
    private String numeroSugerido;
    private String expedienteSugerido;

    // Archivo del expediente
    private boolean informeFinalArchivado;
    private LocalDate fechaArchivoInforme;

    // Dictamen de Expedito
    private String estadoExpedito;
    private String expeditoNumero;
    private String expeditoExpediente;
    private LocalDate expeditoFechaEmision;
    private boolean expeditoElaborado;
    private boolean expeditoFirmadoSubido;
    private String expeditoNumeroSugerido;
    private String expeditoExpedienteSugerido;

    private String pendiente;
}
