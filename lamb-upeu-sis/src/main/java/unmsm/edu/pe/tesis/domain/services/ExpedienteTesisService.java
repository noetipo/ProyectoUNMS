package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.ExpedienteResponse;

import java.util.UUID;

/** Arma el expediente de tesis (línea de tiempo de las 8 etapas del proceso). */
public interface ExpedienteTesisService {

    /** Expediente del estudiante autenticado. */
    ExpedienteResponse miExpediente();

    /** Expediente de una tesis (secretaría / admin). */
    ExpedienteResponse expediente(UUID tesisId);
}
