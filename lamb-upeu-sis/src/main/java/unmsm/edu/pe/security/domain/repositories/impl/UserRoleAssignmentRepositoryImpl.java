package unmsm.edu.pe.security.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserRoleAssignmentRepositoryImpl
        implements UserRoleAssignmentRepository, PanacheRepositoryBase<UserRoleAssignment, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public UserRoleAssignment save(UserRoleAssignment userRole) {
        persist(userRole);
        return userRole;
    }

    @Override
    public List<UserRoleAssignment> saveAll(List<UserRoleAssignment> userRoles) {
        persist(userRoles);
        return userRoles;
    }

    @Override
    public void remove(UserRoleAssignment userRole) {
        persist(userRole);
    }

    @Override
    public long count() {
        return count("deletedAt is null");
    }

    @Override
    public List<UserRoleAssignment> findByUser(User user) {
        return list("user.id = ?1 and deletedAt is null", user.getId());
    }

    @Override
    public List<UserRoleAssignment> findByUserId(UUID userId) {
        return list("user.id = ?1 and deletedAt is null", userId);
    }

    @Override
    public List<String> findRoleNamesByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select r.name from UserRoleAssignment ur join ur.role r " +
                                "where ur.user.id = :userId and ur.assigned = true " +
                                "and ur.deletedAt is null and r.deletedAt is null " +
                                "order by r.code asc",
                        String.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public List<String> findRoleCodesByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select r.code from UserRoleAssignment ur join ur.role r " +
                                "where ur.user.id = :userId and ur.assigned = true " +
                                "and ur.deletedAt is null and r.deletedAt is null " +
                                "and r.code is not null " +
                                "order by r.code asc",
                        String.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public List<Object[]> findAllRolesSelectedByUserId(UUID userId) {
        String sql = """
                SELECT
                    r.id,
                    r.created_at,
                    r.deletedat,
                    r.description,
                    r.name,
                    r.status,
                    r.updated_at,
                    r.code,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM user_roles ur
                            WHERE ur.role_id = r.id
                              AND ur.user_id = :userId
                              AND ur.assigned = true
                              AND ur.deletedat IS NULL
                        ) THEN TRUE
                        ELSE FALSE
                    END AS selected
                FROM roles r
                WHERE r.deletedat IS NULL
                ORDER BY r.code ASC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
}