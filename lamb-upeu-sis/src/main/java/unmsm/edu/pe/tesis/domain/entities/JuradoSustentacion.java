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

import java.util.UUID;

/**
 * Miembro del Jurado de Sustentación (Etapa 8), designado por el Coordinador. Son 3 docentes;
 * califican y suscriben en conjunto UN acta (no hay evaluación individual como en el Jurado
 * Informante), así que aquí solo se registra quiénes son y quién preside.
 */
@Entity
@Table(name = "proyecto_jurado_sustentacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JuradoSustentacion extends AuditableEntity {

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
}
