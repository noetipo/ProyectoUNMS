package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Numeración manual del Dictamen de Expedito. */
@Data
public class ElaborarDictamenExpeditoRequest {
    private String numero;
    private String expediente;
}
