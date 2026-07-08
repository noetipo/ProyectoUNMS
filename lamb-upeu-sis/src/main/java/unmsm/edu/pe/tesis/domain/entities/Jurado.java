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
 * Participación de un docente como jurado de una tesis. Pertenece al módulo de
 * tesis (futuro); aquí se define el mínimo necesario para CONTAR la carga.
 * {@code docente_id} referencia a {@code docentes.persona_id}.
 */
@Entity
@Table(name = "jurados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Jurado extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", columnDefinition = "uuid")
    private UUID tesisId;

    @Column(name = "docente_id", nullable = false, columnDefinition = "uuid")
    private UUID docenteId;

    @Column(length = 30)
    private String cargo; // PRESIDENTE | SECRETARIO | VOCAL
}
