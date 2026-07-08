package unmsm.edu.pe.personas.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.shared.annotations.Normalize;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.util.UUID;

/** Catálogo de líneas de investigación (especialidad del docente). */
@Entity
@Table(name = "lineas_investigacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LineaInvestigacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, unique = true, length = 200)
    @Normalize(Normalize.NormalizeType.TITLE_CASE)
    private String nombre;

    @Column(length = 500)
    private String descripcion;
}
