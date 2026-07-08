package unmsm.edu.pe.personas.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.util.UUID;

/**
 * Grado académico de una {@link Persona} (lista): tipo de grado, año de obtención,
 * universidad y bandera principal. Solo uno puede ser principal por persona
 * (índice único parcial {@code uq_grado_principal_por_persona}).
 * Réplica del patrón de {@code PersonaCargo}/{@code DocenteLineaInvestigacion}.
 */
@Entity
@Table(name = "persona_grados_academicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PersonaGradoAcademico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Persona persona;

    @Enumerated(EnumType.STRING)
    @Column(name = "grado", nullable = false, length = 30)
    private GradoAcademico grado;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "universidad", length = 200)
    private String universidad;

    @Column(name = "principal", nullable = false)
    @Builder.Default
    private Boolean principal = false;
}
