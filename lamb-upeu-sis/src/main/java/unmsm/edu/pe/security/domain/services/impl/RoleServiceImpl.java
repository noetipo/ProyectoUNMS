package unmsm.edu.pe.security.domain.services.impl;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;
import unmsm.edu.pe.security.application.dto.ModuleDto;
import unmsm.edu.pe.security.application.dto.RoleModuleDTO;
import unmsm.edu.pe.security.application.dto.RoleRequestDto;
import unmsm.edu.pe.security.application.dto.RoleResponseDto;
import unmsm.edu.pe.security.application.mapper.RoleMapper;
import unmsm.edu.pe.security.domain.entities.*;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.repositories.*;
import unmsm.edu.pe.security.domain.services.RoleService;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleServiceImpl implements RoleService {

    private static final Logger LOG = Logger.getLogger(RoleServiceImpl.class);

    @Inject
    RoleRepository roleRepository;

    @Inject
    RoleModuleRepository roleModuleRepository;

    @Inject
    ModuleRepository moduleRepository;

    @Inject
    ParentModuleRepository parentModuleRepository;

    @Inject
    RoleMapper roleMapper;

    @Override
    public Map<String, Object> list(Integer page, Integer size, String nameFilter) {
        Page panachePage = Page.of(page, size);

        List<Role> roles = roleRepository.findWithFilters(nameFilter, panachePage);
        long totalElements = roleRepository.countWithFilters(nameFilter);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<RoleResponseDto> roleResponseDtos = roles.stream()
                .map(roleMapper::toResponseDto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", roleResponseDtos);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("pageSize", size);

        return response;
    }

    @Override
    @Transactional
    public RoleResponseDto save(RoleRequestDto roleRequestDto) {
        Role role = roleMapper.toEntity(roleRequestDto);

        Role savedRole = roleRepository.save(role);
        LOG.info("Role saved: " + savedRole.getName());

        return roleMapper.toResponseDto(savedRole);
    }

    @Override
    @Transactional
    public RoleResponseDto saveModuleAssignment(RoleModuleDTO roleModuleDTO) {
        LOG.info("=== Iniciando asignación de módulos ===");
        LOG.info("Role ID: " + roleModuleDTO.getRoleId());
        LOG.info("Parent Module ID: " + roleModuleDTO.getParentModuleId());
        LOG.info("Módulos recibidos: " + (roleModuleDTO.getModuleDtos() != null ? roleModuleDTO.getModuleDtos().size() : 0));

        // Buscar el rol
        Role role = roleRepository.getRoleById(roleModuleDTO.getRoleId())
                .orElseThrow(() -> new NotFoundException("Role not found with ID: " + roleModuleDTO.getRoleId()));

        LOG.info("Rol encontrado: " + role.getName());

        // Buscar el parent module
        ParentModule parentModule = parentModuleRepository.getParentModuleById(roleModuleDTO.getParentModuleId())
                .orElseThrow(() -> new NotFoundException("Parent module not found with ID: " + roleModuleDTO.getParentModuleId()));

        LOG.info("Parent Module encontrado: " + parentModule.getTitle());

        // Buscar los módulos hijos del módulo padre
        List<Module> modules = moduleRepository.findByParentModuleId(roleModuleDTO.getParentModuleId());
        LOG.info("Módulos hijos encontrados: " + modules.size());

        // Buscar y eliminar las asignaciones existentes para este rol y módulos
        List<RoleModule> existingRoleModules = roleModuleRepository
                .findByModuleInAndRole(modules, role);

        if (!existingRoleModules.isEmpty()) {
            LOG.info("Eliminando " + existingRoleModules.size() + " asignaciones existentes");
            for (RoleModule rm : existingRoleModules) {
                roleModuleRepository.remove(rm);
            }
        }

        // Validar que moduleDtos no sea null
        if (roleModuleDTO.getModuleDtos() == null || roleModuleDTO.getModuleDtos().isEmpty()) {
            LOG.warn("No se recibieron módulos para asignar");
            return roleMapper.toResponseDto(role);
        }

        // Crear nuevas asignaciones para los módulos seleccionados
        int assignedCount = 0;
        for (ModuleDto moduleDto : roleModuleDTO.getModuleDtos()) {
            LOG.info("Procesando módulo: " + moduleDto.getTitle() + " - Selected: " + moduleDto.getSelected());

            if (moduleDto.getSelected() != null && moduleDto.getSelected()) {
                RoleModule roleModule = new RoleModule();

                Module module = moduleRepository.findById(moduleDto.getId());

                roleModule.setModule(module);
                roleModule.setAssigned(true);
                roleModule.setRole(role);

                roleModuleRepository.save(roleModule);
                assignedCount++;

                LOG.info("Módulo asignado: " + module.getTitle());
            }
        }

        LOG.info("=== Se asignaron " + assignedCount + " módulos al rol " + role.getName() + " ===");

        // Guardar y retornar el rol actualizado
        Role updatedRole = roleRepository.save(role);
        return roleMapper.toResponseDto(updatedRole);
    }

    @Override
    public RoleResponseDto findById(UUID uuid) {
        Role role = roleRepository.getRoleById(uuid)
                .orElseThrow(() -> new NotFoundException("Role not found with ID: " + uuid));

        return roleMapper.toResponseDto(role);
    }

    @Override
    @Transactional
    public RoleResponseDto update(UUID uuid, RoleRequestDto roleRequestDto) {
        Role existingRole = roleRepository.getRoleById(uuid)
                .orElseThrow(() -> new NotFoundException("Role not found with ID: " + uuid));

        existingRole.setName(roleRequestDto.getName());
        existingRole.setCode(roleRequestDto.getCode());
        existingRole.setDescription(roleRequestDto.getDescription());
        existingRole.setStatus(roleRequestDto.getStatus());

        Role updatedRole = roleRepository.save(existingRole);
        LOG.info("Role updated: " + updatedRole.getName());

        return roleMapper.toResponseDto(updatedRole);
    }

    @Override
    @Transactional
    public void delete(UUID uuid) {
        Role role = roleRepository.getRoleById(uuid)
                .orElseThrow(() -> new NotFoundException("Role not found with ID: " + uuid));

        role.markAsDeleted();
        roleRepository.remove(role);

        LOG.info("Role marked as deleted: " + role.getName());
    }
}