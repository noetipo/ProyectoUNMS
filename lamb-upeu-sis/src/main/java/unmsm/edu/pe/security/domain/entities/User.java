package unmsm.edu.pe.security.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;
import unmsm.edu.pe.shared.annotations.Normalize;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // ✅ Cambio aquí
    @Column(columnDefinition = "uuid")
    private UUID id; // ✅ Cambio de Long a UUID

    @Column(nullable = false, unique = true, length = 50)
    @Normalize(Normalize.NormalizeType.LOWERCASE) // usernames en minúsculas
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    @Normalize(Normalize.NormalizeType.LOWERCASE) // emails en minúsculas
    private String email;

    @Column(nullable = false)
    private String password; // No normalizar - mantener como está

    //@Column(name = "first_name", nullable = false, length = 50)
    @Normalize(Normalize.NormalizeType.TITLE_CASE) // Primera letra mayúscula
    private String firstName;

    //@Column(name = "last_name", nullable = false, length = 50)
    @Normalize(Normalize.NormalizeType.TITLE_CASE) // Primera letra mayúscula
    private String lastName;

    @Column(length = 15)
    @Normalize(Normalize.NormalizeType.SPACES_ONLY) // Solo limpiar espacios
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    //@Column(name = "last_login")
    private LocalDateTime lastLogin;
}