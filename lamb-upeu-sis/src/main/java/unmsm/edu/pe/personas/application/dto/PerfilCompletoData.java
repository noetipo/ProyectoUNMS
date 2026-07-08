package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Parte JSON del multipart: historiales de cargos y centros laborales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilCompletoData {

    private List<CargoHistorial> cargos;
    private List<CentroHistorial> centrosLaborales;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoHistorial {
        private UUID cargoId;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Boolean actual;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CentroHistorial {
        private UUID centroLaboralId;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Boolean actual;
    }
}
