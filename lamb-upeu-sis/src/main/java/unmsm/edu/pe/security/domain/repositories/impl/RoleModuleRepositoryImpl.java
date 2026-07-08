package unmsm.edu.pe.security.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.RoleModule;
import unmsm.edu.pe.security.domain.repositories.RoleModuleRepository;


import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleModuleRepositoryImpl implements RoleModuleRepository, PanacheRepositoryBase<RoleModule, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<RoleModule> getAllRoleModules() {
        return listAll();
    }

    @Override
    public Optional<RoleModule> getRoleModuleById(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public RoleModule save(RoleModule roleModule) {
        persist(roleModule);
        return roleModule;
    }

    @Override
    public void remove(RoleModule roleModule) {
        delete(roleModule);
    }

    @Override
    public List<RoleModule> findByModuleInAndRole(Collection<Module> modules, Role role) {
        if (modules == null || modules.isEmpty()) {
            return Collections.emptyList();
        }

        // Extraer los IDs de los módulos
        List<UUID> moduleIds = modules.stream()
                .map(Module::getId)
                .collect(Collectors.toList());

        // Usar Criteria API para la consulta
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<RoleModule> cq = cb.createQuery(RoleModule.class);
        Root<RoleModule> root = cq.from(RoleModule.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filtro: module.id IN (moduleIds)
        predicates.add(root.get("module").get("id").in(moduleIds));

        // Filtro: role.id = ?
        predicates.add(cb.equal(root.get("role").get("id"), role.getId()));

        cq.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public long count() {
        return count();
    }
}