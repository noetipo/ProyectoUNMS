package unmsm.edu.pe.security.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<User, UUID> {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<User> getAllUsers() {
        return listAll();
    }

    @Override
    public List<User> findAllByStatus(UserStatus status) {
        return list("status = ?1", status);
    }

    @Override
    public Optional<User> getUserById(UUID id) {
        return find("id = ?1", id).firstResultOptional();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.out.println("⚠️ Username es null o vacío");
            return Optional.empty();
        }

        // ✅ Normalizar el username a lowercase antes de buscar (coincide con @Normalize en la entidad)
        String normalizedUsername = username.toLowerCase().trim();
        System.out.println("🔍 Buscando usuario con username: " + normalizedUsername);

        Optional<User> result = find("LOWER(username) = ?1", normalizedUsername).firstResultOptional();

        System.out.println("🔍 Usuario encontrado: " + result.isPresent());
        if (result.isPresent()) {
            System.out.println("🔍 Detalles: ID=" + result.get().getId() + ", Username=" + result.get().getUsername());
        }

        return result;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            System.out.println("⚠️ Email es null o vacío");
            return Optional.empty();
        }

        // ✅ Normalizar el email a lowercase antes de buscar (coincide con @Normalize en la entidad)
        String normalizedEmail = email.toLowerCase().trim();
        System.out.println("🔍 Buscando usuario con email: " + normalizedEmail);

        Optional<User> result = find("LOWER(email) = ?1", normalizedEmail).firstResultOptional();

        System.out.println("🔍 Usuario encontrado: " + result.isPresent());
        if (result.isPresent()) {
            System.out.println("🔍 Detalles: ID=" + result.get().getId() + ", Email=" + result.get().getEmail());
        }

        return result;
    }

    @Override
    public User saveUser(User user) {
        persist(user);
        // ✅ Flush para asegurar que se persista inmediatamente en la BD
        entityManager.flush();
        System.out.println("✅ Usuario guardado en BD: ID=" + user.getId() + ", Username=" + user.getUsername());
        return user;
    }

    @Override
    public void removeUserById(UUID id) {
        delete("id", id);
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        // ✅ Normalizar antes de buscar
        String normalizedUsername = username.toLowerCase().trim();
        System.out.println("🔍 Verificando existencia de username: " + normalizedUsername);

        long count = count("LOWER(username) = ?1", normalizedUsername);
        System.out.println("🔍 Count resultado: " + count);

        return count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // ✅ Normalizar antes de buscar
        String normalizedEmail = email.toLowerCase().trim();
        System.out.println("🔍 Verificando existencia de email: " + normalizedEmail);

        long count = count("LOWER(email) = ?1", normalizedEmail);
        System.out.println("🔍 Count resultado: " + count);

        return count > 0;
    }

    @Override
    public long countByStatus(UserStatus status) {
        return count("status = ?1", status);
    }

    // ── Reporte de usuarios (JOIN persona + filtros) ──────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarReporte(String search, List<String> roleCodes, List<UserStatus> statuses,
                                        int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select u.id, u.username, u.email, u.firstName, u.lastName, u.status, u.active, "
                + "p.id, p.apellidoPaterno, p.apellidoMaterno, p.nombres, p.numeroDocumento "
                + "from User u left join Persona p on p.user = u "
                + whereReporte(search, roleCodes, statuses, params)
                + " order by u.username asc";
        Query q = entityManager.createQuery(jpql);
        params.forEach(q::setParameter);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }

    @Override
    public long contarReporte(String search, List<String> roleCodes, List<UserStatus> statuses) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select count(u) from User u left join Persona p on p.user = u "
                + whereReporte(search, roleCodes, statuses, params);
        Query q = entityManager.createQuery(jpql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String whereReporte(String search, List<String> roleCodes, List<UserStatus> statuses,
                                Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" where 1=1 ");
        if (search != null && !search.isBlank()) {
            w.append("and (lower(u.username) like :s or lower(u.email) like :s "
                    + "or lower(coalesce(p.nombres,'')) like :s "
                    + "or lower(coalesce(p.apellidoPaterno,'')) like :s "
                    + "or lower(coalesce(p.apellidoMaterno,'')) like :s "
                    + "or lower(coalesce(p.numeroDocumento,'')) like :s) ");
            params.put("s", "%" + search.trim().toLowerCase() + "%");
        }
        if (statuses != null && !statuses.isEmpty()) {
            w.append("and u.status in :statuses ");
            params.put("statuses", statuses);
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            w.append("and exists (select 1 from UserRoleAssignment ur "
                    + "where ur.user = u and ur.assigned = true and ur.role.code in :roles) ");
            params.put("roles", roleCodes);
        }
        return w.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> rolesDeUsuarios(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select ur.user.id, ur.role.code from UserRoleAssignment ur "
                                + "where ur.user.id in :ids and ur.assigned = true")
                .setParameter("ids", userIds)
                .getResultList();
    }
}