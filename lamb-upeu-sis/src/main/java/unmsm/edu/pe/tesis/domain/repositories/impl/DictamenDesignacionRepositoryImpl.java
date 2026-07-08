package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.tesis.domain.entities.DictamenDesignacion;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DictamenDesignacionRepositoryImpl
        implements DictamenDesignacionRepository, PanacheRepositoryBase<DictamenDesignacion, UUID> {

    @Override
    public DictamenDesignacion save(DictamenDesignacion dictamen) {
        if (dictamen.getId() == null) {
            persist(dictamen);
            return dictamen;
        }
        return getEntityManager().merge(dictamen);
    }

    @Override
    public Optional<DictamenDesignacion> buscarPorTesisId(UUID tesisId) {
        return find("tesisId = ?1 and active = true", tesisId).firstResultOptional();
    }

    @Override
    public int siguienteCorrelativo(int anio) {
        // Upsert atómico: crea el contador del año o lo incrementa; devuelve el nuevo valor.
        Query q = getEntityManager().createNativeQuery(
                "INSERT INTO contador_dictamen (anio, correlativo) VALUES (:anio, 1) "
                        + "ON CONFLICT (anio) DO UPDATE SET correlativo = contador_dictamen.correlativo + 1 "
                        + "RETURNING correlativo");
        q.setParameter("anio", anio);
        return ((Number) q.getSingleResult()).intValue();
    }

    private static final String BANDEJA_FROM = """
            FROM dictamenes_designacion d
            JOIN tesis te ON te.id = d.tesis_id
            LEFT JOIN tesis_autores ta ON ta.tesis_id = te.id AND ta.es_activa = true AND ta.active = true
            LEFT JOIN estudiantes e ON e.persona_id = ta.estudiante_id
            LEFT JOIN persona p ON p.id = e.persona_id
            LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
            """;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandeja(String estado, UUID facultadId, UUID programaId, String buscar, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT d.tesis_id, p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema, "
                + "prog.nombre, te.titulo, d.estado, d.numero, e.programa_id "
                + BANDEJA_FROM + where(estado, facultadId, programaId, buscar, params)
                + " ORDER BY p.apellido_paterno ASC, p.nombres ASC LIMIT :size OFFSET :offset";
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.setParameter("size", size);
        q.setParameter("offset", page * size);
        return q.getResultList();
    }

    @Override
    public long contarBandeja(String estado, UUID facultadId, UUID programaId, String buscar) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) " + BANDEJA_FROM + where(estado, facultadId, programaId, buscar, params);
        Query q = getEntityManager().createNativeQuery(sql);
        params.forEach(q::setParameter);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public long contarPorEstado(String estado) {
        return count("estado = ?1 and active = true", unmsm.edu.pe.tesis.domain.enums.EstadoDictamen.valueOf(estado));
    }

    private String where(String estado, UUID facultadId, UUID programaId, String buscar, Map<String, Object> params) {
        StringBuilder w = new StringBuilder(" WHERE d.active = true ");
        if (estado != null && !estado.isBlank()) {
            w.append(" AND d.estado = :estado ");
            params.put("estado", estado.trim());
        }
        if (facultadId != null) {
            w.append(" AND prog.facultad_id = :facultadId ");
            params.put("facultadId", facultadId);
        }
        if (programaId != null) {
            w.append(" AND e.programa_id = :programaId ");
            params.put("programaId", programaId);
        }
        if (buscar != null && !buscar.isBlank()) {
            w.append(" AND (LOWER(p.nombres) LIKE :buscar OR LOWER(p.apellido_paterno) LIKE :buscar ")
             .append(" OR LOWER(p.apellido_materno) LIKE :buscar OR LOWER(e.codigo_sistema) LIKE :buscar) ");
            params.put("buscar", "%" + buscar.trim().toLowerCase() + "%");
        }
        return w.toString();
    }
}
