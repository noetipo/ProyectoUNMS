package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.time.LocalDate;

/** Resultado del acto de defensa que registra la Secretaría. */
@Data
public class RegistrarResultadoDefensaRequest {
    /** APROBADO | APROBADO_CON_OBSERVACIONES | DESAPROBADO */
    private String resultado;
    private LocalDate fecha;
    private String observacion;
}
