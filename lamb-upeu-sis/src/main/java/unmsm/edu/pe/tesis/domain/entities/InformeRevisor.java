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
 * Miembro del Jurado Informante que evalúa el informe final de la tesis (Etapa 7).
 * Son 3 docentes con grado de Doctor; el primero (orden 1) es el Presidente.
 */
@Entity
@Table(name = "proyecto_informe_revisores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InformeRevisor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    @Column(name = "docente_id", nullable = false, columnDefinition = "uuid")
    private UUID docenteId;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    /** El primero designado (orden 1) actúa como Presidente del Jurado. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean presidente = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRevisor estado = EstadoRevisor.DESIGNADO;

    /** Puntaje de la rúbrica del informe (0..20). */
    @Column(name = "puntaje")
    private Integer puntaje;

    @Column(length = 2000)
    private String comentario;

    @Column(name = "respuesta_estudiante", length = 2000)
    private String respuestaEstudiante;

    @Column(name = "fecha_conformidad")
    private LocalDate fechaConformidad;
}
