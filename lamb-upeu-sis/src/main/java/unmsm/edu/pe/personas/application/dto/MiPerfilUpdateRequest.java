package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Campos que el propio usuario puede editar de su persona.
 * NO incluye código, programa, condición, documento ni roles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiPerfilUpdateRequest {

    @Email(message = "El email personal no es válido")
    private String emailPersonal;

    private String celular;
    private String orcid;
}
