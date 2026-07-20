package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRubricaPuntaje;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRubricaPuntajeRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProyectoRubricaPuntajeRepositoryImpl
        implements ProyectoRubricaPuntajeRepository, PanacheRepositoryBase<ProyectoRubricaPuntaje, UUID> {

    @Override
    public ProyectoRubricaPuntaje save(ProyectoRubricaPuntaje puntaje) {
        if (puntaje.getId() == null) {
            persist(puntaje);
            return puntaje;
        }
        return getEntityManager().merge(puntaje);
    }

    @Override
    public List<ProyectoRubricaPuntaje> listarPorRevisor(UUID revisorId) {
        return list("revisorId = ?1 and active = true", revisorId);
    }

    @Override
    public void eliminarPorRevisor(UUID revisorId) {
        delete("revisorId = ?1", revisorId);
    }
}
