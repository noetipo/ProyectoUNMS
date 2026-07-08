package unmsm.edu.pe.tesis.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Bandeja del estudiante: tema, asesores sugeridos, estado de la solicitud y documentos. */
@Data
@Builder
public class MiAsesoriaResponse {

    // ── Tema (tesis activa) ──
    private boolean conTema;
    private UUID tesisId;
    private String temaTitulo;
    private String lineaNombre;
    private String nivel;
    private String estadoDerivado; // SIN_TEMA | SIN_ASESOR | <estado tesis>

    // ── Asesores sugeridos por el tutor ──
    private List<AsesorSugeridoItem> sugeridos;

    // ── Solicitud/asesoría actual ──
    private UUID solicitudId;
    private String solicitudEstado; // SIN_SOLICITUD | PENDIENTE | ACEPTADA | RECHAZADA | CANCELADA
    private UUID docenteSolicitadoId;
    private String docenteSolicitadoNombre;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private String motivoRespuesta;

    // ── Documentos disponibles para descargar ──
    private boolean solicitudPdfDisponible;
    private boolean cartaPdfDisponible;

    // ── Firmados subidos por el estudiante + dictamen ──
    private boolean solicitudFirmadaSubida;
    private boolean cartaFirmadaSubida;
    private String dictamenEstado;       // null | POR_ELABORAR | ELABORADO | FIRMADO | OBSERVADO
    private boolean dictamenEmitido;     // true cuando la secretaría subió el firmado
    private String dictamenNumero;
    private java.time.LocalDate dictamenFechaEmision;
    private String dictamenMotivoObservacion;
}
