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
 * Registro de qué notificación derivada (ver {@code NotificacionServiceImpl}) ya se envió por
 * correo a qué persona, para no reenviarla en cada ciclo del scheduler. La "huella" es la misma
 * idea que usa el frontend para marcar una notificación como vista (id + descripción): si el texto
 * cambia (p. ej. avanzó de etapa), se considera una notificación nueva y se vuelve a enviar.
 */
@Entity
@Table(name = "correo_notificacion_enviado",
        uniqueConstraints = @UniqueConstraint(columnNames = {"persona_id", "huella"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CorreoNotificacionEnviado extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "persona_id", nullable = false, columnDefinition = "uuid")
    private UUID personaId;

    @Column(nullable = false, length = 300)
    private String huella;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;
}
