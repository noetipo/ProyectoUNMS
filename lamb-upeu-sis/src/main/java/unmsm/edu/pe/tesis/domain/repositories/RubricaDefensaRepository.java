package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.RubricaDefensa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RubricaDefensaRepository {
    RubricaDefensa save(RubricaDefensa rubrica);

    /** Las rúbricas de defensa recepcionadas de una tesis. */
    List<RubricaDefensa> porTesis(UUID tesisId);

    Optional<RubricaDefensa> buscarPorTesisYDocente(UUID tesisId, UUID docenteId);

    /** Da de baja (soft-delete) las rúbricas de una defensa desaprobada, antes de reprogramar un nuevo acto. */
    void eliminarPorTesis(UUID tesisId);
}
