package unmsm.edu.pe.configuracion.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.configuracion.domain.entities.Facultad;
import unmsm.edu.pe.configuracion.domain.repositories.FacultadRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FacultadRepositoryImpl implements FacultadRepository, PanacheRepositoryBase<Facultad, UUID> {

    @Override
    public Facultad save(Facultad facultad) {
        persist(facultad);
        return facultad;
    }

    @Override
    public Optional<Facultad> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<Facultad> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return find("active = true and lower(codigo) = ?1", codigo.trim().toLowerCase()).firstResultOptional();
    }

    @Override
    public List<Facultad> listar(String search, int page, int size) {
        Sort sort = Sort.by("nombre").ascending();
        if (search == null || search.isBlank()) {
            return find("active = true", sort).page(Page.of(page, size)).list();
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return find("active = true and (lower(nombre) like ?1 or lower(codigo) like ?1)", sort, q)
                .page(Page.of(page, size)).list();
    }

    @Override
    public long contar(String search) {
        if (search == null || search.isBlank()) {
            return count("active = true");
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return count("active = true and (lower(nombre) like ?1 or lower(codigo) like ?1)", q);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return nombre != null && !nombre.isBlank()
                && count("active = true and lower(nombre) = ?1", nombre.trim().toLowerCase()) > 0;
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return codigo != null && !codigo.isBlank()
                && count("active = true and lower(codigo) = ?1", codigo.trim().toLowerCase()) > 0;
    }
}
