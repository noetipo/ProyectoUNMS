package unmsm.edu.pe.security.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDto {
    private UUID id;
    private String title;
    private String subtitle;
    private String type;
    private String icon;
    private Boolean status;
    private Boolean selected;
    private Integer moduleOrder;
    private String link;
    private UUID parentModuleId;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
