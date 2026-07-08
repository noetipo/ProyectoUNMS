package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoTesisRepository {
    DocumentoTesis save(DocumentoTesis doc);
    Optional<DocumentoTesis> buscarPorTesisYTipo(UUID tesisId, String tipo);
    List<DocumentoTesis> listarPorTesis(UUID tesisId);
    boolean existePorTesisYTipo(UUID tesisId, String tipo);
    /** True si la tesis tiene subidos los dos firmados del estudiante. */
    boolean tieneFirmadosCompletos(UUID tesisId);
}
