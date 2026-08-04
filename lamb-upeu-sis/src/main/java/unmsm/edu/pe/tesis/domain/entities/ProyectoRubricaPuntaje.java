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

/** Puntaje de un criterio de la rúbrica, dado por un revisor a un proyecto (Etapa 5). */
@Entity
@Table(name = "proyecto_rubrica_puntajes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoRubricaPuntaje extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** Revisor que dio el puntaje. */
    @Column(name = "revisor_id", nullable = false, columnDefinition = "uuid")
    private UUID revisorId;

    /** Clave del criterio (ver RubricaDefinicion). */
    @Column(nullable = false, length = 40)
    private String criterio;

    /** Nivel elegido: CUMPLE | PARCIAL | NO_CUMPLE. */
    @Column(length = 20)
    private String nivel;

    /** Puntaje resultante del nivel para ese criterio. */
    @Column(nullable = false)
    private Integer puntaje;

    /** Observación / sugerencia de subsanación del revisor para este criterio (opcional). */
    @Column(length = 1000)
    private String observacion;
}
