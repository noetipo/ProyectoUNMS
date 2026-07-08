package unmsm.edu.pe.security.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
public class RefreshToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // ✅ Cambio aquí
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 1000)
    private String token; // No normalizar - es un JWT

    //@Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //@Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}