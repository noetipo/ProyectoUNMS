package unmsm.edu.pe.tesis.application.util;

/**
 * Las 8 etapas del proceso de titulación — fuente única de sus títulos y descripciones.
 * La consumen el expediente del doctorando (línea de tiempo) y el tablero de seguimiento
 * de la Secretaría.
 */
public final class EtapasProceso {

    /** Cantidad de etapas del proceso. */
    public static final int TOTAL = 8;

    /** {título, descripción, título corto (para tarjetas/tablas)}. */
    private static final String[][] DATOS = {
            {"Registro del tema de tesis",
                    "El Coordinador registra el tema y la línea de investigación en línea.",
                    "Registro del tema"},
            {"Designación del tutor",
                    "Asignación de la tutora académica desde el directorio de docentes.",
                    "Designación del tutor"},
            {"Designación del asesor (DICTAMEN)",
                    "Solicitud + carta de aceptación firmadas; la UPG emite el dictamen de designación.",
                    "Designación del asesor"},
            {"Elaboración del proyecto",
                    "Proyecto en línea según enfoque, plan de actividades, observaciones del asesor y Turnitin.",
                    "Elaboración del proyecto"},
            {"Defensa del proyecto y aprobación (DICTAMEN)",
                    "Dos revisores evalúan con rúbrica; defensa oral; dictamen con vigencia de 4 años.",
                    "Defensa del proyecto"},
            {"Ejecución de la tesis",
                    "Comité de ética, avance según plan de actividades, evidencias y aprobación del informe final.",
                    "Ejecución de la tesis"},
            {"Revisión por Jurado Informante",
                    "Tres revisores; observaciones y levantamiento; opinión favorable del Presidente.",
                    "Jurado Informante"},
            {"Sustentación de la tesis",
                    "Expedito, Jurado Examinador, acto público de sustentación y acta.",
                    "Sustentación"}
    };

    private EtapasProceso() {
    }

    /** Título de la etapa (1..8); "Proceso finalizado" para 9. */
    public static String titulo(int numero) {
        if (numero > TOTAL) {
            return "Proceso finalizado";
        }
        return valido(numero) ? DATOS[numero - 1][0] : "";
    }

    /** Descripción de la etapa (1..8). */
    public static String descripcion(int numero) {
        return valido(numero) ? DATOS[numero - 1][1] : "";
    }

    /** Título corto para tarjetas y tablas; "Finalizado" para 9. */
    public static String corto(int numero) {
        if (numero > TOTAL) {
            return "Finalizado";
        }
        return valido(numero) ? DATOS[numero - 1][2] : "";
    }

    /** Avance del proceso en % según la etapa en curso (las anteriores cuentan como completadas). */
    public static int avancePct(int etapaEnCurso) {
        int completadas = Math.max(0, Math.min(TOTAL, etapaEnCurso - 1));
        return Math.round(completadas * 100f / TOTAL);
    }

    private static boolean valido(int numero) {
        return numero >= 1 && numero <= TOTAL;
    }
}
