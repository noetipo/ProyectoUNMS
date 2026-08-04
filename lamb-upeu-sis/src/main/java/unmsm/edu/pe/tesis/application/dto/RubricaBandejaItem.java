package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Secretaría · proyecto con revisores designados que requiere (o ya tiene) la rúbrica oficial subida. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricaBandejaItem {
    private UUID tesisId;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private String enfoque;            // CUALITATIVO | CUANTITATIVO (define la plantilla de rúbrica)
    private int numRevisores;
    private List<String> revisores;    // nombres de los revisores designados
    private boolean rubricaSubida;     // legado: Word adjunto al propio expediente
    private String rubricaNombreArchivo;
    /** Interruptor: la evaluación con la rúbrica oficial está habilitada para este proyecto. */
    private boolean rubricaHabilitada;
    private String plantillaLabel;          // "Cuantitativa / mixta" | "Cualitativa"
    private String plantillaVersion;        // versión vigente publicada (null si aún no hay ninguna)
    private String plantillaVersionAplicada; // la que se le congeló al proyecto al habilitarlo
    // ── Defensa (la programa la Secretaría) ──
    private boolean revisoresConformes;  // los revisores aprobaron → se puede programar la defensa
    private boolean defensaProgramada;
    private java.time.LocalDate fechaDefensa;
    private String horaDefensa;
    private String lugarDefensa;
}
