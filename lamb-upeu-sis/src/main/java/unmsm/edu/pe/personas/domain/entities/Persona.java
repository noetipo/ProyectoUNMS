package unmsm.edu.pe.personas.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.EstadoCivil;
import unmsm.edu.pe.personas.domain.enums.Procedencia;
import unmsm.edu.pe.personas.domain.enums.Sexo;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.shared.annotations.Normalize;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Persona: base del modelo. Los perfiles {@link Estudiante} y {@link Docente}
 * se le agregan 1:1 (PK compartida). La cuenta de acceso vive en {@link User}
 * y se enlaza mediante {@code user_id}.
 */
@Entity
@Table(name = "persona")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Persona extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 30)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", unique = true, nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "apellido_paterno", nullable = false, length = 100)
    @Normalize(Normalize.NormalizeType.TITLE_CASE)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 100)
    @Normalize(Normalize.NormalizeType.TITLE_CASE)
    private String apellidoMaterno;

    @Column(nullable = false, length = 150)
    @Normalize(Normalize.NormalizeType.TITLE_CASE)
    private String nombres;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sexo sexo;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil", length = 20)
    private EstadoCivil estadoCivil;

    @Column(length = 60)
    private String nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Procedencia procedencia;

    @Column(name = "email_personal", length = 120)
    @Normalize(Normalize.NormalizeType.LOWERCASE)
    private String emailPersonal;

    @Column(length = 20)
    private String celular;

    @Column
    private Boolean discapacidad;

    @Column(unique = true, length = 30)
    private String orcid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User user;
}
