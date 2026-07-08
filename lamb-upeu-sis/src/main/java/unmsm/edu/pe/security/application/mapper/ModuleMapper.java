package unmsm.edu.pe.security.application.mapper;


import org.mapstruct.*;
import unmsm.edu.pe.security.application.dto.ModuleDto;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.entities.ParentModule;

import java.util.UUID;

@Mapper(
        componentModel = "cdi",  // ✅ Cambio principal: "spring" → "cdi"
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ModuleMapper {

    /**
     * Convertir Module (Entity) → ModuleDto
     */
    @Mapping(source = "parentModule.id", target = "parentModuleId")
    @Mapping(source = "code", target = "code")
    ModuleDto toDto(Module module);

    /**
     * Convertir ModuleDto → Module (Entity)
     */
    @Mapping(source = "parentModuleId", target = "parentModule")
    @Mapping(target = "id", ignore = true)
    //@Mapping(target = "createdAt", ignore = true)
    //@Mapping(target = "updatedAt", ignore = true)
    //@Mapping(target = "deletedAt", ignore = true)
    //@Mapping(target = "createdBy", ignore = true)
    //@Mapping(target = "updatedBy", ignore = true)
    //@Mapping(target = "active", ignore = true)
    Module toEntity(ModuleDto dto);

    /**
     * Actualizar Module existente desde ModuleDto
     * Se usa en el método update del service
     */
    @Mapping(source = "parentModuleId", target = "parentModule")
    @Mapping(target = "id", ignore = true)
    //@Mapping(target = "createdAt", ignore = true)
    //@Mapping(target = "updatedAt", ignore = true)
    //@Mapping(target = "deletedAt", ignore = true)
    //@Mapping(target = "createdBy", ignore = true)
    //@Mapping(target = "updatedBy", ignore = true)
    //@Mapping(target = "active", ignore = true)
    void updateModuleFromDto(ModuleDto dto, @MappingTarget Module entity);

    /**
     * Mapeo custom: UUID → ParentModule
     * Se usa para la relación ManyToOne
     */
    default ParentModule map(UUID parentModuleId) {
        if (parentModuleId == null) {
            return null;
        }
        ParentModule parentModule = new ParentModule();
        parentModule.setId(parentModuleId);
        return parentModule;
    }

    /**
     * Mapeo custom: ParentModule → UUID
     * Se usa para la conversión inversa
     */
    default UUID map(ParentModule parentModule) {
        return parentModule != null ? parentModule.getId() : null;
    }
}
