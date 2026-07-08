package unmsm.edu.pe.security.domain.services;

import unmsm.edu.pe.security.application.dto.MenuDto;
import unmsm.edu.pe.security.application.dto.ModuleDto;
import unmsm.edu.pe.security.application.dto.ModuleSelectedDto;
import unmsm.edu.pe.security.application.dto.PaginatedResponseDto;
// ✅ IMPORTACIÓN EXPLÍCITA
import unmsm.edu.pe.security.domain.entities.Module;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleService {
    PaginatedResponseDto<Module> list(Integer page, Integer size, String titleFilter);
    Module save(ModuleDto moduleDto);
    Optional<Module> findById(UUID id);
    Optional<Module> findByCode(String code);
    Module update(UUID id, ModuleDto moduleDto);
    void delete(UUID id);
    List<ModuleSelectedDto> findAllModulesAsignetToRol(UUID roleId, UUID parentModuleId);
    List<MenuDto> listMenu();

    List<MenuDto> listMenuByRole(UUID roleId);
}