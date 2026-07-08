package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Fila de la lista de tutores (sin los estudiantes: se cargan al expandir). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteTutorItem {
    private UUID id;
    private String nombres;
    private String apellidos;
    private String gradoAcademico;
    private int estudiantes;
    private int cupoMaximo;
    private boolean cupoLleno;
    private int programas;
}
