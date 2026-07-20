package unmsm.edu.pe.tesis.application.util;

import java.util.List;

/**
 * Rúbrica de evaluación del proyecto por los revisores (Jurado Informante, Etapa 5).
 * Escala 1–4: Excelente (4) · Bueno (3) · Básico (2) · En proceso (1).
 */
public final class RubricaDefinicion {

    private RubricaDefinicion() {
    }

    /** Un criterio de la rúbrica. */
    public record Criterio(String key, String titulo, String descripcion) {
    }

    public static final int PUNTAJE_MIN = 1;
    public static final int PUNTAJE_MAX = 4;

    public static final List<Criterio> CRITERIOS = List.of(
            new Criterio("problema", "Planteamiento del problema",
                    "Pertinencia, formulación clara y justificación del problema de investigación."),
            new Criterio("marco", "Marco teórico",
                    "Solidez y vigencia de antecedentes y bases teóricas vinculadas al problema."),
            new Criterio("metodologia", "Metodología",
                    "Rigor del diseño, población/muestra, técnicas, instrumentos y plan de análisis."),
            new Criterio("viabilidad", "Aspectos administrativos y viabilidad",
                    "Coherencia del cronograma, presupuesto y factibilidad del proyecto."),
            new Criterio("redaccion", "Redacción y referencias",
                    "Claridad de la redacción y uso correcto de las referencias bibliográficas.")
    );

    /** Puntaje máximo posible (n° de criterios × 4). */
    public static int puntajeMaximo() {
        return CRITERIOS.size() * PUNTAJE_MAX;
    }

    public static boolean esCriterioValido(String key) {
        return CRITERIOS.stream().anyMatch(c -> c.key().equals(key));
    }

    public static String titulo(String key) {
        return CRITERIOS.stream().filter(c -> c.key().equals(key)).map(Criterio::titulo).findFirst().orElse(key);
    }
}
