package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PersonaRepositoryImpl implements PersonaRepository, PanacheRepositoryBase<Persona, UUID> {

    @Override
    public Persona save(Persona persona) {
        persist(persona);
        return persona;
    }

    @Override
    public Optional<Persona> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<Persona> findByUserId(UUID userId) {
        return find("user.id = ?1", userId).firstResultOptional();
    }

    @Override
    public boolean existsByNumeroDocumento(String numeroDocumento) {
        return numeroDocumento != null && !numeroDocumento.isBlank()
                && count("numeroDocumento = ?1", numeroDocumento.trim()) > 0;
    }

    @Override
    public boolean existsByOrcid(String orcid) {
        return orcid != null && !orcid.isBlank() && count("orcid = ?1", orcid.trim()) > 0;
    }

    @Override
    public List<Object[]> listar(String search, String tipoPerfil, Boolean activo, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.numero_documento, p.apellido_paterno, p.apellido_materno,
                       p.nombres, p.email_personal, p.celular, p.active,
                       CASE WHEN EXISTS (SELECT 1 FROM estudiantes e WHERE e.persona_id = p.id) THEN true ELSE false END AS has_est,
                       CASE WHEN EXISTS (SELECT 1 FROM docentes d WHERE d.persona_id = p.id) THEN true ELSE false END AS has_doc
                FROM persona p
                """);
        Map<String, Object> params = new java.util.HashMap<>();
        sql.append(whereClause(search, tipoPerfil, activo, params));
        // Último registro primero (para validar de inmediato el alta reciente).
        sql.append(" ORDER BY p.created_at DESC, p.apellido_paterno ASC ");
        sql.append(" LIMIT :size OFFSET :offset ");

        Query q = getEntityManager().createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contar(String search, String tipoPerfil, Boolean activo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM persona p ");
        Map<String, Object> params = new java.util.HashMap<>();
        sql.append(whereClause(search, tipoPerfil, activo, params));

        Query q = getEntityManager().createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String whereClause(String search, String tipoPerfil, Boolean activo, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE 1=1 ");
        // Por defecto solo activos
        w.append(" AND p.active = :activo ");
        params.put("activo", activo == null ? Boolean.TRUE : activo);

        if (search != null && !search.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :search OR LOWER(p.apellido_paterno) LIKE :search ")
             .append(" OR LOWER(p.apellido_materno) LIKE :search OR LOWER(p.numero_documento) LIKE :search) ");
            params.put("search", "%" + search.trim().toLowerCase() + "%");
        }
        if ("ESTUDIANTE".equalsIgnoreCase(tipoPerfil)) {
            w.append(" AND EXISTS (SELECT 1 FROM estudiantes e WHERE e.persona_id = p.id) ");
        } else if ("DOCENTE".equalsIgnoreCase(tipoPerfil)) {
            w.append(" AND EXISTS (SELECT 1 FROM docentes d WHERE d.persona_id = p.id) ");
        } else if ("AMBOS".equalsIgnoreCase(tipoPerfil)) {
            w.append(" AND EXISTS (SELECT 1 FROM estudiantes e WHERE e.persona_id = p.id) ")
             .append(" AND EXISTS (SELECT 1 FROM docentes d WHERE d.persona_id = p.id) ");
        }
        return w.toString();
    }
}
