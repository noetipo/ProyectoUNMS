package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.DocumentoPersona;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.repositories.DocumentoPersonaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DocumentoPersonaRepositoryImpl implements DocumentoPersonaRepository, PanacheRepositoryBase<DocumentoPersona, UUID> {

    @Override
    public DocumentoPersona save(DocumentoPersona documento) {
        persist(documento);
        return documento;
    }

    @Override
    public Optional<DocumentoPersona> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public List<DocumentoPersona> findByPersonaId(UUID personaId) {
        return list("persona.id = ?1", personaId);
    }

    @Override
    public Optional<DocumentoPersona> findByPersonaIdAndTipo(UUID personaId, TipoDocumento tipo) {
        return find("persona.id = ?1 and tipoDocumento = ?2", personaId, tipo).firstResultOptional();
    }

    @Override
    public boolean existsByPersonaIdAndTipo(UUID personaId, TipoDocumento tipo) {
        return count("persona.id = ?1 and tipoDocumento = ?2", personaId, tipo) > 0;
    }
}
