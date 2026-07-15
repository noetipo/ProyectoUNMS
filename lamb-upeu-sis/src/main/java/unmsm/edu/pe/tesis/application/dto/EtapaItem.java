package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una etapa de la línea de tiempo del proceso de titulación. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaItem {
    private int numero;
    private String titulo;
    private String descripcion;
    private String estado;      // COMPLETADO | EN_CURSO | PENDIENTE
    private String fecha;       // formateada (dd MMM yyyy) o null
    private boolean tieneDictamen;
    private String dictamenLabel;
}
