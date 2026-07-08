package unmsm.edu.pe.security.domain.services;

import unmsm.edu.pe.security.application.dto.RoleModuleDTO;
import unmsm.edu.pe.security.application.dto.RoleRequestDto;
import unmsm.edu.pe.security.application.dto.RoleResponseDto;

import java.util.Map;
import java.util.UUID;

public interface RoleService {
    Map<String, Object> list(Integer page, Integer size, String nameFilter);
    RoleResponseDto save(RoleRequestDto roleRequestDto);
    RoleResponseDto saveModuleAssignment(RoleModuleDTO roleModuleDTO);
    RoleResponseDto findById(UUID uuid);
    RoleResponseDto update(UUID uuid, RoleRequestDto roleRequestDto);
    void delete(UUID uuid);
}
