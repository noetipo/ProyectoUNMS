package unmsm.edu.pe.security.domain.repositories;

import io.quarkus.panache.common.Page;
import unmsm.edu.pe.security.domain.entities.ParentModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentModuleRepository {
    // ⚠️ Cambiamos nombres para evitar conflicto con PanacheRepositoryBase
    List<ParentModule> getAllParentModules();
    Optional<ParentModule> getParentModuleById(UUID id);
    Optional<ParentModule> findByCode(String code);
    ParentModule save(ParentModule parentModule);
    Optional<ParentModule> update(UUID id, ParentModule parentModule);
    void remove(ParentModule parentModule);
    // ✅ Agregar este método
    long count();
    // Métodos con filtros y paginación
    List<ParentModule> findWithFilters(String nameFilter, Page page);
    long countWithFilters(String nameFilter);

    // Método para listar sin paginación con filtros
    List<ParentModule> findAllActiveWithFilters(String nameFilter);
}
