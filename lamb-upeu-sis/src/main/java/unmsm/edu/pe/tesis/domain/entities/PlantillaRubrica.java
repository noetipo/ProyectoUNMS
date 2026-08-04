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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rúbrica oficial de los revisores, una por enfoque (CUANTITATIVO y CUALITATIVO) y versionada
 * por año.
 *
 * <p>Antes la Secretaría adjuntaba el mismo Word en cada proyecto; ahora el documento vive una
 * sola vez en el sistema y en la bandeja solo se <b>habilita</b> la evaluación. Al publicar una
 * versión nueva la anterior deja de ser vigente pero <b>se conserva</b>: los proyectos que ya
 * estaban siendo evaluados siguen con la que se les aplicó (no se cambian las reglas a mitad de
 * partido), y queda historial.</p>
 */
@Entity
@Table(name = "plantillas_rubrica")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PlantillaRubrica extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** CUANTITATIVO (cuantitativo y mixto) | CUALITATIVO */
    @Column(nullable = false, length = 20)
    private String enfoque;

    /** Año o etiqueta de la versión: "2026". */
    @Column(nullable = false, length = 20)
    private String version;

    /** Solo una vigente por enfoque; las demás quedan como historial. */
    @Column(nullable = false)
    private Boolean vigente;

    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    /**
     * El Word, dentro de la propia base ({@code bytea}).
     *
     * <p>A diferencia de los documentos del expediente —que van al almacenamiento de archivos—,
     * aquí son dos ficheros institucionales de ~145 KB que casi nunca cambian: guardarlos en la
     * base hace que un respaldo se los lleve consigo y que el sistema arranque completo en
     * cualquier entorno, sin depender de una carpeta de uploads.</p>
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "contenido")
    private byte[] contenido;

    /** Legado: ruta en el almacenamiento de archivos (las primeras versiones se guardaron ahí). */
    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    /** Persona que publicó la versión. */
    @Column(name = "subido_por", columnDefinition = "uuid")
    private UUID subidoPor;
}
