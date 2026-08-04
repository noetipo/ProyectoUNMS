package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila del tablero de seguimiento de la Secretaría: un doctorando con la etapa del proceso
 * en la que se encuentra y la acción que lo tiene detenido (y de quién depende).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoAlumnoItem {

    private UUID estudianteId;
    private UUID tesisId;

    private String apellidos;
    private String nombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private String lineaNombre;

    private String tutorNombre;
    private String asesorNombre;

    /** Etapa en curso (1..8); 9 = proceso finalizado (sustentado). */
    private int etapaNumero;
    private String etapaTitulo;
    /** Avance del proceso en % (etapas completadas sobre 8). */
    private int avancePct;
    /** Estado derivado de la tesis (SIN_TEMA | SIN_ASESOR | &lt;estado&gt;) y su etiqueta. */
    private String estadoDerivado;
    private String estadoLabel;

    /** Qué falta para avanzar (texto corto y accionable). */
    private String pendiente;
    /** Quién debe actuar: SECRETARIA | COORDINADOR | ASESOR | REVISOR | JURADO | ESTUDIANTE | NADIE. */
    private String responsable;
    /** true si la acción pendiente le toca a la Secretaría (se resalta en la tabla). */
    private boolean pendienteSecretaria;

    /** Pantalla donde se resuelve el pendiente (null si nadie del sistema puede actuar ahí). */
    private String accionLink;
    /** Texto del botón que lleva a esa pantalla (verbo corto). */
    private String accionLabel;

    /** Fecha del último hito alcanzado y días transcurridos desde entonces (null si no hay hitos). */
    private LocalDate fechaUltimoHito;
    private Integer diasEnEtapa;

    private LocalDate fechaDefensa;

    /**
     * Vigencia del dictamen de aprobación del proyecto (4 años desde la defensa). Solo aplica a
     * proyectos con defensa programada y no sustentados.
     */
    private LocalDate vigenciaVenceEl;
    /** Meses que faltan para el vencimiento (negativo si ya venció). */
    private Integer vigenciaMeses;
    private boolean vigenciaVencida;
}
