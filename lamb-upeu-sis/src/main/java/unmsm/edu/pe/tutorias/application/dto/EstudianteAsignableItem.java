package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Estudiante en la tabla de asignación en bloque (con su tutor vigente, si tiene). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteAsignableItem {
    private UUID estudianteId;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private UUID programaId;
    private String programaNombre;
    private UUID tutorId;
    private String tutorNombre;
}
