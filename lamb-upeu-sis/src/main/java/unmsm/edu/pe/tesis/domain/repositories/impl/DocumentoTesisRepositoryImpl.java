package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.DocumentoTesis;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DocumentoTesisRepositoryImpl implements DocumentoTesisRepository, PanacheRepositoryBase<DocumentoTesis, UUID> {

    static final String SOLICITUD_FIRMADA = "SOLICITUD_ASESORIA_FIRMADA";
    static final String CARTA_FIRMADA = "CARTA_ACEPTACION_FIRMADA";

    @Override
    public DocumentoTesis save(DocumentoTesis doc) {
        if (doc.getId() == null) {
            persist(doc);
            return doc;
        }
        return getEntityManager().merge(doc);
    }

    @Override
    public Optional<DocumentoTesis> buscarPorTesisYTipo(UUID tesisId, String tipo) {
        return find("tesisId = ?1 and tipo = ?2 and active = true", tesisId, tipo).firstResultOptional();
    }

    @Override
    public List<DocumentoTesis> listarPorTesis(UUID tesisId) {
        return list("tesisId = ?1 and active = true", Sort.by("tipo"), tesisId);
    }

    @Override
    public boolean existePorTesisYTipo(UUID tesisId, String tipo) {
        return count("tesisId = ?1 and tipo = ?2 and active = true", tesisId, tipo) > 0;
    }

    @Override
    public boolean tieneFirmadosCompletos(UUID tesisId) {
        return existePorTesisYTipo(tesisId, SOLICITUD_FIRMADA) && existePorTesisYTipo(tesisId, CARTA_FIRMADA);
    }
}
