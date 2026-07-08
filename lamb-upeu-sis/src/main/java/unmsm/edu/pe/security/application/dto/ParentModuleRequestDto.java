package unmsm.edu.pe.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentModuleRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Subtitle is required")
    @Size(min = 2, max = 100, message = "Subtitle must be between 2 and 100 characters")
    private String subtitle;

    @NotBlank(message = "Type is required")
    @Size(max = 100, message = "Type must not exceed 100 characters")
    private String type;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    private Boolean status = true;

    @NotNull(message = "Module order is required")
    private Integer moduleOrder;

    @NotBlank(message = "Link is required")
    @Size(max = 500, message = "Link must not exceed 500 characters")
    private String link;
}
