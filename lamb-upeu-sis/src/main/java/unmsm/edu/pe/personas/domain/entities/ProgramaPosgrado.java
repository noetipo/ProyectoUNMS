package unmsm.edu.pe.personas.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.configuracion.domain.entities.Facultad;
import unmsm.edu.pe.personas.domain.enums.NivelPrograma;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.util.UUID;

@Entity
@Table(name = "programas_posgrado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProgramaPosgrado extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelPrograma nivel;

    /** Facultad a la que pertenece el programa (para la cascada Facultad → Programa). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id")
    private Facultad facultad;
}
