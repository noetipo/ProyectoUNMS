package unmsm.edu.pe.tesis.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;
import unmsm.edu.pe.tesis.domain.enums.EnfoqueInvestigacion;
import unmsm.edu.pe.tesis.domain.enums.EstadoProyecto;
import unmsm.edu.pe.tesis.domain.enums.EstiloCita;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cabecera del proyecto de tesis en línea (Etapa 4). Uno por tesis. Los campos de
 * texto del editor viven en {@link ProyectoCampo} (clave-valor); Título y Resumen
 * escriben en {@code tesis.titulo}/{@code tesis.resumen}. Los hitos de los 5 pasos
 * de la etapa se llevan con flags booleanos.
 */
@Entity
@Table(name = "proyectos_tesis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoTesis extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID tesisId;

    /** Asesor designado (denormalizado desde asesorias) para la bandeja del asesor. */
    @Column(name = "asesor_id", columnDefinition = "uuid")
    private UUID asesorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnfoqueInvestigacion enfoque = EnfoqueInvestigacion.CUANTITATIVO;

    /** Al elegir un enfoque queda bloqueado (no se puede cambiar hasta desbloquear). */
    @Column(name = "enfoque_bloqueado", nullable = false)
    @Builder.Default
    private Boolean enfoqueBloqueado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoProyecto estado = EstadoProyecto.EN_ELABORACION;

    @Column(length = 60)
    @Builder.Default
    private String financiamiento = "Autofinanciado";

    /** Estilo de cita de las referencias (APA / Vancouver / IEEE / Harvard / MLA / Chicago). */
    @Enumerated(EnumType.STRING)
    @Column(name = "estilo_cita", length = 20)
    @Builder.Default
    private EstiloCita estiloCita = EstiloCita.APA;

    /** Al fijar el estilo queda bloqueado (un documento usa un solo estilo). Nullable para agregado seguro. */
    @Column(name = "estilo_cita_bloqueado")
    @Builder.Default
    private Boolean estiloCitaBloqueado = false;

    // ── Paso 1: listo para revisión ──
    @Column(name = "listo_revision", nullable = false)
    @Builder.Default
    private Boolean listoRevision = false;

    @Column(name = "fecha_listo_revision")
    private LocalDate fechaListoRevision;

    // ── Paso 2: plan de actividades publicado ──
    @Column(name = "plan_publicado", nullable = false)
    @Builder.Default
    private Boolean planPublicado = false;

    // ── Paso 3: carta de opinión favorable del asesor ──
    @Column(name = "carta_asesor", nullable = false)
    @Builder.Default
    private Boolean cartaAsesor = false;

    @Column(name = "fecha_carta_asesor")
    private LocalDate fechaCartaAsesor;

    // ── Paso 4: Turnitin ──
    @Column(name = "turnitin_subido", nullable = false)
    @Builder.Default
    private Boolean turnitinSubido = false;

    /** Índice de similitud (%). Máximo referencial: 20%. */
    @Column(name = "porcentaje_similitud")
    private Integer porcentajeSimilitud;

    // ── Paso 5: expediente / solicitud de aprobación ──
    @Column(name = "expediente_subido", nullable = false)
    @Builder.Default
    private Boolean expedienteSubido = false;

    @Column(name = "fecha_solicitud_aprobacion")
    private LocalDate fechaSolicitudAprobacion;

    // ── Etapa 5 · Defensa (paso 1: la Secretaría recibe y comunica al Coordinador) ──
    /** La Secretaría recibió el expediente y comunicó al Coordinador. (nullable para agregado seguro) */
    @Column(name = "expediente_recibido")
    private Boolean expedienteRecibido;

    @Column(name = "fecha_recepcion")
    private LocalDate fechaRecepcion;

    /**
     * La Secretaría habilitó la evaluación con la rúbrica oficial. Antes esto se deducía de que
     * existiera un Word subido por proyecto; ahora la rúbrica vive una sola vez en el sistema
     * ({@code plantillas_rubrica}) y aquí solo se enciende el interruptor.
     */
    @Column(name = "rubrica_habilitada")
    private Boolean rubricaHabilitada;

    /** Versión de la rúbrica que se le aplicó (se congela: no cambia al publicarse una nueva). */
    @Column(name = "rubrica_version", length = 20)
    private String rubricaVersion;

    /** Ambos revisores dieron conformidad → proyecto listo para la defensa (Etapa 5, paso 4). */
    @Column(name = "revisores_conformes")
    private Boolean revisoresConformes;

    @Column(name = "fecha_revisores_conformes")
    private LocalDate fechaRevisoresConformes;

    // ── Etapa 5 · paso 5: defensa oral programada ──
    @Column(name = "defensa_programada")
    private Boolean defensaProgramada;

    @Column(name = "fecha_defensa")
    private LocalDate fechaDefensa;

    @Column(name = "hora_defensa", length = 10)
    private String horaDefensa;

    @Column(name = "lugar_defensa", length = 200)
    private String lugarDefensa;

    @Column(name = "dictamen_numero", length = 60)
    private String dictamenNumero;

    // ── Etapa 6 · ejecución de la tesis ──
    /** El asesor aprobó el informe final (carta) con el plan 100% ejecutado. */
    @Column(name = "informe_final_aprobado")
    private Boolean informeFinalAprobado;

    @Column(name = "fecha_informe_final")
    private LocalDate fechaInformeFinal;

    // ── Etapa 7 · Jurado Informante del informe final ──
    /** El estudiante solicitó el Jurado Informante (expediente completo). */
    @Column(name = "jurado_informante_solicitado")
    private Boolean juradoInformanteSolicitado;

    @Column(name = "fecha_jurado_informante")
    private LocalDate fechaJuradoInformante;

    /** Los 3 miembros del Jurado Informante aprobaron el informe final. */
    @Column(name = "informe_final_revisado")
    private Boolean informeFinalRevisado;

    @Column(name = "fecha_informe_revisado")
    private LocalDate fechaInformeRevisado;
}
