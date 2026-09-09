package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Numeración manual del dictamen de designación del Jurado de Sustentación. */
@Data
public class ElaborarDictamenSustentacionRequest {
    private String numero;
    private String expediente;
}
