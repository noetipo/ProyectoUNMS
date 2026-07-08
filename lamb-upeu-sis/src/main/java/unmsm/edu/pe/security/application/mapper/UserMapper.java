package unmsm.edu.pe.security.application.mapper;

import org.mapstruct.*;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.application.dto.UserRequestDto;
import unmsm.edu.pe.security.application.dto.UserResponseDto;
import unmsm.edu.pe.security.application.dto.UserUpdateDto;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    // No need to ignore password since UserResponseDto doesn't have password field
    UserResponseDto toResponseDto(User user);

    List<UserResponseDto> toResponseDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "active", ignore = true)
    User toEntity(UserRequestDto requestDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) // Username should not be updatable
    @Mapping(target = "password", ignore = true) // Password updated separately
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntityFromDto(UserUpdateDto updateDto, @MappingTarget User user);
}