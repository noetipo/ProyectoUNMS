package unmsm.edu.pe.tutorias.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TutoriaRepositoryImpl implements TutoriaRepository, PanacheRepositoryBase<Tutoria, UUID> {

    @Override
    public Tutoria save(Tutoria tutoria) {
        if (tutoria.getId() == null) {
            persist(tutoria);
            return tutoria;
        }
        return getEntityManager().merge(tutoria);
    }

    @Override
    public Optional<Tutoria> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<Tutoria> findVigenteByEstudiante(UUID estudianteId) {
        return find("estudiante.personaId = ?1 and actual = true and active = true", estudianteId)
                .firstResultOptional();
    }

    @Override
    public long countActualesByDocente(UUID docenteId) {
        return count("docente.personaId = ?1 and actual = true and active = true", docenteId);
    }

    @Override
    public int cerrarVigentePorEstudiante(UUID estudianteId, java.time.LocalDate fechaFin) {
        // Bulk update: se ejecuta de inmediato (antes del insert de la nueva tutoría),
        // evitando el choque con el índice único parcial (una sola vigente por estudiante).
        return update("actual = false, fechaFin = ?1 where estudiante.personaId = ?2 and actual = true and active = true",
                fechaFin, estudianteId);
    }

    @Override
    public List<Tutoria> findByEstudianteId(UUID estudianteId) {
        return list("estudiante.personaId = ?1",
                Sort.by("actual").descending().and("fechaInicio", Sort.Direction.Descending),
                estudianteId);
    }

    // ── Tutores (cualquier docente activo puede ser tutor) ───────────────────

    private static final String TUTORES_FROM = """
            FROM docentes d
            JOIN persona p ON p.id = d.persona_id
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarTutores(String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = """
                SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres,
                       d.codigo_sistema,
                       (SELECT g.grado FROM persona_grados_academicos g WHERE g.persona_id = d.persona_id AND g.principal = true AND g.active = true LIMIT 1) AS grado_academico,
                       d.cupo_maximo_tutoria,
                       COALESCE(t.cnt, 0) AS actuales,
                       EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                               WHERE ur.user_id = p.user_id AND ur.assigned = true AND r.code = 'PROF_TUTOR') AS es_tutor
                """ + TUTORES_FROM
                + "LEFT JOIN (SELECT docente_id, COUNT(*) AS cnt FROM tutorias WHERE actual = true AND active = true GROUP BY docente_id) t ON t.docente_id = d.persona_id "
                + tutoresWhere(buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarTutores(String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(DISTINCT d.persona_id) " + TUTORES_FROM + tutoresWhere(buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String tutoresWhere(String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE p.active = true ");
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(d.codigo_sistema) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }

    // ── Estudiantes asignables (programa + estado tutor + texto) ──────────────

    private static final String ESTUDIANTES_FROM = """
            FROM estudiantes e
            JOIN persona p ON p.id = e.persona_id
            LEFT JOIN tutorias tv ON tv.estudiante_id = e.persona_id AND tv.actual = true AND tv.active = true
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarEstudiantesAsignables(UUID facultadId, UUID programaId, Boolean conTutor, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = """
                SELECT p.id, p.nombres, p.apellido_paterno, p.apellido_materno, e.codigo_sistema,
                       prog.id AS programa_id, prog.nombre AS programa_nombre,
                       tv.docente_id AS tutor_id, dp.nombres AS tutor_nombres,
                       dp.apellido_paterno AS tutor_ap, dp.apellido_materno AS tutor_am
                """ + ESTUDIANTES_FROM
                + "LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id "
                + "LEFT JOIN persona dp ON dp.id = tv.docente_id "
                + estudiantesWhere(facultadId, programaId, conTutor, buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarEstudiantesAsignables(UUID facultadId, UUID programaId, Boolean conTutor, String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + ESTUDIANTES_FROM + estudiantesWhere(facultadId, programaId, conTutor, buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String estudiantesWhere(UUID facultadId, UUID programaId, Boolean conTutor, String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE p.active = true ");
        if (facultadId != null) {
            w.append(" AND e.programa_id IN (SELECT pf.id FROM programas_posgrado pf WHERE pf.facultad_id = :facultadId) ");
            params.put("facultadId", facultadId);
        }
        if (programaId != null) {
            w.append(" AND e.programa_id = :programaId ");
            params.put("programaId", programaId);
        }
        if (Boolean.TRUE.equals(conTutor)) {
            w.append(" AND tv.id IS NOT NULL ");
        } else if (Boolean.FALSE.equals(conTutor)) {
            w.append(" AND tv.id IS NULL ");
        }
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(e.codigo_sistema) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }
}
