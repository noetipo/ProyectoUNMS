package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoJurado;

import java.util.List;
import java.util.UUID;

public interface ProyectoJuradoRepository {
    ProyectoJurado save(ProyectoJurado jurado);
    List<ProyectoJurado> listarPorProyecto(UUID proyectoId);
    long contarPorProyecto(UUID proyectoId);
}
