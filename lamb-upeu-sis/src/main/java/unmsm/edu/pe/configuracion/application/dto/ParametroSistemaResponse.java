package unmsm.edu.pe.configuracion.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistemaResponse {
    private UUID id;
    private String clave;
    private String valor;
    private String descripcion;
    private Boolean activo;
}
