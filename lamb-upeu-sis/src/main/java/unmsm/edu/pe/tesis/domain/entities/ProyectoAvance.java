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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evaluación periódica del avance de la tesis por el asesor (Etapa 6 · ejecución).
 * La rúbrica de 4 criterios se guarda en columnas (escala 1..4).
 */
@Entity
@Table(name = "proyecto_avances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoAvance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDate fechaEvaluacion;

    @Column(name = "puntaje_ejecucion")
    private Integer puntajeEjecucion;

    @Column(name = "puntaje_datos")
    private Integer puntajeDatos;

    @Column(name = "puntaje_analisis")
    private Integer puntajeAnalisis;

    @Column(name = "puntaje_interpretacion")
    private Integer puntajeInterpretacion;

    /** % del plan ejecutado al momento de la evaluación (actividades HECHA). */
    @Column(name = "porcentaje_plan")
    private Integer porcentajePlan;

    @Column(length = 2000)
    private String comentario;
}
