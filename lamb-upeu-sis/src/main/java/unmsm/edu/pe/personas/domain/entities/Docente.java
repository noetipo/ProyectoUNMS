package unmsm.edu.pe.personas.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.CategoriaDocente;
import unmsm.edu.pe.personas.domain.enums.CondicionDocente;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.util.UUID;

/**
 * Perfil docente de una {@link Persona}. PK compartida con persona
 * (persona_id es a la vez PK y FK, vía {@code @MapsId}).
 */
@Entity
@Table(name = "docentes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Docente extends AuditableEntity {

    @Id
    @Column(name = "persona_id", columnDefinition = "uuid")
    private UUID personaId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "persona_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Persona persona;

    @Column(name = "codigo_sistema", unique = true, length = 20)
    private String codigoSistema;

    @Column(name = "email_institucional", unique = true, length = 120)
    private String emailInstitucional;

    // El grado académico ahora vive en persona_grados_academicos (lista a nivel de persona);
    // el "grado del docente" es el grado marcado como principal.

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoriaDocente categoria;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CondicionDocente condicion;

    /** Cupo máximo de estudiantes en tutoría vigente. Null = sin límite configurado (usa default global). */
    @Column(name = "cupo_maximo_tutoria")
    private Integer cupoMaximoTutoria;
}
