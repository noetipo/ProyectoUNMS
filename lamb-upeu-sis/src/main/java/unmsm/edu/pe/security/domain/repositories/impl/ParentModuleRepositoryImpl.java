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
import unmsm.edu.pe.security.domain.entities.ParentModule;
import unmsm.edu.pe.security.domain.repositories.ParentModuleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ParentModuleRepositoryImpl implements ParentModuleRepository, PanacheRepositoryBase<ParentModule, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<ParentModule> getAllParentModules() {
        return list("deletedAt is null", Sort.by("moduleOrder").ascending());
    }

    @Override
    public Optional<ParentModule> getParentModuleById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    @Override
    public Optional<ParentModule> findByCode(String code) {
        return find("code = ?1 and deletedAt is null", code).firstResultOptional();
    }

    @Override
    public ParentModule save(ParentModule parentModule) {
        persist(parentModule);
        return parentModule;
    }

    @Override
    public Optional<ParentModule> update(UUID id, ParentModule parentModule) {
        Optional<ParentModule> existing = findByIdOptional(id);
        if (existing.isPresent()) {
            ParentModule existingParentModule = existing.get();
            existingParentModule.setTitle(parentModule.getTitle());
            existingParentModule.setIcon(parentModule.getIcon());
            existingParentModule.setModuleOrder(parentModule.getModuleOrder());
            existingParentModule.setLink(parentModule.getLink());
            existingParentModule.setStatus(parentModule.getStatus());
            existingParentModule.setSubtitle(parentModule.getSubtitle());
            existingParentModule.setType(parentModule.getType());
            return Optional.of(existingParentModule);
        }
        return Optional.empty();
    }

    @Override
    public void remove(ParentModule parentModule) {
        persist(parentModule);
    }
    // ✅ IMPLEMENTACIÓN DEL MÉTODO count()
    @Override
    public long count() {
        return count("deletedAt is null");
    }
    @Override
    public List<ParentModule> findWithFilters(String nameFilter, Page page) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParentModule> cq = cb.createQuery(ParentModule.class);
        Root<ParentModule> root = cq.from(ParentModule.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filtro: no eliminados
        predicates.add(cb.isNull(root.get("deletedAt")));

        // Filtro: nombre/título
        if (nameFilter != null && !nameFilter.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + nameFilter.toLowerCase() + "%"));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("moduleOrder")));

        return entityManager.createQuery(cq)
                .setFirstResult(page.index * page.size)
                .setMaxResults(page.size)
                .getResultList();
    }

    @Override
    public long countWithFilters(String nameFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ParentModule> root = cq.from(ParentModule.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filtro: no eliminados
        predicates.add(cb.isNull(root.get("deletedAt")));

        // Filtro: nombre/título
        if (nameFilter != null && !nameFilter.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + nameFilter.toLowerCase() + "%"));
        }

        cq.select(cb.count(root));
        cq.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    public List<ParentModule> findAllActiveWithFilters(String nameFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ParentModule> cq = cb.createQuery(ParentModule.class);
        Root<ParentModule> root = cq.from(ParentModule.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filtro: no eliminados
        predicates.add(cb.isNull(root.get("deletedAt")));

        // Filtro: nombre/título
        if (nameFilter != null && !nameFilter.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + nameFilter.toLowerCase() + "%"));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("moduleOrder")));

        return entityManager.createQuery(cq).getResultList();
    }
}