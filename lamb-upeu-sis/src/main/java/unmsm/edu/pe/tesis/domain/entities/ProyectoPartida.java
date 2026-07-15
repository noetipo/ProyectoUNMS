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

import java.math.BigDecimal;
import java.util.UUID;

/** Partida del presupuesto por rubro del proyecto. El TOTAL S/ se deriva (suma). */
@Entity
@Table(name = "proyecto_presupuesto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoPartida extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    /** Personal de apoyo | Insumos de laboratorio | Equipos | Servicios | Movilidad | Publicación | Otros */
    @Column(length = 60)
    private String rubro;

    @Column(length = 300)
    private String descripcion;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}
