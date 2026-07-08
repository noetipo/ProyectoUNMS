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
public class EstudianteListItem {
    private UUID personaId;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String codMatricula;
    private String emailInstitucional;
    private Integer anioIngreso;
    private String condicion;
    private String financiamiento;
    private UUID programaId;
    private String programaNombre;
    private String nivel;
    private Boolean activo;
}
