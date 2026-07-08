package unmsm.edu.pe.configuracion.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.configuracion.domain.entities.LemaAnual;
import unmsm.edu.pe.configuracion.domain.repositories.LemaAnualRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LemaAnualRepositoryImpl implements LemaAnualRepository, PanacheRepositoryBase<LemaAnual, UUID> {

    @Override
    public LemaAnual save(LemaAnual lema) {
        persist(lema);
        return lema;
    }

    @Override
    public Optional<LemaAnual> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public List<LemaAnual> listar(String search, int page, int size) {
        // Muestra vigentes e inactivos, año descendente; se filtra solo lo borrado nunca (no hay borrado físico).
        Sort sort = Sort.by("anio", "createdAt").descending();
        if (search == null || search.isBlank()) {
            return findAll(sort).page(Page.of(page, size)).list();
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return find("lower(texto) like ?1 or cast(anio as string) like ?1", sort, q)
                .page(Page.of(page, size)).list();
    }

    @Override
    public long contar(String search) {
        if (search == null || search.isBlank()) {
            return count();
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return count("lower(texto) like ?1 or cast(anio as string) like ?1", q);
    }

    @Override
    public Optional<LemaAnual> buscarActivoPorAnio(Integer anio) {
        if (anio == null) {
            return Optional.empty();
        }
        return find("active = true and anio = ?1", anio).firstResultOptional();
    }

    @Override
    public boolean existeActivoPorAnio(Integer anio) {
        return anio != null && count("active = true and anio = ?1", anio) > 0;
    }

    @Override
    public void flushCambios() {
        flush();
    }
}
