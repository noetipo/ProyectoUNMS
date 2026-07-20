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
import unmsm.edu.pe.tesis.domain.enums.RolJurado;

import java.util.UUID;

/** Miembro del Jurado Examinador de la defensa del proyecto (Etapa 5, paso 5). */
@Entity
@Table(name = "proyecto_jurado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoJurado extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    @Column(name = "docente_id", nullable = false, columnDefinition = "uuid")
    private UUID docenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_jurado", nullable = false, length = 20)
    private RolJurado rolJurado;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}
