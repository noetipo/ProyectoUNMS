package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.TesisAutor;

import java.util.UUID;

public interface TesisAutorRepository {
    TesisAutor save(TesisAutor autor);

    UUID tesisActivaId(UUID estudianteId);

    /** Estudiante (persona_id) autor activo de una tesis. */
    UUID estudianteDeTesis(UUID tesisId);
}
