package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.SugerenciaAsesor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SugerenciaAsesorRepository {
    SugerenciaAsesor save(SugerenciaAsesor sugerencia);

    Optional<SugerenciaAsesor> buscarPorId(UUID id);

    /** Sugerencias activas de un estudiante (la terna que ve el estudiante). */
    List<SugerenciaAsesor> listarPorEstudiante(UUID estudianteId);

    /** ¿Ya se sugirió ese asesor a ese estudiante (activa)? (evita duplicados) */
    boolean existe(UUID estudianteId, UUID asesorDocenteId);
}
