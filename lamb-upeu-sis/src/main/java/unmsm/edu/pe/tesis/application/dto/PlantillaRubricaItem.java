package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Rúbrica oficial de un enfoque: la versión vigente + su historial. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaRubricaItem {
    private String enfoque;          // CUANTITATIVO | CUALITATIVO
    private String enfoqueLabel;     // "Cuantitativa / mixta" | "Cualitativa"
    private String descripcion;      // a qué proyectos se aplica
    private int criterios;           // cuántos criterios trae la rúbrica del sistema
    private int puntajeTotal;        // 100
    private int puntajeAprobacion;   // 65

    // ── Versión vigente (null si aún no se publicó ninguna) ──
    private UUID id;
    private String version;
    private String nombreOriginal;
    private LocalDateTime fechaCarga;

    /** Versiones anteriores, de la más reciente a la más antigua. */
    private List<VersionItem> historial;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionItem {
        private UUID id;
        private String version;
        private String nombreOriginal;
        private LocalDateTime fechaCarga;
    }
}
