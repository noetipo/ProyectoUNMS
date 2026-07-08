package unmsm.edu.pe.security.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.entities.ParentModule;
import unmsm.edu.pe.security.domain.repositories.ModuleRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ModuleRepositoryImpl implements ModuleRepository, PanacheRepositoryBase<Module, UUID> {

    @Inject
    EntityManager entityManager;

    @Override
    public List<Module> getAllModules() {
        return list("status = true and deletedAt is null", Sort.by("moduleOrder").ascending());
    }

    @Override
    public Optional<Module> getModuleById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    @Override
    public List<Module> findByParentModuleId(UUID parentModuleId) {
        return list("parentModule.id = ?1 and deletedAt is null",
                Sort.by("moduleOrder").ascending(), parentModuleId);
    }

    @Override
    public List<Module> findByParentModule(ParentModule parentModule) {
        return list("parentModule.id = ?1 and deletedAt is null",
                Sort.by("moduleOrder").ascending(), parentModule.getId());
    }

    @Override
    public Optional<Module> findByCode(String code) {
        return find("code = ?1 and deletedAt is null", code).firstResultOptional();
    }

    @Override
    public Module save(Module module) {
        persist(module);
        return module;
    }

    @Override
    public Optional<Module> update(UUID id, Module module) {
        Optional<Module> existing = findByIdOptional(id);
        if (existing.isPresent()) {
            Module existingModule = existing.get();
            existingModule.setTitle(module.getTitle());
            existingModule.setSubtitle(module.getSubtitle());
            existingModule.setType(module.getType());
            existingModule.setCode(module.getCode());
            existingModule.setIcon(module.getIcon());
            existingModule.setStatus(module.getStatus());
            existingModule.setModuleOrder(module.getModuleOrder());
            existingModule.setLink(module.getLink());
            existingModule.setParentModule(module.getParentModule());
            persist(existingModule);
            return Optional.of(existingModule);
        }
        return Optional.empty();
    }

    @Override
    public void remove(Module module) {
        module.markAsDeleted();
        persist(module);
    }

    @Override
    public long count() {
        return count("status = true and deletedAt is null");
    }

    @Override
    public PanacheQuery<Module> findWithFilters(String nameFilter, UUID parentModuleId) {
        StringBuilder queryBuilder = new StringBuilder("status = true and deletedAt is null");

        if (nameFilter != null && !nameFilter.isEmpty()) {
            queryBuilder.append(" and LOWER(title) LIKE LOWER(?1)");
        }

        if (parentModuleId != null) {
            queryBuilder.append(nameFilter != null && !nameFilter.isEmpty() ?
                    " and parentModule.id = ?2" : " and parentModule.id = ?1");
        }

        String query = queryBuilder.toString();
        Sort sort = Sort.by("moduleOrder").ascending();

        // Construir query con parámetros
        if (nameFilter != null && !nameFilter.isEmpty() && parentModuleId != null) {
            return find(query, sort, "%" + nameFilter + "%", parentModuleId);
        } else if (nameFilter != null && !nameFilter.isEmpty()) {
            return find(query, sort, "%" + nameFilter + "%");
        } else if (parentModuleId != null) {
            return find(query, sort, parentModuleId);
        } else {
            return find(query, sort);
        }
    }

    @Override
    public long countWithFilters(String nameFilter) {
        if (nameFilter != null && !nameFilter.isEmpty()) {
            return count("status = true and deletedAt is null and LOWER(title) LIKE LOWER(?1)",
                    "%" + nameFilter + "%");
        }
        return count("status = true and deletedAt is null");
    }

    @Override
    public List<Object[]> findAllModulesSelected(UUID roleId, UUID parentModuleId) {
        // ✅ CORREGIDO: Usar nombres de columna reales de la BD
        String sql = """
            SELECT 
                m.id, 
                m.created_at, 
                m.deletedat, 
                m.icon, 
                m.moduleorder, 
                m.title, 
                m.status, 
                m.updated_at, 
                m.link, 
                m.parent_module_id, 
                CASE 
                    WHEN EXISTS ( 
                        SELECT 1 
                        FROM role_modules rm 
                        WHERE rm.module_id = m.id 
                          AND rm.role_id = :roleId 
                          AND rm.deletedat IS NULL 
                    ) THEN TRUE 
                    ELSE FALSE 
                END AS selected,
                m.subtitle, 
                m.type 
            FROM 
                modules m 
            WHERE m.parent_module_id = :parentModuleId
              AND m.deletedat IS NULL
              AND m.status = true
            ORDER BY m.moduleorder ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("roleId", roleId);
        query.setParameter("parentModuleId", parentModuleId);

        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllMenuParent(UUID userId) {
        // Menú padre según los roles asignados al usuario vía user_roles
        String sql = """
            SELECT DISTINCT
                pm.id,
                pm.created_at,
                pm.deletedat,
                pm.icon,
                pm.moduleorder,
                pm.title,
                pm.status,
                pm.updated_at,
                pm.link,
                pm.subtitle,
                pm.type
            FROM parent_modules pm
            WHERE pm.id IN (
                SELECT DISTINCT m.parent_module_id
                FROM modules m
                INNER JOIN role_modules rm ON rm.module_id = m.id
                INNER JOIN user_roles ur ON ur.role_id = rm.role_id
                WHERE ur.user_id = :userId
                  AND ur.assigned = true
                  AND rm.assigned = true
                  AND m.deletedat IS NULL
                  AND m.status = true
                  AND rm.deletedat IS NULL
                  AND ur.deletedat IS NULL
            )
            AND pm.deletedat IS NULL
            AND pm.status = true
            ORDER BY pm.moduleorder ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);

        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllMenu(UUID userId, UUID parentModuleId) {
        // Módulos hijo según los roles asignados al usuario vía user_roles
        String sql = """
            SELECT DISTINCT
                m.id,
                m.created_at,
                m.deletedat,
                m.icon,
                m.moduleorder,
                m.title,
                m.status,
                m.updated_at,
                m.link,
                m.parent_module_id,
                m.subtitle,
                m.type
            FROM modules m
            INNER JOIN role_modules rm ON rm.module_id = m.id
            INNER JOIN user_roles ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = :userId
              AND m.parent_module_id = :parentModuleId
              AND ur.assigned = true
              AND rm.assigned = true
              AND m.deletedat IS NULL
              AND m.status = true
              AND rm.deletedat IS NULL
              AND ur.deletedat IS NULL
            ORDER BY m.moduleorder ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("parentModuleId", parentModuleId);

        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllMenuParentByRoleCodes(List<String> roleCodes) {
        // Menú padre según los CÓDIGOS de rol del JWT (independiente del userId).
        String sql = """
            SELECT DISTINCT
                pm.id, pm.created_at, pm.deletedat, pm.icon, pm.moduleorder,
                pm.title, pm.status, pm.updated_at, pm.link, pm.subtitle, pm.type
            FROM parent_modules pm
            WHERE pm.id IN (
                SELECT DISTINCT m.parent_module_id
                FROM modules m
                INNER JOIN role_modules rm ON rm.module_id = m.id
                INNER JOIN roles r ON r.id = rm.role_id
                WHERE r.code IN (:codes)
                  AND rm.assigned = true
                  AND m.deletedat IS NULL
                  AND m.status = true
                  AND rm.deletedat IS NULL
            )
            AND pm.deletedat IS NULL
            AND pm.status = true
            ORDER BY pm.moduleorder ASC
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("codes", roleCodes);
        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllMenuByRoleCodes(List<String> roleCodes, UUID parentModuleId) {
        // Módulos hijo según los CÓDIGOS de rol del JWT (independiente del userId).
        String sql = """
            SELECT DISTINCT
                m.id, m.created_at, m.deletedat, m.icon, m.moduleorder, m.title,
                m.status, m.updated_at, m.link, m.parent_module_id, m.subtitle, m.type
            FROM modules m
            INNER JOIN role_modules rm ON rm.module_id = m.id
            INNER JOIN roles r ON r.id = rm.role_id
            WHERE r.code IN (:codes)
              AND m.parent_module_id = :parentModuleId
              AND rm.assigned = true
              AND m.deletedat IS NULL
              AND m.status = true
              AND rm.deletedat IS NULL
            ORDER BY m.moduleorder ASC
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("codes", roleCodes);
        query.setParameter("parentModuleId", parentModuleId);
        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllModulesAssignedToRole(UUID roleId, UUID parentModuleId) {
        // ✅ CORREGIDO: Usar nombres de columna reales de la BD
        String sql = """
            SELECT 
                m.id, 
                m.created_at, 
                m.deletedat, 
                m.icon, 
                m.moduleorder, 
                m.title, 
                m.status, 
                m.updated_at, 
                m.link, 
                m.parent_module_id, 
                CASE 
                    WHEN EXISTS ( 
                        SELECT 1 
                        FROM role_modules rm 
                        WHERE rm.module_id = m.id 
                          AND rm.role_id = :roleId 
                          AND rm.assigned = true
                          AND rm.deletedat IS NULL 
                    ) THEN TRUE 
                    ELSE FALSE 
                END AS selected,
                m.subtitle, 
                m.type 
            FROM 
                modules m 
            WHERE m.parent_module_id = :parentModuleId
              AND m.deletedat IS NULL
              AND m.status = true
            ORDER BY m.moduleorder ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("roleId", roleId);
        query.setParameter("parentModuleId", parentModuleId);

        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllMenuParentByRole(UUID roleId) {
        String sql = """
            SELECT DISTINCT
                pm.id,
                pm.created_at,
                pm.deletedat,
                pm.icon,
                pm.moduleorder,
                pm.title,
                pm.status,
                pm.updated_at,
                pm.link,
                pm.subtitle,
                pm.type
            FROM parent_modules pm
            WHERE pm.id IN (
                SELECT DISTINCT m.parent_module_id
                FROM modules m
                INNER JOIN role_modules rm ON rm.module_id = m.id
                WHERE rm.role_id = :roleId
                  AND rm.assigned = true
                  AND m.deletedat IS NULL
                  AND m.status = true
                  AND rm.deletedat IS NULL
            )
            AND pm.deletedat IS NULL
            AND pm.status = true
            ORDER BY pm.moduleorder ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("roleId", roleId);
        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllMenuByRole(UUID roleId, UUID parentModuleId) {
        String sql = """
            SELECT DISTINCT
                m.id,
                m.created_at,
                m.deletedat,
                m.icon,
                m.moduleorder,
                m.title,
                m.status,
                m.updated_at,
                m.link,
                m.parent_module_id,
                m.subtitle,
                m.type
            FROM modules m
            INNER JOIN role_modules rm ON rm.module_id = m.id
            WHERE rm.role_id = :roleId
              AND m.parent_module_id = :parentModuleId
              AND rm.assigned = true
              AND m.deletedat IS NULL
              AND m.status = true
              AND rm.deletedat IS NULL
            ORDER BY m.moduleorder ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("roleId", roleId);
        query.setParameter("parentModuleId", parentModuleId);
        return query.getResultList();
    }
}