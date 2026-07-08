package unmsm.edu.pe.tesis.domain.repositories.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import unmsm.edu.pe.tesis.domain.repositories.CoordinadorTemaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CoordinadorTemaRepositoryImpl implements CoordinadorTemaRepository {

    @Inject
    EntityManager em;

    /** Bloque FROM común: estudiante + persona + programa + tesis activa (si existe). */
    private static final String FROM =
            "FROM estudiantes e "
                    + "JOIN persona p ON p.id = e.persona_id "
                    + "LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id "
                    + "LEFT JOIN tesis_autores ta ON ta.estudiante_id = e.persona_id AND ta.es_activa = true AND ta.active = true "
                    + "LEFT JOIN tesis te ON te.id = ta.tesis_id AND te.active = true "
                    + "LEFT JOIN lineas_investigacion li ON li.id = te.linea_investigacion_id ";

    private static final String ASESOR_EXISTS =
            "CASE WHEN EXISTS (SELECT 1 FROM asesorias a WHERE a.tesis_id = te.id "
                    + "AND UPPER(a.tipo) = 'ASESOR' AND a.active = true) THEN 1 ELSE 0 END";

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> estudiantesTema(UUID facultadId, UUID programaId, Boolean conTema, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres, "
                + "e.codigo_sistema, e.cod_matricula, prog.nombre, prog.nivel, "
                + "te.id, te.titulo, li.nombre, te.estado, " + ASESOR_EXISTS + " AS tiene_asesor "
                + FROM + where(facultadId, programaId, conTema, buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = em.createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarEstudiantesTema(UUID facultadId, UUID programaId, Boolean conTema, String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + FROM + where(facultadId, programaId, conTema, buscar, params);
        return scalar(sql, params);
    }

    @Override
    public long contarTotal(UUID facultadId, UUID programaId) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) FROM estudiantes e JOIN persona p ON p.id = e.persona_id "
                + "WHERE p.active = true " + facultadCond(facultadId, params) + programaCond(programaId, params);
        return scalar(sql, params);
    }

    @Override
    public long contarConTema(UUID facultadId, UUID programaId) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) FROM estudiantes e JOIN persona p ON p.id = e.persona_id "
                + "WHERE p.active = true AND EXISTS (SELECT 1 FROM tesis_autores ta "
                + "WHERE ta.estudiante_id = e.persona_id AND ta.es_activa = true AND ta.active = true) "
                + facultadCond(facultadId, params) + programaCond(programaId, params);
        return scalar(sql, params);
    }

    // ── helpers ──
    private String where(UUID facultadId, UUID programaId, Boolean conTema, String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE p.active = true ");
        w.append(facultadCond(facultadId, params));
        w.append(programaCond(programaId, params));
        if (conTema != null) {
            w.append(conTema ? " AND te.id IS NOT NULL " : " AND te.id IS NULL ");
        }
        if (buscar != null && !buscar.isBlank()) {
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar "
                    + "OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(e.codigo_sistema) LIKE :buscar "
                    + "OR LOWER(e.cod_matricula) LIKE :buscar) ");
        }
        return w.toString();
    }

    private String facultadCond(UUID facultadId, Map<String, Object> params) {
        if (facultadId == null) {
            return "";
        }
        params.put("facultadId", facultadId);
        return " AND e.programa_id IN (SELECT pf.id FROM programas_posgrado pf WHERE pf.facultad_id = :facultadId) ";
    }

    private String programaCond(UUID programaId, Map<String, Object> params) {
        if (programaId == null) {
            return "";
        }
        params.put("programaId", programaId);
        return " AND e.programa_id = :programaId ";
    }

    private long scalar(String sql, Map<String, Object> params) {
        Query q = em.createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }
}
