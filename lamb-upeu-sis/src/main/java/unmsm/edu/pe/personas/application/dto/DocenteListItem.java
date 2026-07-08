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
public class DocenteListItem {
    private UUID personaId;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String emailInstitucional;
    private String gradoAcademico;
    private String categoria;
    private String condicion;
    private long asesorias;
    private long jurados;
    private long carga;
    private Boolean activo;
}
