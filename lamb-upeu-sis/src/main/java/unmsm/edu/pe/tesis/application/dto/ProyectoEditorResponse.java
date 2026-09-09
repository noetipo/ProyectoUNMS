package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Estado completo del editor "Proyecto de tesis en línea" (Etapa 4). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProyectoEditorResponse {
    private UUID tesisId;
    private UUID proyectoId;

    // Cabecera (contexto de la tesis)
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String asesorNombre;
    private String lineaNombre;
    private String nivel;

    // Datos generales (viven en tesis)
    private String titulo;
    private String resumen;

    // Proyecto
    private String enfoque;         // CUANTITATIVO | CUALITATIVO
    private boolean enfoqueBloqueado;
    private String estado;          // EstadoProyecto
    private String financiamiento;

    // Hitos de los 5 pasos
    private boolean listoRevision;
    private boolean planPublicado;
    private boolean cartaAsesor;
    private boolean turnitinSubido;
    private boolean proyectoFinalSubido;
    private boolean expedienteSubido;
    private boolean expedienteRecibido;
    private Integer porcentajeSimilitud;

    // Avance de campos (X / total, %)
    private int avanceCompletados;
    private int avanceTotal;
    private int avancePct;

    // Contenido
    private Map<String, String> campos;
    private List<ObjetivoItem> objetivos;
    private List<ActividadItem> actividades;
    private List<PartidaItem> partidas;
    private BigDecimal presupuestoTotal;
    private List<RevisionItem> revisiones;

    // Referencias bibliográficas estructuradas + estilo de cita
    private List<ReferenciaItem> referencias;
    private String estiloCita;          // APA | VANCOUVER | IEEE | HARVARD | MLA | CHICAGO
    private boolean estiloCitaBloqueado;

    // Etapa 5 · evaluación de los revisores (Jurado Informante)
    private List<RevisorEvalItem> evaluacionesRevisores;
    private boolean revisoresConformes; // ambos revisores dieron conformidad

    // Etapa 5 · defensa programada, realizada y cierre del proyecto
    private boolean defensaProgramada;
    private java.time.LocalDate fechaDefensa;
    private String horaDefensa;
    private String lugarDefensa;
    private String modalidadDefensaLabel;
    private String enlaceDefensa;
    private String dictamenNumero;
    private boolean defensaRealizada;
    private String resultadoDefensa;       // APROBADO | APROBADO_CON_OBSERVACIONES | DESAPROBADO
    private String resultadoDefensaLabel;
    /** El dictamen de aprobación quedó firmado y el proyecto archivado: recién aquí abre la Etapa 6. */
    private boolean proyectoAprobado;

    // Etapa 6 · ejecución
    private boolean informeFinalSubido;

    /**
     * El co-asesor entra en modo consulta: ve el proyecto y el historial, pero no observa,
     * no da conformidad ni emite la carta. Solo lo marca el detalle del asesor.
     */
    private boolean soloLectura;
    private boolean informeFinalAprobado;
    private List<AvanceItem> avances;

    // Etapa 7 · Jurado Informante del informe final
    private boolean juradoInformanteSolicitado;
    private boolean informeFinalRevisado;
    private List<InformeRevisorItem> evaluacionesJuradoInforme;

    // Etapa 8 · Sustentación de la tesis (la última)
    private boolean sustentacionSolicitada;
    private boolean expedienteSustentacionRecibido;
    private List<JuradoItem> juradoSustentacion;
    private boolean sustentacionProgramada;
    private java.time.LocalDate fechaSustentacion;
    private String horaSustentacion;
    private String lugarSustentacion;
    private String modalidadSustentacionLabel;
    private String enlaceSustentacion;
    private boolean actaSustentacionSubida;
    private java.time.LocalDate fechaActaSustentacion;
    private String resultadoSustentacionLabel;
    private boolean tesisConcluida;
    private java.time.LocalDate fechaConclusionTesis;

    // Derivados para gating de la UI
    private boolean puedeMarcarListo;   // avancePct >= 100
    private boolean todosConformes;     // todos los ítems observados están CONFORME
}
