package unmsm.edu.pe.security.application.mapper;

import org.mapstruct.*;
import unmsm.edu.pe.security.application.dto.RoleRequestDto;
import unmsm.edu.pe.security.application.dto.RoleResponseDto;
import unmsm.edu.pe.security.domain.entities.Role;

@Mapper(componentModel = "cdi",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {

    /**
     * Convierte RoleRequestDto a entidad Role
     */
    @Mapping(target = "id", ignore = true)
    Role toEntity(RoleRequestDto roleRequestDto);

    /**
     * Convierte entidad Role a RoleResponseDto
     */
    RoleResponseDto toResponseDto(Role role);

    /**
     * Actualiza una entidad Role existente desde un RoleRequestDto
     */
    @Mapping(target = "id", ignore = true)
    void updateRoleFromDto(RoleRequestDto roleRequestDto, @MappingTarget Role role);
}