package unmsm.edu.pe.tesis.domain.repositories.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import unmsm.edu.pe.tesis.domain.repositories.SeguimientoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SeguimientoRepositoryImpl implements SeguimientoRepository {

    @Inject
    EntityManager em;

    /** Estudiante + tesis activa (si tiene) + proyecto (si tiene). Los que no tienen tema también salen. */
    private static final String FROM = """
            FROM estudiantes e
            JOIN persona p ON p.id = e.persona_id
            LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
            LEFT JOIN tesis_autores ta ON ta.estudiante_id = e.persona_id AND ta.es_activa = true AND ta.active = true
            LEFT JOIN tesis te ON te.id = ta.tesis_id AND te.active = true
            LEFT JOIN lineas_investigacion li ON li.id = te.linea_investigacion_id
            LEFT JOIN proyectos_tesis pr ON pr.tesis_id = te.id AND pr.active = true
            """;

    private static final String TUTOR = """
            (SELECT TRIM(CONCAT_WS(' ', pd.nombres, pd.apellido_paterno, pd.apellido_materno))
               FROM tutorias tu
               JOIN persona pd ON pd.id = tu.docente_id
              WHERE tu.estudiante_id = e.persona_id AND tu.actual = true AND tu.active = true
              LIMIT 1)
            """;

    private static final String ASESOR = """
            (SELECT TRIM(CONCAT_WS(' ', pa.nombres, pa.apellido_paterno, pa.apellido_materno))
               FROM asesorias a
               JOIN persona pa ON pa.id = a.docente_id
              WHERE a.tesis_id = te.id AND UPPER(a.tipo) = 'ASESOR' AND a.active = true
              LIMIT 1)
            """;

    private static final String TUTORIA_INICIO = """
            (SELECT tu2.fecha_inicio FROM tutorias tu2
              WHERE tu2.estudiante_id = e.persona_id AND tu2.actual = true AND tu2.active = true
              LIMIT 1)
            """;

    private static final String DICTAMEN_EMISION = """
            (SELECT d2.fecha_emision FROM dictamenes_designacion d2
              WHERE d2.tesis_id = te.id AND d2.estado = 'FIRMADO' AND d2.active = true
              LIMIT 1)
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> seguimiento(UUID programaId, String buscar, int limite) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT p.id, p.apellido_paterno, p.apellido_materno, p.nombres, "
                + "e.codigo_sistema, prog.nombre, te.id, te.titulo, li.nombre, te.estado, "
                + TUTOR + ", " + ASESOR + ", "
                + "CASE WHEN EXISTS (SELECT 1 FROM dictamenes_designacion d WHERE d.tesis_id = te.id "
                + "  AND d.estado = 'FIRMADO' AND d.active = true) THEN 1 ELSE 0 END, "
                + "COALESCE(pr.listo_revision, false), COALESCE(pr.carta_asesor, false), "
                + "COALESCE(pr.expediente_subido, false), COALESCE(pr.expediente_recibido, false), "
                + "COALESCE(pr.revisores_conformes, false), COALESCE(pr.defensa_programada, false), "
                + "COALESCE(pr.informe_final_aprobado, false), COALESCE(pr.jurado_informante_solicitado, false), "
                + "COALESCE(pr.informe_final_revisado, false), "
                + "(SELECT COUNT(*) FROM proyecto_revisores rv WHERE rv.proyecto_id = pr.id AND rv.active = true), "
                + "CASE WHEN EXISTS (SELECT 1 FROM documentos_tesis dt WHERE dt.tesis_id = te.id "
                + "  AND dt.tipo = 'RUBRICA_REVISOR' AND dt.active = true) THEN 1 ELSE 0 END, "
                + "(SELECT COUNT(*) FROM proyecto_informe_revisores ir WHERE ir.proyecto_id = pr.id AND ir.active = true), "
                + "pr.fecha_defensa, "
                // fechas de los hitos (para "hace cuánto no se mueve")
                + "te.fecha_registro, " + TUTORIA_INICIO + ", " + DICTAMEN_EMISION + ", "
                + "pr.fecha_listo_revision, pr.fecha_carta_asesor, pr.fecha_solicitud_aprobacion, "
                + "pr.fecha_recepcion, pr.fecha_revisores_conformes, pr.fecha_informe_final, "
                + "pr.fecha_jurado_informante, pr.fecha_informe_revisado "
                + FROM + where(programaId, buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :limite";
        Query q = em.createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("limite", limite);
        return q.getResultList();
    }

    private String where(UUID programaId, String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE p.active = true ");
        if (programaId != null) {
            params.put("programaId", programaId);
            w.append(" AND e.programa_id = :programaId ");
        }
        if (buscar != null && !buscar.isBlank()) {
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(e.codigo_sistema) LIKE :buscar ")
             .append(" OR LOWER(te.titulo) LIKE :buscar) ");
        }
        return w.toString();
    }
}
