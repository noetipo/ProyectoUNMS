package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Docente disponible para el combo del formulario (filtrado por línea). Incluye un perfil
 * breve —categoría, cargo vigente, estudios y carga— para que el tutor pueda decidir a quién
 * sugerir sin salir del diálogo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocenteComboItem {
    private UUID id;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String gradoAcademico;

    /** PRINCIPAL | ASOCIADO | AUXILIAR */
    private String categoria;
    /** NOMBRADO | CONTRATADO */
    private String condicion;
    /** Cargo vigente (p. ej. "Director de la Unidad de Posgrado"); null si no tiene. */
    private String cargoActual;
    /** Grados académicos ya formateados: "Doctor — UNMSM (2015)". */
    private List<String> estudios;
    /** Asesorías activas: sirve para repartir la carga entre docentes. */
    private long asesoriasActivas;
    private String emailInstitucional;
    /** Líneas de investigación registradas del docente. */
    private List<String> lineas;

    // ── Trayectoria (lo que la UPG pidió ver antes de designar) ──
    /** Centro laboral vigente, p. ej. "Hospital Nacional Dos de Mayo". */
    private String centroLaboral;
    /** Detalle del centro (sede/especialidad), para el subtítulo de la ficha. */
    private String centroLaboralDetalle;
    /** Años de experiencia: desde el inicio de su centro laboral más antiguo. */
    private Integer experienciaAnios;
    /** ORCID del docente, si lo registró. */
    private String orcid;
}
