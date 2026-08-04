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
    private String tipo;           // ASESOR | COASESOR — puesto para el que lo sugirió el tutor

    // ── Trayectoria: el doctorando elige a quién confiar su tesis, no solo un nombre ──
    private String categoria;          // Docente Principal / Asociado / Auxiliar
    private String condicion;          // Nombrado / Contratado
    private String cargoActual;        // cargo vigente, si tiene
    private String centroLaboral;      // dónde trabaja
    private String centroLaboralDetalle;
    private Integer experienciaAnios;  // años desde su centro laboral más antiguo
    private String orcid;
    private List<String> estudios;     // grados académicos formateados
}
