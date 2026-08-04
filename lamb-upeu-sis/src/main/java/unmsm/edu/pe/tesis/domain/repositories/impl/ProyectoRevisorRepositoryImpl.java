package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoRevisorRepositoryImpl
        implements ProyectoRevisorRepository, PanacheRepositoryBase<ProyectoRevisor, UUID> {

    @Override
    public ProyectoRevisor save(ProyectoRevisor revisor) {
        if (revisor.getId() == null) {
            persist(revisor);
            return revisor;
        }
        return getEntityManager().merge(revisor);
    }

    @Override
    public Optional<ProyectoRevisor> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public List<ProyectoRevisor> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public long contarPorProyecto(UUID proyectoId) {
        return count("proyectoId = ?1 and active = true", proyectoId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> docentesOpcion() {
        return getEntityManager().createQuery(
                        "select d.personaId, p.nombres, p.apellidoPaterno, p.apellidoMaterno, d.categoria "
                                + "from Docente d join d.persona p where p.active = true "
                                + "order by p.apellidoPaterno, p.apellidoMaterno")
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> docentesOpcionPorLinea(UUID lineaId) {
        return getEntityManager().createQuery(
                        "select distinct d.personaId, p.nombres, p.apellidoPaterno, p.apellidoMaterno, d.categoria "
                                + "from DocenteLineaInvestigacion dli join dli.docente d join d.persona p "
                                + "where dli.lineaInvestigacion.id = :lineaId and dli.active = true and p.active = true "
                                + "order by p.apellidoPaterno, p.apellidoMaterno")
                .setParameter("lineaId", lineaId)
                .getResultList();
    }

    @Override
    public Optional<ProyectoRevisor> buscarPorProyectoYDocente(UUID proyectoId, UUID docenteId) {
        return find("proyectoId = ?1 and docenteId = ?2 and active = true", proyectoId, docenteId)
                .firstResultOptional();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> bandejaDeRevisor(UUID docenteId) {
        return getEntityManager().createNativeQuery("""
                SELECT pr.tesis_id, pr.id, rv.id, rv.estado, rv.puntaje_total,
                       p.apellido_paterno, p.apellido_materno, p.nombres, e.codigo_sistema,
                       prog.nombre, te.titulo, pr.fecha_recepcion
                FROM proyecto_revisores rv
                JOIN proyectos_tesis pr ON pr.id = rv.proyecto_id
                JOIN tesis te ON te.id = pr.tesis_id
                LEFT JOIN tesis_autores ta ON ta.tesis_id = te.id AND ta.es_activa = true AND ta.active = true
                LEFT JOIN estudiantes e ON e.persona_id = ta.estudiante_id
                LEFT JOIN persona p ON p.id = e.persona_id
                LEFT JOIN programas_posgrado prog ON prog.id = e.programa_id
                WHERE rv.active = true AND rv.docente_id = :doc AND pr.active = true
                ORDER BY pr.fecha_recepcion DESC NULLS LAST
                """)
                .setParameter("doc", docenteId)
                .getResultList();
    }
}

