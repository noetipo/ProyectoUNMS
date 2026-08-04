package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;
import unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SolicitudAsesoriaRepositoryImpl
        implements SolicitudAsesoriaRepository, PanacheRepositoryBase<SolicitudAsesoria, UUID> {

    /**
     * Para decidir, el asesor necesita el TEMA del doctorando, no solo el título tentativo de la
     * solicitud: se traen el tema registrado (título y resumen), el nivel del programa y el tutor
     * que lo sugirió. Las subconsultas usan la tesis activa del estudiante.
     */
    private static final String SELECT_BANDEJA =
            "select s.id, e.personaId, e.persona.nombres, e.persona.apellidoPaterno, e.persona.apellidoMaterno, "
                    + "e.codigoSistema, prog.nombre, li.nombre, s.tituloTentativo, s.tipo, s.estado, "
                    + "s.fechaSolicitud, s.fechaRespuesta, s.motivoRespuesta, s.mensaje, "
                    + "prog.nivel, "
                    + "(select t.titulo from Tesis t, TesisAutor ta where ta.tesisId = t.id "
                    + "   and ta.estudianteId = e.personaId and ta.esActiva = true and ta.active = true and t.active = true), "
                    + "(select t.resumen from Tesis t, TesisAutor ta where ta.tesisId = t.id "
                    + "   and ta.estudianteId = e.personaId and ta.esActiva = true and ta.active = true and t.active = true), "
                    + "(select concat(tu.docente.persona.apellidoPaterno, ', ', tu.docente.persona.nombres) "
                    + "   from Tutoria tu where tu.estudiante.personaId = e.personaId "
                    + "   and tu.actual = true and tu.active = true) "
                    + "from SolicitudAsesoria s join s.estudiante e "
                    + "left join e.programa prog left join s.lineaInvestigacion li ";

    private static final String SELECT_MIAS =
            "select s.id, d.personaId, d.persona.nombres, d.persona.apellidoPaterno, d.persona.apellidoMaterno, "
                    + "d.codigoSistema, (select g.grado from PersonaGradoAcademico g where g.persona = d.persona and g.principal = true and g.active = true), li.nombre, s.tituloTentativo, s.tipo, s.estado, "
                    + "s.fechaSolicitud, s.fechaRespuesta, s.motivoRespuesta, s.mensaje "
                    + "from SolicitudAsesoria s join s.docente d "
                    + "left join s.lineaInvestigacion li ";

    @Override
    public SolicitudAsesoria save(SolicitudAsesoria solicitud) {
        if (solicitud.getId() == null) {
            persist(solicitud);
            return solicitud;
        }
        return getEntityManager().merge(solicitud);
    }

    @Override
    public Optional<SolicitudAsesoria> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public boolean existePendientePorEstudiante(UUID estudianteId) {
        return count("estudiante.personaId = ?1 and estado = ?2 and active = true",
                estudianteId, EstadoSolicitud.PENDIENTE) > 0;
    }

    @Override
    public Optional<SolicitudAsesoria> ultimaDeEstudiante(UUID estudianteId) {
        return find("estudiante.personaId = ?1 and active = true order by fechaSolicitud desc", estudianteId)
                .firstResultOptional();
    }

    @Override
    public Optional<SolicitudAsesoria> ultimaDeEstudiantePorTipo(UUID estudianteId, TipoAsesoria tipo) {
        // Las solicitudes antiguas (previas al co-asesor) no tienen tipo: cuentan como ASESOR.
        String filtroTipo = tipo == TipoAsesoria.ASESOR ? "(s.tipo = ?2 or s.tipo is null)" : "s.tipo = ?2";
        return find("from SolicitudAsesoria s where s.estudiante.personaId = ?1 and " + filtroTipo
                + " and s.active = true order by s.fechaSolicitud desc", estudianteId, tipo)
                .firstResultOptional();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarPorDocente(UUID docenteId, EstadoSolicitud estado, int page, int size) {
        Query q = getEntityManager().createQuery(SELECT_BANDEJA
                + "where s.docente.personaId = :docenteId and s.estado = :estado and s.active = true "
                + "order by s.fechaSolicitud desc");
        q.setParameter("docenteId", docenteId);
        q.setParameter("estado", estado);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }

    @Override
    public long contarPorDocente(UUID docenteId, EstadoSolicitud estado) {
        return count("docente.personaId = ?1 and estado = ?2 and active = true", docenteId, estado);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> listarPorEstudiante(UUID estudianteId, EstadoSolicitud estado, int page, int size) {
        String jpql = SELECT_MIAS + "where s.estudiante.personaId = :estudianteId and s.active = true "
                + (estado != null ? "and s.estado = :estado " : "")
                + "order by s.fechaSolicitud desc";
        Query q = getEntityManager().createQuery(jpql);
        q.setParameter("estudianteId", estudianteId);
        if (estado != null) {
            q.setParameter("estado", estado);
        }
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }

    @Override
    public long contarPorEstudiante(UUID estudianteId, EstadoSolicitud estado) {
        if (estado != null) {
            return count("estudiante.personaId = ?1 and estado = ?2 and active = true", estudianteId, estado);
        }
        return count("estudiante.personaId = ?1 and active = true", estudianteId);
    }
}
