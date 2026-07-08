package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.AsesoresCandidatosResponse;
import unmsm.edu.pe.tesis.application.dto.AsesorSugeridoItem;
import unmsm.edu.pe.tesis.application.dto.SugerirAsesorRequest;

import java.util.List;
import java.util.UUID;

public interface SugerenciaAsesorService {

    /** Sugerencias (enriquecidas) de un estudiante. Reutilizable por la bandeja del estudiante. */
    List<AsesorSugeridoItem> listar(UUID estudianteId);

    /** El tutor autenticado ve las sugerencias de SU tutorando. */
    List<AsesorSugeridoItem> listarDeMiTutorando(UUID estudianteId);

    /**
     * Candidatos a asesor para SU tutorando: docentes cuya especialidad coincide con
     * la línea de investigación del tema del estudiante.
     */
    AsesoresCandidatosResponse candidatosDeMiTutorando(UUID estudianteId);

    /** El tutor autenticado sugiere un asesor a SU tutorando. */
    AsesorSugeridoItem sugerir(UUID estudianteId, SugerirAsesorRequest request);

    /** El tutor autenticado quita una sugerencia suya. */
    void quitar(UUID sugerenciaId);
}
