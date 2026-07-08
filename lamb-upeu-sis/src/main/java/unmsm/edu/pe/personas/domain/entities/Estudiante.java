package unmsm.edu.pe.personas.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.CondicionEstudiante;
import unmsm.edu.pe.personas.domain.enums.Financiamiento;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.util.UUID;

/**
 * Perfil estudiante de una {@link Persona}. PK compartida con persona
 * (persona_id es a la vez PK y FK, vía {@code @MapsId}).
 */
@Entity
@Table(name = "estudiantes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Estudiante extends AuditableEntity {

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

    @Column(name = "cod_matricula", unique = true, length = 30)
    private String codMatricula;

    @Column(name = "email_institucional", unique = true, length = 120)
    private String emailInstitucional;

    @Column(name = "anio_ingreso")
    private Integer anioIngreso;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CondicionEstudiante condicion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Financiamiento financiamiento;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProgramaPosgrado programa;
}
