package unmsm.edu.pe.configuracion.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.configuracion.domain.entities.ParametroSistema;
import unmsm.edu.pe.configuracion.domain.repositories.ParametroSistemaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ParametroSistemaRepositoryImpl
        implements ParametroSistemaRepository, PanacheRepositoryBase<ParametroSistema, UUID> {

    @Override
    public ParametroSistema save(ParametroSistema parametro) {
        persist(parametro);
        return parametro;
    }

    @Override
    public Optional<ParametroSistema> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<ParametroSistema> buscarPorClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return Optional.empty();
        }
        return find("active = true and lower(clave) = ?1", clave.trim().toLowerCase()).firstResultOptional();
    }

    @Override
    public List<ParametroSistema> listar(String search, int page, int size) {
        Sort sort = Sort.by("clave").ascending();
        if (search == null || search.isBlank()) {
            return find("active = true", sort).page(Page.of(page, size)).list();
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return find("active = true and (lower(clave) like ?1 or lower(descripcion) like ?1)", sort, q)
                .page(Page.of(page, size)).list();
    }

    @Override
    public long contar(String search) {
        if (search == null || search.isBlank()) {
            return count("active = true");
        }
        String q = "%" + search.trim().toLowerCase() + "%";
        return count("active = true and (lower(clave) like ?1 or lower(descripcion) like ?1)", q);
    }

    @Override
    public boolean existsByClave(String clave) {
        return clave != null && !clave.isBlank()
                && count("active = true and lower(clave) = ?1", clave.trim().toLowerCase()) > 0;
    }
}
