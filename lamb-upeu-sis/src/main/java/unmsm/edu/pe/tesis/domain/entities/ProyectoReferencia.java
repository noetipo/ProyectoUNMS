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
import unmsm.edu.pe.tesis.domain.enums.TipoReferencia;

import java.util.UUID;

/**
 * Referencia bibliográfica estructurada del proyecto (repetible, "+ Agregar referencia").
 * Se formatea al vuelo al estilo elegido (APA / Vancouver / IEEE) para el documento final.
 */
@Entity
@Table(name = "proyecto_referencias")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoReferencia extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TipoReferencia tipo = TipoReferencia.ARTICULO;

    /** Autores separados por ';', en formato "Apellido, II" (ej. "García, JM; Pérez, M"). */
    @Column(length = 600)
    private String autores;

    @Column(length = 10)
    private String anio;

    @Column(length = 600)
    private String titulo;

    /** Revista / editorial / sitio, según el tipo. */
    @Column(length = 400)
    private String fuente;

    @Column(length = 40)
    private String volumen;

    @Column(length = 40)
    private String numero;

    @Column(length = 40)
    private String paginas;

    @Column(length = 200)
    private String editorial;

    @Column(length = 120)
    private String ciudad;

    @Column(length = 200)
    private String doi;

    @Column(length = 400)
    private String url;

    /** Fecha de acceso (recursos web), texto libre ej. "12 jul 2026". */
    @Column(name = "fecha_acceso", length = 40)
    private String fechaAcceso;
}
