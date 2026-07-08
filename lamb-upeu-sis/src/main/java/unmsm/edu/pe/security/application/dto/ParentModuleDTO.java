package unmsm.edu.pe.security.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentModuleDTO {
    private UUID id;
    private String title;
    private String code;
    private String subtitle;
    private String type;
    private String icon;
    private Boolean status;
    private Integer moduleOrder;
    private String link;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    //private List<ModuleDTO> moduleDTOS;
}