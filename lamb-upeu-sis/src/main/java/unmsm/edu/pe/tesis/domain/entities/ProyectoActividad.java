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
import unmsm.edu.pe.tesis.domain.enums.EstadoActividad;
import unmsm.edu.pe.tesis.domain.enums.FaseActividad;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Actividad del cronograma (Gantt de 18 meses) del proyecto. Es a la vez el
 * "plan de actividades" del paso 2 de la Etapa 4.
 */
@Entity
@Table(name = "proyecto_actividades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoActividad extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    @Column(nullable = false, length = 300)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FaseActividad fase;

    /** Mes de inicio (1..18) — derivado/legado; el cronograma se posiciona por fecha. */
    @Column(name = "mes_inicio")
    private Integer mesInicio;

    /** Mes de fin (1..18) — derivado/legado; el cronograma se posiciona por fecha. */
    @Column(name = "mes_fin")
    private Integer mesFin;

    /** Fecha de inicio de la actividad (fuente de verdad del cronograma). */
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    /** Fecha de fin de la actividad. */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoActividad estado = EstadoActividad.PENDIENTE;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}
