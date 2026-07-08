package unmsm.edu.pe.security.domain.repositories;

import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    List<User> getAllUsers();
    List<User> findAllByStatus(UserStatus status);
    Optional<User> getUserById(UUID id); // ✅ Cambio de Long a UUID
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User saveUser(User user);
    void removeUserById(UUID id); // ✅ Cambio de Long a UUID
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByStatus(UserStatus status);

    /**
     * Reporte paginado de usuarios con JOIN a persona y filtros.
     * Cada fila: [id, username, email, first_name, last_name, status, active,
     * persona_id, apellido_paterno, apellido_materno, nombres, numero_documento].
     * @param roleCodes filtro multiselección por rol (vacío/null = todos)
     * @param statuses  filtro multiselección por estado (vacío/null = todos)
     */
    List<Object[]> listarReporte(String search, List<String> roleCodes, List<UserStatus> statuses, int page, int size);

    long contarReporte(String search, List<String> roleCodes, List<UserStatus> statuses);

    /** Roles (código) asignados y activos de un conjunto de usuarios. Cada fila: [user_id, role_code]. */
    List<Object[]> rolesDeUsuarios(List<UUID> userIds);
}