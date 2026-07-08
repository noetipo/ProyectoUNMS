package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.EstudianteTemaItem;
import unmsm.edu.pe.tesis.application.dto.EstudiantesTemaResumen;
import unmsm.edu.pe.tesis.application.dto.RegistrarTemaRequest;
import unmsm.edu.pe.tesis.application.dto.TemaResponse;

import java.util.UUID;

public interface CoordinadorTemaService {

    EstudiantesTemaResumen resumen(UUID facultadId, UUID programaId);

    PageResponse<EstudianteTemaItem> listar(UUID facultadId, UUID programaId, Boolean conTema, String buscar, int page, int size);

    /** Crea la tesis del estudiante (nivel derivado, fecha/creador del servidor). */
    TemaResponse registrarTema(UUID estudianteId, RegistrarTemaRequest request);

    /** Edita título/línea/resumen de la tesis activa del estudiante. */
    TemaResponse editarTema(UUID estudianteId, RegistrarTemaRequest request);

    /** Tema (tesis activa) del estudiante, o null si aún no tiene. Reutilizable por el perfil. */
    TemaResponse temaDeEstudiante(UUID estudianteId);
}
