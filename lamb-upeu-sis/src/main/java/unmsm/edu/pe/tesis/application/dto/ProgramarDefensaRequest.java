package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Programación de la defensa: modalidad, fecha, hora y aula/enlace. Quiénes la evalúan no se
 * eligen aquí — son los mismos dos revisores ya designados para el proyecto.
 */
@Data
public class ProgramarDefensaRequest {
    private LocalDate fecha;
    private String hora;             // "10:00"
    private String lugar;
    /** PRESENCIAL | VIRTUAL | HIBRIDA — de ella dependen el ambiente y el enlace. */
    private String modalidad;
    private String enlace;           // sala de videoconferencia, en virtual e híbrida
}
