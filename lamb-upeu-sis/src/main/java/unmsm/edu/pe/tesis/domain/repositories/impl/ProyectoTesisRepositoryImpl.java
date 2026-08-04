package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoTesisRepositoryImpl
        implements ProyectoTesisRepository, PanacheRepositoryBase<ProyectoTesis, UUID> {

    @Override
    public ProyectoTesis save(ProyectoTesis proyecto) {
        if (proyecto.getId() == null) {
            persist(proyecto);
            return proyecto;
        }
        return getEntityManager().merge(proyecto);
    }

    @Override
    public Optional<ProyectoTesis> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public Optional<ProyectoTesis> buscarPorTesisId(UUID tesisId) {
        return find("tesisId = ?1 and active = true", tesisId).firstResultOptional();
    }

    private static final String BANDEJA_FROM = """
            FROM proyectos_tesis pr
            JOIN tesis te ON te.id = pr.tesis_id
            LEFT JOIN tesis_autores ta ON ta.tesis_id = te.id AND ta.es_activa = true AND ta.active = true
            LEFT JOIN estudiantes e ON e.persona_id = ta.estudiante_id
            LEFT JOIN persona p ON p.id = e.persona_id
            LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandejaDeAsesor(UUID asesorId, String estado, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT pr.tesis_id, pr.id, pr.estado, p.apellido_paterno, p.apellido_materno, p.nombres, "
                + "e.codigo_sistema, prog.nombre, te.titulo "
                + BANDEJA_FROM + where(asesorId, estado, buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarBandejaDeAsesor(UUID asesorId, String estado, String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + BANDEJA_FROM + where(asesorId, estado, buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private static final String BANDEJA_TUTOR_FROM = """
            FROM proyectos_tesis pr
            JOIN tesis te ON te.id = pr.tesis_id
            LEFT JOIN tesis_autores ta ON ta.tesis_id = te.id AND ta.es_activa = true AND ta.active = true
            LEFT JOIN estudiantes e ON e.persona_id = ta.estudiante_id
            JOIN tutorias tu ON tu.estudiante_id = e.persona_id AND tu.docente_id = :tutorId AND tu.actual = true AND tu.active = true
            LEFT JOIN persona p ON p.id = e.persona_id
            LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandejaDeTutor(UUID tutorId, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("tutorId", tutorId);
        String sql = "SELECT pr.tesis_id, pr.id, pr.estado, p.apellido_paterno, p.apellido_materno, p.nombres, "
                + "e.codigo_sistema, prog.nombre, te.titulo "
                + BANDEJA_TUTOR_FROM + whereTutor(buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarBandejaDeTutor(UUID tutorId, String buscar) {
        Map<String, Object> params = new HashMap<>();
        params.put("tutorId", tutorId);
        String sql = "SELECT COUNT(*) " + BANDEJA_TUTOR_FROM + whereTutor(buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private static final String BANDEJA_EXP_FROM = """
            FROM proyectos_tesis pr
            JOIN tesis te ON te.id = pr.tesis_id
            LEFT JOIN tesis_autores ta ON ta.tesis_id = te.id AND ta.es_activa = true AND ta.active = true
            LEFT JOIN estudiantes e ON e.persona_id = ta.estudiante_id
            LEFT JOIN persona p ON p.id = e.persona_id
            LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandejaExpedientes(String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT pr.tesis_id, p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema, "
                + "prog.nombre, te.titulo, pr.fecha_solicitud_aprobacion, COALESCE(pr.expediente_recibido, false) "
                + BANDEJA_EXP_FROM + whereExp(buscar, params)
                + " ORDER BY pr.fecha_solicitud_aprobacion DESC NULLS LAST LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarBandejaExpedientes(String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + BANDEJA_EXP_FROM + whereExp(buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandejaDefensa(String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT pr.tesis_id, pr.id, p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema, "
                + "prog.nombre, te.titulo, pr.fecha_recepcion, "
                + "(SELECT COUNT(*) FROM proyecto_revisores rv WHERE rv.proyecto_id = pr.id AND rv.active = true), "
                + "COALESCE(pr.revisores_conformes, false), COALESCE(pr.defensa_programada, false), pr.fecha_defensa, "
                + "COALESCE(pr.jurado_informante_solicitado, false), "
                + "(SELECT COUNT(*) FROM proyecto_informe_revisores ir WHERE ir.proyecto_id = pr.id AND ir.active = true), "
                + "COALESCE(pr.informe_final_revisado, false) "
                + BANDEJA_EXP_FROM + whereDefensa(buscar, params)
                + " ORDER BY pr.fecha_recepcion DESC NULLS LAST LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarBandejaDefensa(String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + BANDEJA_EXP_FROM + whereDefensa(buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    private String whereDefensa(String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE pr.active = true AND COALESCE(pr.expediente_recibido, false) = true ");
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(te.titulo) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }

    private String whereExp(String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE pr.active = true AND pr.expediente_subido = true ");
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(te.titulo) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }

    private String whereTutor(String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE pr.active = true ");
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(te.titulo) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }

    private String where(UUID asesorId, String estado, String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE pr.active = true AND pr.listo_revision = true ");
        if (asesorId != null) {
            // La designación vigente vive en `asesorias`: se consulta ahí y NO por pr.asesor_id,
            // que es una copia escrita al crear el proyecto (si el alumno abrió el editor antes de
            // que su asesor aceptara, quedó en NULL y la bandeja salía vacía).
            // El co-asesor ve los mismos proyectos que su asesor, pero el detalle le llega en
            // modo consulta (ver AsesorProyectoServiceImpl.detalle).
            w.append(" AND (pr.asesor_id = :asesorId OR EXISTS (SELECT 1 FROM asesorias ca ")
             .append("      WHERE ca.tesis_id = pr.tesis_id AND ca.docente_id = :asesorId ")
             .append("      AND UPPER(ca.tipo) IN ('ASESOR', 'COASESOR') AND ca.active = true)) ");
            params.put("asesorId", asesorId);
        }
        if (estado != null && !estado.isBlank()) {
            w.append(" AND pr.estado = :estado ");
            params.put("estado", estado.trim());
        }
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(te.titulo) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }
}
