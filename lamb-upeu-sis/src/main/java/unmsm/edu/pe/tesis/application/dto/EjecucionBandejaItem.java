package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Fila de la bandeja de ejecución del asesor (Etapa 6): proyecto en ejecución con % del plan. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjecucionBandejaItem {
    private UUID tesisId;
    private UUID proyectoId;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private int porcentajePlan;
    private boolean informeFinalAprobado;
    private int numAvances;
}
