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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento del hilo de revisión de un ítem (el {@code hist[]} del prototipo):
 * OBSERVACIÓN (asesor) → EDICIÓN/CORRECCIÓN (estudiante) → CONFORMIDAD (asesor).
 */
@Entity
@Table(name = "proyecto_revision_eventos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoRevisionEvento extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "revision_id", nullable = false, columnDefinition = "uuid")
    private UUID revisionId;

    /** OBSERVACIÓN | EDICIÓN | CORRECCIÓN | CONFORMIDAD */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** Nombre visible del autor del evento. */
    @Column(length = 200)
    private String autor;

    /** ESTUDIANTE | ASESOR */
    @Column(length = 20)
    private String rol;

    @Column(columnDefinition = "text")
    private String texto;

    @Column(name = "fecha_evento")
    private LocalDateTime fechaEvento;
}
