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
 * Rúbrica de la defensa oral que cada revisor entrega firmada tras el acto (Etapa 5).
 * Una por revisor y tesis: guarda la nota y el archivo que recepciona la Secretaría.
 */
@Entity
@Table(name = "rubricas_defensa",
        uniqueConstraints = @UniqueConstraint(name = "uk_rubrica_defensa", columnNames = {"tesis_id", "docente_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RubricaDefensa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", nullable = false, columnDefinition = "uuid")
    private UUID tesisId;

    /** Revisor que firma la rúbrica (persona_id del docente). */
    @Column(name = "docente_id", nullable = false, columnDefinition = "uuid")
    private UUID docenteId;

    /** Nota sobre 100 que consigna el revisor. */
    private Integer puntaje;

    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    @Column(name = "subido_por", columnDefinition = "uuid")
    private UUID subidoPor;
}
