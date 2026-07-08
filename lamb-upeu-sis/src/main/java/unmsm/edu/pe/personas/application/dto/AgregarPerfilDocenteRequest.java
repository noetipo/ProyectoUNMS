package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.CategoriaDocente;
import unmsm.edu.pe.personas.domain.enums.CondicionDocente;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgregarPerfilDocenteRequest {

    private String codigoSistema;

    @Email(message = "El email institucional no es válido")
    private String emailInstitucional;

    // El grado académico ahora vive en la lista de grados de la persona (persona_grados_academicos).

    private CategoriaDocente categoria;
    private CondicionDocente condicion;
}
