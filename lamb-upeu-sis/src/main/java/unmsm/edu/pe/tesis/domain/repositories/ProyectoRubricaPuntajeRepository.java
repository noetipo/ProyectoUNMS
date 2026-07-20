package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoRubricaPuntaje;

import java.util.List;
import java.util.UUID;

public interface ProyectoRubricaPuntajeRepository {
    ProyectoRubricaPuntaje save(ProyectoRubricaPuntaje puntaje);
    List<ProyectoRubricaPuntaje> listarPorRevisor(UUID revisorId);
    /** Elimina (físico) los puntajes previos de un revisor, para re-evaluar. */
    void eliminarPorRevisor(UUID revisorId);
}
