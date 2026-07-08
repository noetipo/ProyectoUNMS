package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Estudiante sin tutoría vigente. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteSinTutorItem {
    private UUID estudianteId;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String codMatricula;
    private String programaNombre;
    private Integer anioIngreso;
}
