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
 * Vínculo estudiante ↔ tesis. {@code es_activa = true} marca la tesis vigente del
 * estudiante (un estudiante no puede tener dos tesis activas a la vez).
 * {@code estudiante_id} referencia a {@code estudiantes.persona_id} y
 * {@code tesis_id} a {@code tesis.id}.
 */
@Entity
@Table(name = "tesis_autores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TesisAutor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", nullable = false, columnDefinition = "uuid")
    private UUID tesisId;

    @Column(name = "estudiante_id", nullable = false, columnDefinition = "uuid")
    private UUID estudianteId;

    @Column(name = "es_activa", nullable = false)
    @Builder.Default
    private Boolean esActiva = true;
}
