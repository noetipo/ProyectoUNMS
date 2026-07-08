package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Estudiante vigente de un tutor (detalle lazy). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorEstudianteItem {
    private UUID estudianteId;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String codMatricula;
    private String programaNombre;
    private LocalDate fechaInicio;
    private Integer anioIngreso;

    // Estado derivado del tema (se llena solo en el panel del tutor; null en el reporte de tutores).
    private String estadoDerivado; // SIN_TEMA | SIN_ASESOR | <estado tesis>
    private String tesisTitulo;
    private String lineaNombre;
}
