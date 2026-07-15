package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Estado de revisión de un campo del proyecto + su hilo de eventos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionItem {
    private String campo;
    private String estado;   // EstadoItemRevision
    private List<RevisionEventoItem> eventos;
}
