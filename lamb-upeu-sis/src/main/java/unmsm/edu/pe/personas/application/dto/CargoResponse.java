package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoResponse {
    private UUID id;
    private String codigoSistema;
    private String nombre;
    private String descripcion;
    private Boolean activo;
}
