package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.time.LocalDate;

/** Numeración manual del dictamen de aprobación; la vigencia se calcula si no se indica. */
@Data
public class ElaborarDictamenAprobacionRequest {
    private String numero;
    private String expediente;
    private LocalDate vigenciaHasta;
}
