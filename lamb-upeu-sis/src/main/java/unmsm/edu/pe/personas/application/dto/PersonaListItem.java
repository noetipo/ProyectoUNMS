package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaListItem {
    private UUID id;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String emailPersonal;
    private String celular;
    private List<String> perfiles;
    private Boolean activo;
}
