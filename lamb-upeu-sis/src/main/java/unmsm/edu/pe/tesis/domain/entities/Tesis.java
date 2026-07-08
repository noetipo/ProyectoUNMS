package unmsm.edu.pe.tesis.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.enums.NivelPrograma;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tesis de un estudiante. Se crea al registrar el tema (coordinador) y se vincula
 * al estudiante vía {@link TesisAutor} ({@code es_activa = true}).
 * El {@code nivel} se deriva del programa del estudiante y {@code fechaRegistro}/
 * {@code createdBy} los pone el servidor, nunca el cliente.
 */
@Entity
@Table(name = "tesis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tesis extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(length = 2000)
    private String resumen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_investigacion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private LineaInvestigacion lineaInvestigacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NivelPrograma nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoTesis estado = EstadoTesis.TEMA_REGISTRADO;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;
}
