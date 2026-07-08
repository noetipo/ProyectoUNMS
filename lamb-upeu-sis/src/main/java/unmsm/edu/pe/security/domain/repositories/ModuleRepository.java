package unmsm.edu.pe.security.domain.repositories;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.entities.ParentModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ Extiende PanacheRepositoryBase para heredar métodos como findByIdOptional
 */
public interface ModuleRepository extends PanacheRepositoryBase<Module, UUID> {

    /**
     * Obtener todos los módulos activos
     */
    List<Module> getAllModules();

    /**
     * Buscar módulo por ID (custom)
     */
    Optional<Module> getModuleById(UUID id);

    /**
     * Buscar módulos por ID de módulo padre
     */
    List<Module> findByParentModuleId(UUID parentModuleId);

    /**
     * Buscar módulos por entidad ParentModule
     */
    List<Module> findByParentModule(ParentModule parentModule);

    /**
     * Buscar módulo por código
     */
    Optional<Module> findByCode(String code);

    /**
     * Guardar módulo
     */
    Module save(Module module);

    /**
     * Actualizar módulo
     */
    Optional<Module> update(UUID id, Module module);

    /**
     * Eliminar (soft delete) módulo
     */
    void remove(Module module);

    /**
     * Buscar con filtros y retornar PanacheQuery para paginación
     */
    PanacheQuery<Module> findWithFilters(String nameFilter, UUID parentModuleId);

    /**
     * Contar con filtros
     */
    long countWithFilters(String nameFilter);

    /**
     * Queries nativas para funcionalidades específicas
     */
    List<Object[]> findAllModulesSelected(UUID roleId, UUID parentModuleId);

    List<Object[]> findAllMenuParent(UUID userId);

    List<Object[]> findAllMenu(UUID userId, UUID parentModuleId);

    /**
     * Menú resuelto por CÓDIGOS de rol (los que trae el JWT), no por userId.
     * Independiente del id del usuario, así una sesión sobrevive a recreaciones de BD.
     */
    List<Object[]> findAllMenuParentByRoleCodes(List<String> roleCodes);

    List<Object[]> findAllMenuByRoleCodes(List<String> roleCodes, UUID parentModuleId);

    List<Object[]> findAllModulesAssignedToRole(UUID roleId, UUID parentModuleId);

    List<Object[]> findAllMenuParentByRole(UUID roleId);

    List<Object[]> findAllMenuByRole(UUID roleId, UUID parentModuleId);
}