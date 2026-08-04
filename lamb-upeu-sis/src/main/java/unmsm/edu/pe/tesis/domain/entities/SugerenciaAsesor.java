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
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;

import java.util.UUID;

/**
 * Asesor sugerido por el tutor a su tutorando. El estudiante ve esta terna y elige
 * a quién solicitar asesoría. {@code estudiante_id}/{@code tutor_id}/{@code asesor_docente_id}
 * referencian a {@code estudiantes.persona_id} y {@code docentes.persona_id}.
 */
@Entity
@Table(name = "sugerencias_asesor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SugerenciaAsesor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "estudiante_id", nullable = false, columnDefinition = "uuid")
    private UUID estudianteId;

    @Column(name = "tutor_id", nullable = false, columnDefinition = "uuid")
    private UUID tutorId;

    @Column(name = "asesor_docente_id", nullable = false, columnDefinition = "uuid")
    private UUID asesorDocenteId;

    /**
     * Para qué puesto se sugiere: la tesis tiene un solo ASESOR y, opcionalmente, un solo
     * COASESOR. Filas antiguas quedan en null y se leen como ASESOR.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TipoAsesoria tipo = TipoAsesoria.ASESOR;

    @Column(length = 300)
    private String nota;

    /** Tipo efectivo (null histórico = ASESOR). */
    public TipoAsesoria tipoEfectivo() {
        return tipo != null ? tipo : TipoAsesoria.ASESOR;
    }
}
