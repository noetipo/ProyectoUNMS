package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DocenteRepositoryImpl implements DocenteRepository, PanacheRepositoryBase<Docente, UUID> {

    @Override
    public Docente save(Docente docente) {
        persist(docente);
        return docente;
    }

    @Override
    public Optional<Docente> findByPersonaId(UUID personaId) {
        return findByIdOptional(personaId);
    }

    @Override
    public List<Docente> listarTodosActivos() {
        return list("persona.active = true");
    }

    @Override
    public boolean existsByPersonaId(UUID personaId) {
        return personaId != null && count("personaId = ?1", personaId) > 0;
    }

    @Override
    public boolean existsByCodigoSistema(String codigoSistema) {
        return codigoSistema != null && !codigoSistema.isBlank()
                && count("codigoSistema = ?1", codigoSistema.trim()) > 0;
    }

    @Override
    public boolean existsByEmailInstitucional(String emailInstitucional) {
        return emailInstitucional != null && !emailInstitucional.isBlank()
                && count("emailInstitucional = ?1", emailInstitucional.trim()) > 0;
    }

    @Override
    public List<Object[]> listar(String search, String grado, String categoria, String condicion, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.numero_documento, p.apellido_paterno, p.apellido_materno, p.nombres,
                       d.codigo_sistema, d.email_institucional,
                       (SELECT g.grado FROM persona_grados_academicos g WHERE g.persona_id = d.persona_id AND g.principal = true AND g.active = true LIMIT 1) AS grado_academico,
                       d.categoria, d.condicion,
                       p.active,
                       COALESCE(a.cnt, 0) AS asesorias_cnt,
                       COALESCE(j.cnt, 0) AS jurados_cnt
                FROM docentes d
                JOIN persona p ON p.id = d.persona_id
                LEFT JOIN (SELECT docente_id, COUNT(*) AS cnt FROM asesorias WHERE active = true GROUP BY docente_id) a
                       ON a.docente_id = d.persona_id
                LEFT JOIN (SELECT docente_id, COUNT(*) AS cnt FROM jurados WHERE active = true GROUP BY docente_id) j
                       ON j.docente_id = d.persona_id
                """);
        Map<String, Object> params = new HashMap<>();
        sql.append(whereClause(search, grado, categoria, condicion, params));
        sql.append(" ORDER BY p.apellido_paterno ASC, p.nombres ASC ");
        sql.append(" LIMIT :size OFFSET :offset ");

        Query q = getEntityManager().createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contar(String search, String grado, String categoria, String condicion) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM docentes d JOIN persona p ON p.id = d.persona_id ");
        Map<String, Object> params = new HashMap<>();
        sql.append(whereClause(search, grado, categoria, condicion, params));

        Query q = getEntityManager().createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarActivos() {
        return ((Number) getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM docentes d JOIN persona p ON p.id = d.persona_id WHERE p.active = true")
                .getSingleResult()).longValue();
    }

    @Override
    public long contarPorGrado(String grado) {
        Query q = getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM docentes d JOIN persona p ON p.id = d.persona_id " +
                        "WHERE p.active = true AND EXISTS (SELECT 1 FROM persona_grados_academicos g " +
                        "WHERE g.persona_id = d.persona_id AND g.grado = :grado AND g.principal = true AND g.active = true)");
        q.setParameter("grado", grado);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarAsesorando() {
        return ((Number) getEntityManager().createNativeQuery(
                "SELECT COUNT(DISTINCT d.persona_id) FROM docentes d " +
                        "JOIN persona p ON p.id = d.persona_id " +
                        "JOIN asesorias a ON a.docente_id = d.persona_id AND a.active = true " +
                        "WHERE p.active = true")
                .getSingleResult()).longValue();
    }

    @Override
    public long contarEnJurados() {
        return ((Number) getEntityManager().createNativeQuery(
                "SELECT COUNT(DISTINCT d.persona_id) FROM docentes d " +
                        "JOIN persona p ON p.id = d.persona_id " +
                        "JOIN jurados j ON j.docente_id = d.persona_id AND j.active = true " +
                        "WHERE p.active = true")
                .getSingleResult()).longValue();
    }

    private String whereClause(String search, String grado, String categoria, String condicion, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE p.active = true ");
        if (search != null && !search.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :search OR LOWER(p.apellido_paterno) LIKE :search ")
             .append(" OR LOWER(p.apellido_materno) LIKE :search OR LOWER(p.numero_documento) LIKE :search ")
             .append(" OR LOWER(d.codigo_sistema) LIKE :search) ");
            params.put("search", "%" + search.trim().toLowerCase() + "%");
        }
        if (grado != null && !grado.isBlank()) {
            // Filtra por el grado PRINCIPAL (el que muestra la columna "Grado" del reporte).
            w.append(" AND EXISTS (SELECT 1 FROM persona_grados_academicos g " +
                    "WHERE g.persona_id = d.persona_id AND g.grado = :grado AND g.principal = true AND g.active = true) ");
            params.put("grado", grado.trim());
        }
        if (categoria != null && !categoria.isBlank()) {
            w.append(" AND d.categoria = :categoria ");
            params.put("categoria", categoria.trim());
        }
        if (condicion != null && !condicion.isBlank()) {
            w.append(" AND d.condicion = :condicion ");
            params.put("condicion", condicion.trim());
        }
        return w.toString();
    }
}
