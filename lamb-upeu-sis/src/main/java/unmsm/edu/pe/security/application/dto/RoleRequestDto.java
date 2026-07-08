package unmsm.edu.pe.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 10, message = "Code must not exceed 10 characters")
    private String code;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Boolean status = true;
}