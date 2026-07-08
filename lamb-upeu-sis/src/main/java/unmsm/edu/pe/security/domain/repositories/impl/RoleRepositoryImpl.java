package unmsm.edu.pe.security.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleRepositoryImpl implements RoleRepository, PanacheRepositoryBase<Role, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<Role> getAllRoles() {
        return list("deletedAt is null", Sort.by("createdAt").descending());
    }

    @Override
    public Optional<Role> getRoleById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return find("code = ?1 and deletedAt is null", code).firstResultOptional();
    }

    @Override
    public Optional<Role> findFirstByCodeOrderByCreatedAtAsc(String code) {
        return find("code = ?1 and deletedAt is null", Sort.by("createdAt").ascending(), code)
                .firstResultOptional();
    }

    @Override
    public Role save(Role role) {
        persist(role);
        return role;
    }

    @Override
    public Optional<Role> update(UUID id, Role role) {
        Optional<Role> existing = findByIdOptional(id);
        if (existing.isPresent()) {
            Role existingRole = existing.get();
            existingRole.setName(role.getName());
            existingRole.setDescription(role.getDescription());
            existingRole.setStatus(role.getStatus());
            existingRole.setCode(role.getCode());
            return Optional.of(existingRole);
        }
        return Optional.empty();
    }

    @Override
    public void remove(Role role) {
        persist(role);
    }

    @Override
    public long count() {
        return count("deletedAt is null");
    }

    @Override
    public List<Role> findWithFilters(String nameFilter, Page page) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filtro: no eliminados
        predicates.add(cb.isNull(root.get("deletedAt")));

        // Filtro: nombre
        if (nameFilter != null && !nameFilter.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%"));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")));

        return entityManager.createQuery(cq)
                .setFirstResult(page.index * page.size)
                .setMaxResults(page.size)
                .getResultList();
    }

    @Override
    public long countWithFilters(String nameFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Role> root = cq.from(Role.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filtro: no eliminados
        predicates.add(cb.isNull(root.get("deletedAt")));

        // Filtro: nombre
        if (nameFilter != null && !nameFilter.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%"));
        }

        cq.select(cb.count(root));
        cq.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getSingleResult();
    }
}