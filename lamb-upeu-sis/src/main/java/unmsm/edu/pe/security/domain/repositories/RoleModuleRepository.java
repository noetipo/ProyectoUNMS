package unmsm.edu.pe.security.domain.repositories;

import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.RoleModule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleModuleRepository {
    List<RoleModule> getAllRoleModules();
    Optional<RoleModule> getRoleModuleById(UUID id);
    RoleModule save(RoleModule roleModule);
    void remove(RoleModule roleModule);
    List<RoleModule> findByModuleInAndRole(Collection<Module> modules, Role role);
    long count();
}