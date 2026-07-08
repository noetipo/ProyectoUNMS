package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.ProgramaPosgradoResponse;

import java.util.List;
import java.util.UUID;

public interface ProgramaPosgradoService {
    /** Lista programas; si facultadId no es null, solo los de esa facultad. */
    List<ProgramaPosgradoResponse> list(UUID facultadId);
}
