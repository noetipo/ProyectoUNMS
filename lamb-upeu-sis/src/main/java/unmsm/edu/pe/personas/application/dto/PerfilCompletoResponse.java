package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilCompletoResponse {
    private PersonaResponse persona;
    private List<CargoHistorialItem> cargos;
    private List<CentroHistorialItem> centrosLaborales;
    private List<DocumentoItem> documentos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoHistorialItem {
        private UUID id;
        private UUID cargoId;
        private String cargoNombre;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Boolean actual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CentroHistorialItem {
        private UUID id;
        private UUID centroLaboralId;
        private String centroNombre;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Boolean actual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoItem {
        private UUID id;
        private String tipoDocumento;
        private String nombreOriginal;
        private String contentType;
        private Long tamanioBytes;
        private LocalDateTime fechaCarga;
    }
}
