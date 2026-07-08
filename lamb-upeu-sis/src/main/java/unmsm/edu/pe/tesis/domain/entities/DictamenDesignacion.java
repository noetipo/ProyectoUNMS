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
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;

import java.time.LocalDate;
import java.util.UUID;

/** Dictamen de designación de asesor de una tesis (uno por tesis). */
@Entity
@Table(name = "dictamenes_designacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DictamenDesignacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID tesisId;

    /** Correlativo formateado, ej. 001984-2026-UPG-VDIP-FM/UNMSM */
    @Column(length = 60)
    private String numero;

    private Integer anio;

    private Integer correlativo;

    @Column(length = 60)
    private String expediente;

    @Column(name = "fecha_solicitud")
    private LocalDate fechaSolicitud;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoDictamen estado = EstadoDictamen.POR_ELABORAR;

    /** Snapshot (JSON) de los marcadores del dictamen, al elaborarlo. */
    @Column(name = "datos_dictamen", columnDefinition = "text")
    private String datosDictamen;

    @Column(name = "motivo_observacion", length = 500)
    private String motivoObservacion;
}
