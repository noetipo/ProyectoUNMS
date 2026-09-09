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

/**
 * Dictamen de aprobación del proyecto de tesis (Etapa 5), posterior a la defensa.
 * Vive aparte del de designación de asesor —cada tesis tiene uno de cada uno— y comparte
 * con aquel la numeración correlativa de la Unidad de Posgrado.
 */
@Entity
@Table(name = "dictamenes_aprobacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DictamenAprobacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID tesisId;

    /** Correlativo formateado, ej. 001985-2026-UPG-VDIP-FM/UNMSM */
    @Column(length = 60)
    private String numero;

    private Integer anio;

    private Integer correlativo;

    @Column(length = 60)
    private String expediente;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    /** El proyecto aprobado tiene vigencia de 4 años para ejecutarse y sustentarse. */
    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoDictamen estado = EstadoDictamen.POR_ELABORAR;

    /** Snapshot (JSON) de los marcadores del dictamen, al elaborarlo. */
    @Column(name = "datos_dictamen", columnDefinition = "text")
    private String datosDictamen;
}
