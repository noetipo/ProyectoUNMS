package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Numeración manual del dictamen de designación del Jurado Informante. */
@Data
public class ElaborarDictamenJuradoInformeRequest {
    private String numero;
    private String expediente;
}
