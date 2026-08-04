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

    // ── Tutor asignado (tutoría vigente); null si aún no le asignaron tutor ──
    private UUID tutorId;
    private String tutorNombre;
    private String tutorGrado;

    // ── Asesores sugeridos por el tutor ──
    private List<AsesorSugeridoItem> sugeridos;

    /** Designación vigente: una tesis tiene un solo asesor y, opcionalmente, un solo co-asesor. */
    private String asesorNombre;
    private String coasesorNombre;
    /** Ids de los designados: con ellos la UI descarta de "sugeridos" a quien ya tiene el puesto. */
    private UUID asesorDocenteId;
    private UUID coasesorDocenteId;
    /** true si ya hay asesor pero aún no co-asesor (la siguiente solicitud sería de co-asesoría). */
    private boolean puedeSolicitarCoasesor;

    // ── Solicitud del ASESOR (la principal): manda para los documentos y para el avance del proceso ──
    private UUID solicitudId;
    private String solicitudEstado; // SIN_SOLICITUD | PENDIENTE | ACEPTADA | RECHAZADA | CANCELADA
    private UUID docenteSolicitadoId;
    private String docenteSolicitadoNombre;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private String motivoRespuesta;

    // ── Solicitud de CO-ASESORÍA (opcional): se informa aparte para que no bloquee al asesor ──
    private UUID coasesorSolicitudId;
    private String coasesorSolicitudEstado; // null si nunca solicitó co-asesor
    private String coasesorSolicitadoNombre;
    private String coasesorMotivoRespuesta;

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
