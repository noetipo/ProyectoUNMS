package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Expediente de tesis: cabecera + línea de tiempo de las 8 etapas del proceso. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteResponse {
    private UUID tesisId;
    private String codigo;          // p. ej. EST-80000010
    private String estadoDerivado;  // código
    private String estadoLabel;     // etiqueta legible
    private String titulo;
    private String doctorandoNombre;
    private String programaNombre;
    private String asesorNombre;
    private String tutorNombre;
    private String lineaNombre;
    private int avancePct;          // etapas completadas / 8
    private List<EtapaItem> etapas;
}
