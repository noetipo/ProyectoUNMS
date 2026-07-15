package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoCampo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoCampoRepository {
    ProyectoCampo save(ProyectoCampo campo);
    Optional<ProyectoCampo> buscarPorProyectoYClave(UUID proyectoId, String clave);
    List<ProyectoCampo> listarPorProyecto(UUID proyectoId);
}
