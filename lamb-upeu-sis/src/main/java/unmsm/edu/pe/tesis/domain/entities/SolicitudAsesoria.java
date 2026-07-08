package unmsm.edu.pe.tesis.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Carta de solicitud de asesoría del estudiante a un docente (paso previo a la
 * asesoría formal). {@code estudiante_id} y {@code docente_id} referencian a
 * {@code estudiantes.persona_id} y {@code docentes.persona_id}.
 */
@Entity
@Table(name = "solicitudes_asesoria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SolicitudAsesoria extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_investigacion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private LineaInvestigacion lineaInvestigacion;

    /** Referencia libre a la tesis (aún sin FK/tabla en el esquema activo). */
    @Column(name = "tesis_id", columnDefinition = "uuid")
    private UUID tesisId;

    @Column(name = "titulo_tentativo", length = 500)
    private String tituloTentativo;

    @Column(length = 2000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoAsesoria tipo = TipoAsesoria.ASESOR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "motivo_respuesta", length = 500)
    private String motivoRespuesta;

    /** Snapshot (JSON) de los marcadores resueltos para la Solicitud, al crearla. */
    @Column(name = "datos_solicitud", columnDefinition = "text")
    private String datosSolicitud;

    /** Snapshot (JSON) de los marcadores resueltos para la Carta, al aceptar. */
    @Column(name = "datos_carta", columnDefinition = "text")
    private String datosCarta;
}
