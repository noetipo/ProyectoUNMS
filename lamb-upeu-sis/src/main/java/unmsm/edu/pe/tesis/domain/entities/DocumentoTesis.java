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
 * Archivo subido asociado a una tesis (documentos firmados y dictamen firmado).
 * Único por (tesis, tipo): subir de nuevo REEMPLAZA. Los documentos GENERADOS por el
 * sistema (Solicitud/Carta/Dictamen) NO se guardan aquí (se renderizan al vuelo desde snapshot).
 */
@Entity
@Table(name = "documentos_tesis",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_documento_tesis_tipo",
                columnNames = {"tesis_id", "tipo"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentoTesis extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tesis_id", nullable = false, columnDefinition = "uuid")
    private UUID tesisId;

    /** SOLICITUD_ASESORIA_FIRMADA | CARTA_ACEPTACION_FIRMADA | DICTAMEN_DESIGNACION_FIRMADO */
    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    /** Persona que subió el archivo (estudiante o secretaría). */
    @Column(name = "subido_por", columnDefinition = "uuid")
    private UUID subidoPor;
}
