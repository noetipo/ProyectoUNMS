package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramaPosgradoResponse {
    private UUID id;
    private String nombre;
    private String nivel;
    private UUID facultadId;
    private String facultadNombre;
}
