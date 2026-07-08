package unmsm.edu.pe.configuracion.domain.repositories;

import unmsm.edu.pe.configuracion.domain.entities.ParametroSistema;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParametroSistemaRepository {
    ParametroSistema save(ParametroSistema parametro);
    Optional<ParametroSistema> buscarPorId(UUID id);
    Optional<ParametroSistema> buscarPorClave(String clave);
    List<ParametroSistema> listar(String search, int page, int size);
    long contar(String search);
    boolean existsByClave(String clave);
}
