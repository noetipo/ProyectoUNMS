package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.CentroLaboral;
import unmsm.edu.pe.personas.domain.repositories.CentroLaboralRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CentroLaboralRepositoryImpl implements CentroLaboralRepository, PanacheRepositoryBase<CentroLaboral, UUID> {

    @Override
    public CentroLaboral save(CentroLaboral centro) {
        persist(centro);
        return centro;
    }

    @Override
    public Optional<CentroLaboral> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public List<CentroLaboral> listar(String search, int page, int size) {
        Sort sort = Sort.by("nombre").ascending();
        if (search == null || search.isBlank()) {
            return find("active = true", sort).page(Page.of(page, size)).list();
        }
        return find("active = true and lower(nombre) like ?1", sort, "%" + search.trim().toLowerCase() + "%")
                .page(Page.of(page, size)).list();
    }

    @Override
    public long contar(String search) {
        if (search == null || search.isBlank()) {
            return count("active = true");
        }
        return count("active = true and lower(nombre) like ?1", "%" + search.trim().toLowerCase() + "%");
    }

    @Override
    public long contarTotal() {
        return count();
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return nombre != null && !nombre.isBlank()
                && count("active = true and lower(nombre) = ?1", nombre.trim().toLowerCase()) > 0;
    }
}
