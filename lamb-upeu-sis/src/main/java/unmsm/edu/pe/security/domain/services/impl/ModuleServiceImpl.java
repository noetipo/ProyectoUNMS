package unmsm.edu.pe.security.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import unmsm.edu.pe.security.application.dto.MenuDto;
import unmsm.edu.pe.security.application.dto.ModuleDto;
import unmsm.edu.pe.security.application.dto.ModuleSelectedDto;
import unmsm.edu.pe.security.application.dto.PaginatedResponseDto;
import unmsm.edu.pe.security.application.mapper.ModuleMapper;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.repositories.ModuleRepository;
import unmsm.edu.pe.security.domain.services.ModuleService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ModuleServiceImpl implements ModuleService {

    @Inject
    ModuleRepository moduleRepository;

    @Inject
    ModuleMapper moduleMapper;

    @Inject
    SecurityUtils securityUtils;

    @Override
    public PaginatedResponseDto<Module> list(Integer page, Integer size, String titleFilter) {
        var query = moduleRepository.findWithFilters(titleFilter, null);
        var paginatedQuery = query.page(io.quarkus.panache.common.Page.of(page, size));

        return PaginatedResponseDto.<Module>builder()
                .content(paginatedQuery.list())
                .totalElements(query.count())
                .totalPages(paginatedQuery.pageCount())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    @Override
    @Transactional
    public Module save(ModuleDto moduleDto) {
        Module module = moduleMapper.toEntity(moduleDto);
        moduleRepository.save(module);
        return module;
    }

    @Override
    public Optional<Module> findById(UUID id) {
        return moduleRepository.findByIdOptional(id);
    }

    @Override
    public Optional<Module> findByCode(String code) {
        return moduleRepository.findByCode(code);
    }

    @Override
    @Transactional
    public Module update(UUID id, ModuleDto moduleDto) {
        Module existingModule = moduleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Module not found with ID: " + id));

        moduleMapper.updateModuleFromDto(moduleDto, existingModule);
        moduleRepository.save(existingModule);

        return existingModule;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Module module = moduleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Module not found with ID: " + id));

        module.markAsDeleted();
        moduleRepository.save(module);
    }

    @Override
    public List<ModuleSelectedDto> findAllModulesAsignetToRol(UUID roleId, UUID parentModuleId) {
        List<Object[]> results = moduleRepository.findAllModulesAssignedToRole(roleId, parentModuleId);
        return results.stream()
                .map(this::mapToModuleSelectedDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MenuDto> listMenuByRole(UUID roleId) {
        List<Object[]> results = moduleRepository.findAllMenuParentByRole(roleId);
        List<MenuDto> menuDtoList = results.stream()
                .map(this::mapToMenuDto)
                .collect(Collectors.toList());

        for (MenuDto menuDto : menuDtoList) {
            List<Object[]> children = moduleRepository.findAllMenuByRole(roleId, menuDto.getId());
            menuDto.setChildren(children.stream().map(this::mapToModuleDto).collect(Collectors.toList()));
        }

        return menuDtoList;
    }

    @Override
    public List<MenuDto> listMenu() {
        // El menú se resuelve por los CÓDIGOS de rol que trae el JWT (no por userId),
        // así una sesión sigue funcionando aunque se recree la BD y cambien los ids.
        String[] rolesArr = securityUtils.getCurrentUserRoles();
        List<String> roleCodes = (rolesArr != null) ? Arrays.asList(rolesArr) : List.of();
        if (roleCodes.isEmpty()) {
            throw new NotFoundException("Authenticated user has no roles");
        }

        // Módulos padres del menú según los roles del usuario
        List<Object[]> results = moduleRepository.findAllMenuParentByRoleCodes(roleCodes);
        List<MenuDto> menuDtoList = results.stream()
                .map(this::mapToMenuDto)
                .collect(Collectors.toList());

        // Para cada módulo padre, buscar sus hijos
        for (MenuDto menuDto : menuDtoList) {
            List<Object[]> resultsMenu = moduleRepository.findAllMenuByRoleCodes(roleCodes, menuDto.getId());

            List<ModuleDto> moduleDtos = resultsMenu.stream()
                    .map(this::mapToModuleDto)
                    .collect(Collectors.toList());

            menuDto.setChildren(moduleDtos);
        }

        return menuDtoList;
    }

    // ========== UTILIDAD PARA CONVERSIÓN DE FECHAS ==========

    /**
     * ✅ Convierte Object a LocalDateTime de forma segura
     * Maneja tanto LocalDateTime (Hibernate moderno) como Timestamp (JDBC legacy)
     */
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        // Por si acaso viene como java.util.Date
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime()).toLocalDateTime();
        }
        return null;
    }

    // ========== MAPPERS PRIVADOS ==========

    private ModuleSelectedDto mapToModuleSelectedDto(Object[] result) {
        return ModuleSelectedDto.builder()
                .id((UUID) result[0])
                .createdAt(toLocalDateTime(result[1]))
                .deletedAt(toLocalDateTime(result[2]))
                .icon((String) result[3])
                .moduleOrder((Integer) result[4])
                .title((String) result[5])
                .status((Boolean) result[6])
                .updatedAt(toLocalDateTime(result[7]))
                .link((String) result[8])
                .parentModuleId((UUID) result[9])
                .selected((Boolean) result[10])
                .build();
    }

    private MenuDto mapToMenuDto(Object[] result) {
        return MenuDto.builder()
                .id((UUID) result[0])
                .createdAt(toLocalDateTime(result[1]))
                .deletedAt(toLocalDateTime(result[2]))
                .icon((String) result[3])
                .moduleOrder((Integer) result[4])
                .title((String) result[5])
                .status((Boolean) result[6])
                .updatedAt(toLocalDateTime(result[7]))
                .link((String) result[8])
                .subtitle((String) result[9])
                .type((String) result[10])
                .build();
    }

    private ModuleDto mapToModuleDto(Object[] result) {
        return ModuleDto.builder()
                .id((UUID) result[0])
                .createdAt(toLocalDateTime(result[1]))
                .deletedAt(toLocalDateTime(result[2]))
                .icon((String) result[3])
                .moduleOrder((Integer) result[4])
                .title((String) result[5])
                .status((Boolean) result[6])
                .updatedAt(toLocalDateTime(result[7]))
                .link((String) result[8])
                .parentModuleId((UUID) result[9])
                .subtitle((String) result[10])
                .type((String) result[11])
                .build();
    }
}