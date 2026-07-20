package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoAvance;

import java.util.List;
import java.util.UUID;

public interface ProyectoAvanceRepository {
    ProyectoAvance save(ProyectoAvance avance);
    List<ProyectoAvance> listarPorProyecto(UUID proyectoId);
}
