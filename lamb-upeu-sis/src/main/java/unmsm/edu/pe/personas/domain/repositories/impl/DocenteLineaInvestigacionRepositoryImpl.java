package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocenteLineaInvestigacionRepositoryImpl
        implements DocenteLineaInvestigacionRepository, PanacheRepositoryBase<DocenteLineaInvestigacion, UUID> {

    @Override
    public List<DocenteLineaInvestigacion> findByDocenteId(UUID docenteId) {
        return list("docente.personaId = ?1", Sort.by("createdAt").ascending(), docenteId);
    }

    @Override
    public List<DocenteLineaInvestigacion> saveAll(List<DocenteLineaInvestigacion> lineas) {
        persist(lineas);
        return lineas;
    }

    @Override
    public void deleteByDocenteId(UUID docenteId) {
        delete("docente.personaId = ?1", docenteId);
    }

    @Override
    public boolean existsByDocenteAndLinea(UUID docenteId, UUID lineaId) {
        return count("docente.personaId = ?1 and lineaInvestigacion.id = ?2 and active = true",
                docenteId, lineaId) > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarDocentesPorLinea(UUID lineaId) {
        return getEntityManager().createQuery(
                        "select d.personaId, d.persona.apellidoPaterno, d.persona.apellidoMaterno, "
                                + "d.persona.nombres, d.codigoSistema, "
                                + "(select g.grado from PersonaGradoAcademico g where g.persona = d.persona and g.principal = true and g.active = true) "
                                + "from DocenteLineaInvestigacion dli join dli.docente d "
                                + "where dli.lineaInvestigacion.id = :lineaId and dli.active = true "
                                + "and d.persona.active = true "
                                + "order by d.persona.apellidoPaterno, d.persona.apellidoMaterno")
                .setParameter("lineaId", lineaId)
                .getResultList();
    }
}
