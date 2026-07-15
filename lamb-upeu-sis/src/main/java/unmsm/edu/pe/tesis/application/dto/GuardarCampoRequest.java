package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Autoguardado de un campo del editor. {@code valor} puede venir vacío (limpiar). */
@Data
public class GuardarCampoRequest {
    private String valor;
}
