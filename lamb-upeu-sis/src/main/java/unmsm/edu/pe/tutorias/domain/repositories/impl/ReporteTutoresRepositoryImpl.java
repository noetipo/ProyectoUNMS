package unmsm.edu.pe.tutorias.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.ReporteTutoresRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ReporteTutoresRepositoryImpl implements ReporteTutoresRepository, PanacheRepositoryBase<Tutoria, UUID> {

    // ── Resumen ──────────────────────────────────────────────────────────────

    @Override
    public long contarTutores(String buscar, UUID facultadId, UUID programaId) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(DISTINCT t.docente_id) "
                + "FROM tutorias t JOIN docentes d ON d.persona_id = t.docente_id "
                + "JOIN persona p ON p.id = d.persona_id "
                + "JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                + tutoresWhere(buscar, facultadId, programaId, params);
        return scalar(sql, params);
    }

    @Override
    public long contarEstudiantesConTutor(UUID facultadId, UUID programaId) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(DISTINCT t.estudiante_id) "
                + "FROM tutorias t JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                + "JOIN persona p ON p.id = e.persona_id "
                + "WHERE t.actual = true AND t.active = true AND p.active = true "
                + facultadCond(facultadId, params, "e") + programaCond(programaId, params, "e");
        return scalar(sql, params);
    }

    @Override
    public long contarEstudiantesTotales(UUID facultadId, UUID programaId) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) FROM estudiantes e JOIN persona p ON p.id = e.persona_id "
                + "WHERE p.active = true " + facultadCond(facultadId, params, "e") + programaCond(programaId, params, "e");
        return scalar(sql, params);
    }

    // ── Lista de tutores ─────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarTutores(String buscar, UUID facultadId, UUID programaId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres, "
                + "(SELECT g.grado FROM persona_grados_academicos g WHERE g.persona_id = d.persona_id AND g.principal = true AND g.active = true LIMIT 1), "
                + "d.cupo_maximo_tutoria, COUNT(t.id) AS estudiantes, "
                + "COUNT(DISTINCT e.programa_id) AS programas "
                + "FROM tutorias t JOIN docentes d ON d.persona_id = t.docente_id "
                + "JOIN persona p ON p.id = d.persona_id "
                + "JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                + tutoresWhere(buscar, facultadId, programaId, params)
                + " GROUP BY p.id, d.persona_id, p.apellido_paterno, p.apellido_materno, p.nombres, d.cupo_maximo_tutoria "
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    // ── Estudiantes de un tutor ──────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> estudiantesDeTutor(UUID docenteId, int page, int size) {
        Query q = getEntityManager().createNativeQuery(
                "SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema, "
                        + "e.cod_matricula, prog.nombre, t.fecha_inicio, e.anio_ingreso "
                        + "FROM tutorias t JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                        + "JOIN persona p ON p.id = e.persona_id "
                        + "LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id "
                        + "WHERE t.docente_id = :docenteId AND t.actual = true AND t.active = true "
                        + "ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset");
        q.setParameter("docenteId", docenteId);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarEstudiantesDeTutor(UUID docenteId) {
        Query q = getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM tutorias t WHERE t.docente_id = :docenteId AND t.actual = true AND t.active = true");
        q.setParameter("docenteId", docenteId);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> tutorandos(UUID docenteId, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("docenteId", docenteId);
        String sql = "SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema, "
                + "e.cod_matricula, prog.nombre, t.fecha_inicio, e.anio_ingreso, "
                + "te.id, te.titulo, li.nombre, te.estado, "
                + "CASE WHEN EXISTS (SELECT 1 FROM asesorias a WHERE a.tesis_id = te.id "
                + "AND UPPER(a.tipo) = 'ASESOR' AND a.active = true) THEN 1 ELSE 0 END AS tiene_asesor "
                + "FROM tutorias t JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                + "JOIN persona p ON p.id = e.persona_id "
                + "LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id "
                + "LEFT JOIN tesis_autores ta ON ta.estudiante_id = e.persona_id AND ta.es_activa = true AND ta.active = true "
                + "LEFT JOIN tesis te ON te.id = ta.tesis_id AND te.active = true "
                + "LEFT JOIN lineas_investigacion li ON li.id = te.linea_investigacion_id "
                + "WHERE t.docente_id = :docenteId AND t.actual = true AND t.active = true "
                + tutorandoBuscar(buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarTutorandos(UUID docenteId, String buscar) {
        Map<String, Object> params = new HashMap<>();
        params.put("docenteId", docenteId);
        String sql = "SELECT COUNT(*) FROM tutorias t JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                + "JOIN persona p ON p.id = e.persona_id "
                + "WHERE t.docente_id = :docenteId AND t.actual = true AND t.active = true "
                + tutorandoBuscar(buscar, params);
        return scalar(sql, params);
    }

    private String tutorandoBuscar(String buscar, Map<String, Object> params) {
        if (buscar == null || buscar.isBlank()) return "";
        params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        return " AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar "
                + "OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(e.codigo_sistema) LIKE :buscar "
                + "OR LOWER(e.cod_matricula) LIKE :buscar) ";
    }

    // ── Estudiantes sin tutor ────────────────────────────────────────────────

    private String sinTutorFrom() {
        return "FROM estudiantes e JOIN persona p ON p.id = e.persona_id "
                + "WHERE p.active = true AND NOT EXISTS (SELECT 1 FROM tutorias t "
                + "WHERE t.estudiante_id = e.persona_id AND t.actual = true AND t.active = true) ";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> estudiantesSinTutor(UUID facultadId, UUID programaId, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema, "
                + "e.cod_matricula, prog.nombre, e.anio_ingreso "
                + "FROM estudiantes e JOIN persona p ON p.id = e.persona_id "
                + "LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id "
                + "WHERE p.active = true AND NOT EXISTS (SELECT 1 FROM tutorias t "
                + "WHERE t.estudiante_id = e.persona_id AND t.actual = true AND t.active = true) "
                + facultadCond(facultadId, params, "e") + programaCond(programaId, params, "e") + estudianteBuscar(buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarEstudiantesSinTutor(UUID facultadId, UUID programaId, String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + sinTutorFrom()
                + facultadCond(facultadId, params, "e") + programaCond(programaId, params, "e") + estudianteBuscar(buscar, params);
        return scalar(sql, params);
    }

    // ── Export (plano, sin paginar) ──────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> datosExport(String buscar, UUID facultadId, UUID programaId) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT tp.id, tp.apellido_paterno, tp.apellido_materno, tp.nombres, "
                + "(SELECT g.grado FROM persona_grados_academicos g WHERE g.persona_id = d.persona_id AND g.principal = true AND g.active = true LIMIT 1), "
                + "d.cupo_maximo_tutoria, "
                + "ep.apellido_paterno, ep.apellido_materno, ep.nombres, e.codigo_sistema, e.cod_matricula, "
                + "prog.nombre, t.fecha_inicio "
                + "FROM tutorias t JOIN docentes d ON d.persona_id = t.docente_id "
                + "JOIN persona tp ON tp.id = d.persona_id "
                + "JOIN estudiantes e ON e.persona_id = t.estudiante_id "
                + "JOIN persona ep ON ep.id = e.persona_id "
                + "LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id "
                + "WHERE t.actual = true AND t.active = true AND tp.active = true "
                + facultadCond(facultadId, params, "e") + programaCond(programaId, params, "e") + tutorBuscar(buscar, params, "tp")
                + " ORDER BY tp.apellido_paterno ASC, tp.nombres ASC, ep.apellido_paterno ASC";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long scalar(String sql, Map<String, Object> params) {
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String tutoresWhere(String buscar, UUID facultadId, UUID programaId, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE t.actual = true AND t.active = true AND p.active = true ");
        w.append(facultadCond(facultadId, params, "e"));
        w.append(programaCond(programaId, params, "e"));
        w.append(tutorBuscar(buscar, params, "p"));
        return w.toString();
    }

    private String programaCond(UUID programaId, Map<String, Object> params, String estAlias) {
        if (programaId == null) return "";
        params.put("programaId", programaId);
        return " AND " + estAlias + ".programa_id = :programaId ";
    }

    /** Filtra por facultad vía el programa del estudiante (cascada Facultad → Programa). */
    private String facultadCond(UUID facultadId, Map<String, Object> params, String estAlias) {
        if (facultadId == null) return "";
        params.put("facultadId", facultadId);
        return " AND " + estAlias + ".programa_id IN (SELECT pf.id FROM programas_posgrado pf WHERE pf.facultad_id = :facultadId) ";
    }

    private String tutorBuscar(String buscar, Map<String, Object> params, String personaAlias) {
        if (buscar == null || buscar.isBlank()) return "";
        params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        return " AND (LOWER(" + personaAlias + ".nombres) LIKE :buscar OR LOWER(" + personaAlias
                + ".apellido_paterno) LIKE :buscar OR LOWER(" + personaAlias + ".apellido_materno) LIKE :buscar) ";
    }

    private String estudianteBuscar(String buscar, Map<String, Object> params) {
        if (buscar == null || buscar.isBlank()) return "";
        params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        return " AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar "
                + "OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(e.codigo_sistema) LIKE :buscar) ";
    }
}
