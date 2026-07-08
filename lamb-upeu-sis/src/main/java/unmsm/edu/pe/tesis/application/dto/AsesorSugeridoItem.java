package unmsm.edu.pe.tesis.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Asesor sugerido por el tutor, con el detalle del docente para que el estudiante decida. */
@Data
@Builder
public class AsesorSugeridoItem {
    private UUID sugerenciaId;
    private UUID asesorDocenteId;
    private String nombres;
    private String apellidos;
    private String gradoAcademico;
    private String emailInstitucional;
    private List<String> lineas;
    private List<UUID> lineaIds;   // para reutilizar POST /solicitudes-asesoria (exige línea del docente)
    private long asesoriasActivas; // carga actual del docente como asesor
    private String nota;           // comentario del tutor
}
