package unmsm.edu.pe.tesis.application.util;

import java.util.List;

/**
 * Rúbrica de avances de la tesis (Etapa 6 · ejecución). El asesor evalúa periódicamente
 * la ejecución del plan. Escala 1–4: Excelente (4) · Bueno (3) · Básico (2) · En proceso (1).
 */
public final class RubricaAvanceDefinicion {

    private RubricaAvanceDefinicion() {
    }

    public record Criterio(String key, String titulo, String descripcion) {
    }

    public static final int PUNTAJE_MIN = 1;
    public static final int PUNTAJE_MAX = 4;

    public static final List<Criterio> CRITERIOS = List.of(
            new Criterio("ejecucion", "Ejecución del plan",
                    "Cumplimiento del cronograma y de las actividades previstas."),
            new Criterio("datos", "Recolección y calidad de los datos",
                    "Rigor en el trabajo de campo y calidad de los datos obtenidos."),
            new Criterio("analisis", "Análisis preliminar",
                    "Procesamiento y análisis de la información recolectada."),
            new Criterio("interpretacion", "Interpretación de resultados",
                    "Coherencia de la interpretación con los objetivos de la investigación.")
    );

    public static int puntajeMaximo() {
        return CRITERIOS.size() * PUNTAJE_MAX;
    }

    public static boolean esCriterioValido(String key) {
        return CRITERIOS.stream().anyMatch(c -> c.key().equals(key));
    }
}
