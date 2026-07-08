package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.Tesis;

import java.util.Optional;
import java.util.UUID;

public interface TesisRepository {
    Tesis save(Tesis tesis);

    Optional<Tesis> buscarPorId(UUID id);

    /** Tesis vigente del estudiante (vía tesis_autores.es_activa = true), si existe. */
    Optional<Tesis> tesisActivaDeEstudiante(UUID estudianteId);

    /** True si el estudiante ya tiene una tesis activa (no puede tener dos). */
    boolean existeTesisActiva(UUID estudianteId);

    /** True si la tesis ya tiene un asesor (asesoría tipo ASESOR activa). */
    boolean tieneAsesor(UUID tesisId);
}
