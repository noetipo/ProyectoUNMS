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
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;

import java.util.UUID;

/**
 * Estado de revisión de un campo/ítem del proyecto (uno por campo revisable).
 * Su historial de eventos (observación → edición → corrección → conformidad) vive
 * en {@link ProyectoRevisionEvento}.
 */
@Entity
@Table(name = "proyecto_revisiones",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_proyecto_revision_campo",
                columnNames = {"proyecto_id", "campo"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoRevision extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    /** Clave del campo revisado (situacion, objGeneral, hipotesis, …). */
    @Column(nullable = false, length = 40)
    private String campo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoItemRevision estado = EstadoItemRevision.SIN_REVISION;
}
