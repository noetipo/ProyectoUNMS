package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Docente-tutor para el autocomplete, con cupo y disponibilidad. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorComboItem {
    private UUID id;
    private String nombres;
    private String apellidos;
    private String gradoAcademico;
    private String codigoSistema;
    private int estudiantesActuales;
    private int cupoMaximo;
    private boolean disponible;
    /** true si el docente ya tiene el rol PROF_TUTOR; si es false, quedará como tutor nuevo al asignarle. */
    private boolean esTutor;
}
