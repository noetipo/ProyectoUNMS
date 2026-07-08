package unmsm.edu.pe.security.domain.repositories;

import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;

import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentRepository {

    UserRoleAssignment save(UserRoleAssignment userRole);

    List<UserRoleAssignment> saveAll(List<UserRoleAssignment> userRoles);

    void remove(UserRoleAssignment userRole);

    long count();

    /** Asignaciones activas (no eliminadas) de un usuario. */
    List<UserRoleAssignment> findByUser(User user);

    /** Asignaciones activas (no eliminadas) de un usuario por id. */
    List<UserRoleAssignment> findByUserId(UUID userId);

    /** Nombres de los roles activos asignados a un usuario (para mostrar). */
    List<String> findRoleNamesByUserId(UUID userId);

    /** Códigos de los roles activos asignados a un usuario (para autorización/JWT). */
    List<String> findRoleCodesByUserId(UUID userId);

    /**
     * Todos los roles disponibles marcando cuáles están asignados al usuario.
     * Cada fila: id, created_at, deletedat, description, name, status, updated_at, code, selected.
     */
    List<Object[]> findAllRolesSelectedByUserId(UUID userId);
}