package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un evento del hilo de revisión de un ítem. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionEventoItem {
    private String tipo;    // OBSERVACIÓN | EDICIÓN | CORRECCIÓN | CONFORMIDAD
    private String autor;
    private String rol;     // ESTUDIANTE | ASESOR
    private String texto;
    private String fecha;   // formateada
}
