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
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Revisor del proyecto designado por el Coordinador (Etapa 5). Normalmente 2 por proyecto.
 * Guarda la designación y, más adelante, su evaluación con rúbrica.
 */
@Entity
@Table(name = "proyecto_revisores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoRevisor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    /** Docente designado como revisor (persona_id del docente). */
    @Column(name = "docente_id", nullable = false, columnDefinition = "uuid")
    private UUID docenteId;

    /** Orden del revisor (1, 2). */
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRevisor estado = EstadoRevisor.DESIGNADO;

    @Column(length = 2000)
    private String comentario;

    /** Puntaje total de la rúbrica (suma de criterios). */
    @Column(name = "puntaje_total")
    private Integer puntajeTotal;

    /** Levantamiento del estudiante a las observaciones de este revisor. */
    @Column(name = "respuesta_estudiante", length = 2000)
    private String respuestaEstudiante;

    @Column(name = "fecha_respuesta")
    private LocalDate fechaRespuesta;

    @Column(name = "fecha_conformidad")
    private LocalDate fechaConformidad;
}
