package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.repositories.ProgramaPosgradoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProgramaPosgradoRepositoryImpl
        implements ProgramaPosgradoRepository, PanacheRepositoryBase<ProgramaPosgrado, UUID> {

    @Override
    public List<ProgramaPosgrado> getAll() {
        return listAll(Sort.by("nombre").ascending());
    }

    @Override
    public List<ProgramaPosgrado> getByFacultad(UUID facultadId) {
        if (facultadId == null) {
            return getAll();
        }
        return list("facultad.id = ?1", Sort.by("nombre").ascending(), facultadId);
    }

    @Override
    public Optional<ProgramaPosgrado> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public ProgramaPosgrado save(ProgramaPosgrado programa) {
        persist(programa);
        return programa;
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return nombre != null && !nombre.isBlank() && count("nombre = ?1", nombre.trim()) > 0;
    }
}
