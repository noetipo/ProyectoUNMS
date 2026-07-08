package unmsm.edu.pe.security.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleModuleDTO {
    private UUID roleId;
    private UUID parentModuleId;

    @JsonProperty("moduleDTOS")
    @JsonAlias({"modules", "moduleDtos", "moduleDTOS"})
    private List<ModuleDto> moduleDtos;
}