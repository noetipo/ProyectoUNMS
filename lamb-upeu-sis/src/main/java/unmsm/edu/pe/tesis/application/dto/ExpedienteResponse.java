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
    private int etapaEnCurso;       // 1..8 (9 = proceso finalizado)
    /**
     * true cuando el proceso llegó a la Etapa 4: la UPG ya emitió el dictamen de designación del
     * asesor. Antes de eso el doctorando no tiene nada que redactar todavía, así que la pestaña
     * "Mi proyecto" ni siquiera se muestra (ver MiTesisComponent).
     */
    private boolean proyectoHabilitado;
    /**
     * true cuando el asesor ya emitió su carta de opinión favorable: la redacción terminó y lo que
     * queda es el trámite de cierre (Turnitin, versión final y solicitud de aprobación), que vive
     * en su propia pantalla y no dentro del editor del proyecto.
     */
    private boolean cierreHabilitado;
    private List<EtapaItem> etapas;
}
