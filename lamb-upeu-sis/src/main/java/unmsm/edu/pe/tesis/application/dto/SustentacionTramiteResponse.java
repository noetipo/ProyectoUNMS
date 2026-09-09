package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detalle del trámite de Sustentación (Etapa 8) para la Secretaría. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SustentacionTramiteResponse {
    private UUID tesisId;
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;

    private LocalDate fechaSolicitud;
    private boolean expedienteRecibido;
    private LocalDate fechaRecepcion;

    private List<JuradoItem> jurado;

    // Dictamen de designación del Jurado de Sustentación
    private String estadoDictamen;
    private String dictamenNumero;
    private String dictamenExpediente;
    private LocalDate dictamenFechaEmision;
    private boolean dictamenElaborado;
    private boolean dictamenFirmadoSubido;
    private String numeroSugerido;
    private String expedienteSugerido;

    // Programación del acto
    private boolean sustentacionProgramada;
    private LocalDate fechaSustentacion;
    private String horaSustentacion;
    private String lugarSustentacion;
    private String modalidad;
    private String modalidadLabel;
    private String enlaceSustentacion;

    // Acta y cierre
    private boolean actaSubida;
    private LocalDate fechaActa;
    private String resultado;
    private String resultadoLabel;
    private String observacionActa;

    private boolean concluida;
    private LocalDate fechaConclusion;
    private String pendiente;
}
