package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.time.LocalDate;

/** Programación del acto de sustentación: modalidad, fecha, hora y aula/enlace. */
@Data
public class ProgramarSustentacionRequest {
    private LocalDate fecha;
    private String hora;
    private String lugar;
    /** PRESENCIAL | VIRTUAL | HIBRIDA. */
    private String modalidad;
    private String enlace;
}
