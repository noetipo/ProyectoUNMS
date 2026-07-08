package unmsm.edu.pe.configuracion.domain.repositories;

import unmsm.edu.pe.configuracion.domain.entities.LemaAnual;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LemaAnualRepository {
    LemaAnual save(LemaAnual lema);
    Optional<LemaAnual> buscarPorId(UUID id);
    /** Lista TODOS los lemas (vigentes e inactivos) para poder alternar su estado. */
    List<LemaAnual> listar(String search, int page, int size);
    long contar(String search);
    /** Lema actualmente VIGENTE (active = true) de un año, si existe. */
    Optional<LemaAnual> buscarActivoPorAnio(Integer anio);
    /** Lema vigente de un año, hoy (para plantillas: "nombre del año"). */
    boolean existeActivoPorAnio(Integer anio);
    /** Fuerza el flush (aplicar la baja del vigente antes de activar otro). */
    void flushCambios();
}
