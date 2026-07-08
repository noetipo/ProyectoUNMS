package unmsm.edu.pe.security.domain.entities;

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
 * Asignación de roles a usuarios (RBAC).
 * Enlaza {@link User} ↔ {@link Role} directamente.
 */
@Entity
@Table(
        name = "user_roles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_roles_user_role",
                columnNames = {"user_id", "role_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserRoleAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    @Builder.Default
    private Boolean assigned = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Role role;

    private LocalDateTime deletedAt;

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
}