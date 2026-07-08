package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EstudianteRepositoryImpl implements EstudianteRepository, PanacheRepositoryBase<Estudiante, UUID> {

    @Override
    public Estudiante save(Estudiante estudiante) {
        persist(estudiante);
        return estudiante;
    }

    @Override
    public Optional<Estudiante> findByPersonaId(UUID personaId) {
        return findByIdOptional(personaId);
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
    public boolean existsByCodMatricula(String codMatricula) {
        return codMatricula != null && !codMatricula.isBlank()
                && count("codMatricula = ?1", codMatricula.trim()) > 0;
    }

    @Override
    public boolean existsByEmailInstitucional(String emailInstitucional) {
        return emailInstitucional != null && !emailInstitucional.isBlank()
                && count("emailInstitucional = ?1", emailInstitucional.trim()) > 0;
    }

    @Override
    public List<Object[]> listar(String search, UUID facultadId, UUID programaId, String condicion, String nivel, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.numero_documento, p.apellido_paterno, p.apellido_materno, p.nombres,
                       e.codigo_sistema, e.cod_matricula, e.email_institucional, e.anio_ingreso,
                       e.condicion, e.financiamiento, e.programa_id, pr.nombre, pr.nivel, p.active
                FROM estudiantes e
                JOIN persona p ON p.id = e.persona_id
                LEFT JOIN programas_posgrado pr ON pr.id = e.programa_id
                """);
        Map<String, Object> params = new HashMap<>();
        sql.append(whereClause(search, facultadId, programaId, condicion, nivel, params));
        sql.append(" ORDER BY p.apellido_paterno ASC, p.nombres ASC ");
        sql.append(" LIMIT :size OFFSET :offset ");

        Query q = getEntityManager().createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contar(String search, UUID facultadId, UUID programaId, String condicion, String nivel) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM estudiantes e
                JOIN persona p ON p.id = e.persona_id
                LEFT JOIN programas_posgrado pr ON pr.id = e.programa_id
                """);
        Map<String, Object> params = new HashMap<>();
        sql.append(whereClause(search, facultadId, programaId, condicion, nivel, params));

        Query q = getEntityManager().createNativeQuery(sql.toString());
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarActivos() {
        Query q = getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM estudiantes e JOIN persona p ON p.id = e.persona_id WHERE p.active = true");
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarPorCondicion(String condicion) {
        Query q = getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM estudiantes e JOIN persona p ON p.id = e.persona_id " +
                        "WHERE p.active = true AND e.condicion = :condicion");
        q.setParameter("condicion", condicion);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String whereClause(String search, UUID facultadId, UUID programaId, String condicion, String nivel, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE p.active = true ");
        if (search != null && !search.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :search OR LOWER(p.apellido_paterno) LIKE :search ")
             .append(" OR LOWER(p.apellido_materno) LIKE :search OR LOWER(p.numero_documento) LIKE :search ")
             .append(" OR LOWER(e.cod_matricula) LIKE :search) ");
            params.put("search", "%" + search.trim().toLowerCase() + "%");
        }
        if (facultadId != null) {
            w.append(" AND pr.facultad_id = :facultadId ");
            params.put("facultadId", facultadId);
        }
        if (programaId != null) {
            w.append(" AND e.programa_id = :programaId ");
            params.put("programaId", programaId);
        }
        if (condicion != null && !condicion.isBlank()) {
            w.append(" AND e.condicion = :condicion ");
            params.put("condicion", condicion.trim());
        }
        if (nivel != null && !nivel.isBlank()) {
            w.append(" AND pr.nivel = :nivel ");
            params.put("nivel", nivel.trim());
        }
        return w.toString();
    }
}
