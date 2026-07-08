package unmsm.edu.pe.security.domain.repositories;

import io.quarkus.panache.common.Page;
import unmsm.edu.pe.security.domain.entities.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    List<Role> getAllRoles();
    Optional<Role> getRoleById(UUID id);
    Optional<Role> findByCode(String code);
    Optional<Role> findFirstByCodeOrderByCreatedAtAsc(String code);
    Role save(Role role);
    Optional<Role> update(UUID id, Role role);
    void remove(Role role);
    long count();

    // Métodos para paginación y filtrado
    List<Role> findWithFilters(String nameFilter, Page page);
    long countWithFilters(String nameFilter);
}