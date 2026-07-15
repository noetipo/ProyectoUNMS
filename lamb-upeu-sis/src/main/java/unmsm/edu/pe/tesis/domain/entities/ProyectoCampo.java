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

import java.util.UUID;

/**
 * Un campo de texto del editor del proyecto (almacenamiento clave-valor). Único por
 * (proyecto, clave). {@code clave} es la clave del prototipo (situacion, formulacion,
 * objGeneral, hipotesis, …). Título y Resumen NO se guardan aquí (van a {@code tesis}).
 * El autoguardado por campo hace un upsert de una sola fila.
 */
@Entity
@Table(name = "proyecto_campos",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_proyecto_campo",
                columnNames = {"proyecto_id", "clave"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProyectoCampo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "proyecto_id", nullable = false, columnDefinition = "uuid")
    private UUID proyectoId;

    @Column(nullable = false, length = 40)
    private String clave;

    @Column(columnDefinition = "text")
    private String valor;
}
