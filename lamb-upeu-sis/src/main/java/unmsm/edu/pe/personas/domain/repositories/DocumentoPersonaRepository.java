package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.DocumentoPersona;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoPersonaRepository {
    DocumentoPersona save(DocumentoPersona documento);
    Optional<DocumentoPersona> buscarPorId(UUID id);
    List<DocumentoPersona> findByPersonaId(UUID personaId);
    Optional<DocumentoPersona> findByPersonaIdAndTipo(UUID personaId, TipoDocumento tipo);
    boolean existsByPersonaIdAndTipo(UUID personaId, TipoDocumento tipo);
}
