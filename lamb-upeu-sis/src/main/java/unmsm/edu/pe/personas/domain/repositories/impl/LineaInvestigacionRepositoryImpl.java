package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LineaInvestigacionRepositoryImpl
        implements LineaInvestigacionRepository, PanacheRepositoryBase<LineaInvestigacion, UUID> {

    @Override
    public LineaInvestigacion save(LineaInvestigacion linea) {
        persist(linea);
        return linea;
    }

    @Override
    public Optional<LineaInvestigacion> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public List<LineaInvestigacion> listar(String search, int page, int size) {
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
