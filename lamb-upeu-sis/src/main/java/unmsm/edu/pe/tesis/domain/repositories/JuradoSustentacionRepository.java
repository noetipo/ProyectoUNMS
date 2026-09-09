package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.JuradoSustentacion;

import java.util.List;
import java.util.UUID;

public interface JuradoSustentacionRepository {
    JuradoSustentacion save(JuradoSustentacion jurado);

    List<JuradoSustentacion> listarPorProyecto(UUID proyectoId);

    long contarPorProyecto(UUID proyectoId);
}
