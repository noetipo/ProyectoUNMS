package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Candidatos a asesor para sugerir a un tutorando: los docentes cuya especialidad
 * (línea de investigación) coincide con la línea del tema del estudiante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsesoresCandidatosResponse {
    /** True si el estudiante tiene un tema activo con línea de investigación registrada. */
    private boolean conLinea;
    private UUID lineaId;
    private String lineaNombre;
    private List<DocenteComboItem> docentes;
}
