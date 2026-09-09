package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Todo lo que la pantalla de cierre necesita: defensa, resultado, rúbricas, dictamen y archivo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreProyectoResponse {
    private UUID tesisId;
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;

    // Paso 1 · la defensa
    private LocalDate fechaDefensa;
    private String horaDefensa;
    private String lugarDefensa;
    private String modalidad;
    private String modalidadLabel;
    private String enlaceDefensa;

    // Paso 1 · resultado y rúbricas
    private boolean defensaRealizada;
    private String resultado;
    private String resultadoLabel;
    private LocalDate fechaResultado;
    private String observacionDefensa;
    private List<RubricaDefensaItem> rubricas;

    // Paso 2 · dictamen de aprobación
    private String estadoDictamen;
    private String dictamenNumero;
    private String dictamenExpediente;
    private LocalDate dictamenFechaEmision;
    private LocalDate dictamenVigenciaHasta;
    private boolean dictamenElaborado;
    private boolean dictamenFirmadoSubido;
    /** Propuesta de numeración cuando aún no se ha elaborado. */
    private String numeroSugerido;
    private String expedienteSugerido;

    // Paso 3 · archivo
    private boolean proyectoFinalDelEstudiante;
    private boolean proyectoFinalArchivado;
    private String proyectoFinalNombre;

    private boolean cerrado;
    private LocalDate fechaCierre;
    private String pendiente;
}
