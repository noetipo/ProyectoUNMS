package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Detalle de ejecución de la tesis (Etapa 6): plan, avances y estado del informe final. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjecucionDetalleResponse {
    private ProyectoEditorResponse proyecto;   // incluye actividades del plan
    private List<AvanceItem> avances;
    private int porcentajePlan;                // % de actividades HECHA
    private boolean planCompleto;              // 100% ejecutado
    private boolean informeFinalAprobado;
    private boolean informeFinalSubido;        // el estudiante subió el informe final
    private int puntajeMaximo;
}
