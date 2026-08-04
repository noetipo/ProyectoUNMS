package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Cuerpo para dar conformidad a todos los ítems de una sección de una sola vez (flujo por bloque). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConformidadSeccionRequest {
    /** Claves de los ítems del bloque a los que se da conformidad (campos del editor y/o "plan"). */
    private List<String> campos;
}
