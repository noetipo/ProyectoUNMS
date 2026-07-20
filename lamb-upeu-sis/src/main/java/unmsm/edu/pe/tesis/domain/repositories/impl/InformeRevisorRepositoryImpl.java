package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;
import unmsm.edu.pe.tesis.domain.repositories.InformeRevisorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InformeRevisorRepositoryImpl
        implements InformeRevisorRepository, PanacheRepositoryBase<InformeRevisor, UUID> {

    @Override
    public InformeRevisor save(InformeRevisor revisor) {
        if (revisor.getId() == null) {
            persist(revisor);
            return revisor;
        }
        return getEntityManager().merge(revisor);
    }

    @Override
    public Optional<InformeRevisor> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public List<InformeRevisor> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public long contarPorProyecto(UUID proyectoId) {
        return count("proyectoId = ?1 and active = true", proyectoId);
    }

    @Override
    public Optional<InformeRevisor> buscarPorProyectoYDocente(UUID proyectoId, UUID docenteId) {
        return find("proyectoId = ?1 and docenteId = ?2 and active = true", proyectoId, docenteId)
                .firstResultOptional();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandejaDeJurado(UUID docenteId) {
        return getEntityManager().createNativeQuery("""
                SELECT pr.tesis_id, pr.id, rv.id, rv.estado, rv.presidente,
                       p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema,
                       prog.nombre, te.titulo
                FROM proyecto_informe_revisores rv
                JOIN proyectos_tesis pr ON pr.id = rv.proyecto_id
                JOIN tesis te ON te.id = pr.tesis_id
                LEFT JOIN tesis_autores ta ON ta.tesis_id = te.id AND ta.es_activa = true AND ta.active = true
                LEFT JOIN estudiantes e ON e.persona_id = ta.estudiante_id
                LEFT JOIN persona p ON p.id = e.persona_id
                LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
                WHERE rv.active = true AND rv.docente_id = :doc AND pr.active = true
                ORDER BY te.titulo
                """)
                .setParameter("doc", docenteId)
                .getResultList();
    }
}
