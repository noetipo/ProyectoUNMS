package unmsm.edu.pe.security.application.mapper;

import org.mapstruct.*;
import unmsm.edu.pe.security.application.dto.ParentModuleDTO;
import unmsm.edu.pe.security.application.dto.ParentModuleRequestDto;
import unmsm.edu.pe.security.domain.entities.ParentModule;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface ParentModuleMapper {

    ParentModuleDTO toDTO(ParentModule parentModule);

    List<ParentModuleDTO> toDTOList(List<ParentModule> parentModules);

    @Mapping(target = "id", ignore = true)
   // @Mapping(target = "createdAt", ignore = true)
    //@Mapping(target = "updatedAt", ignore = true)
    //@Mapping(target = "deletedAt", ignore = true)
    //@Mapping(target = "active", ignore = true)
    ParentModule requestDtoToEntity(ParentModuleRequestDto requestDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    //@Mapping(target = "id", ignore = true)
    //@Mapping(target = "createdAt", ignore = true)
    //@Mapping(target = "updatedAt", ignore = true)
    //@Mapping(target = "deletedAt", ignore = true)
    //@Mapping(target = "active", ignore = true)
    void updateEntityFromDto(ParentModuleRequestDto requestDto, @MappingTarget ParentModule parentModule);

    ParentModule toEntity(ParentModuleDTO parentModuleDTO);
}
