package unmsm.edu.pe.tesis.application.util;

import unmsm.edu.pe.tesis.domain.enums.EnfoqueInvestigacion;

import java.util.List;
import java.util.Map;

/**
 * Definición estática de la estructura del editor del proyecto (equivalente a
 * {@code proyDef} del prototipo). Determina qué campos existen por sección y cuáles
 * dependen del enfoque; se usa para calcular el avance (X / total, %) igual que
 * {@code proyStats()}.
 */
public final class ProyectoDefinicion {

    private ProyectoDefinicion() {
    }

    /** Una sección del editor. {@code cuant}/{@code cual} = visible solo para ese enfoque. */
    public record Seccion(String id, String titulo, List<String> campos, boolean cuant, boolean cual) {
    }

    public static final List<Seccion> SECCIONES = List.of(
            new Seccion("g", "Datos generales", List.of("titulo", "resumen", "palabras"), false, false),
            new Seccion("p1", "I. Planteamiento del problema",
                    List.of("situacion", "formulacion", "justificacion", "objGeneral"), false, false),
            new Seccion("p2", "II. Marco teórico",
                    List.of("antecedentes", "bases", "glosario"), false, false),
            new Seccion("p3", "III. Hipótesis y variables",
                    List.of("hipotesis", "variables", "operacional"), true, false),
            new Seccion("p3q", "III. Categorías del estudio",
                    List.of("categorias", "supuestos"), false, true),
            new Seccion("p4", "IV. Metodología",
                    List.of("diseno", "poblacion", "tecnicas", "analisis", "eticos"), false, false),
            new Seccion("p6", "VI. Referencias y anexos",
                    List.of("referencias", "anexos"), false, false)
    );

    /** Campos de texto que se guardan en {@code proyecto_campos} (todos menos titulo/resumen). */
    public static final List<String> CAMPOS_PERSISTIDOS = SECCIONES.stream()
            .flatMap(s -> s.campos().stream())
            .filter(k -> !k.equals("titulo") && !k.equals("resumen"))
            .toList();

    /** Secciones activas para un enfoque (oculta p3 en cualitativo y p3q en cuantitativo). */
    public static List<Seccion> seccionesActivas(EnfoqueInvestigacion enfoque) {
        boolean cual = enfoque == EnfoqueInvestigacion.CUALITATIVO;
        return SECCIONES.stream()
                .filter(s -> !(s.cuant() && cual) && !(s.cual() && !cual))
                .toList();
    }

    /** ¿La clave es un campo válido del editor (para cualquier enfoque)? */
    public static boolean esCampoValido(String clave) {
        return SECCIONES.stream().anyMatch(s -> s.campos().contains(clave));
    }

    /**
     * Ítems que el asesor puede observar o dar conformidad para un enfoque: todos los campos
     * de texto de las secciones activas + el plan de actividades ({@code "plan"}). Es el conjunto
     * canónico que debe estar CONFORME para poder emitir la carta de opinión favorable.
     */
    public static List<String> itemsRevisables(EnfoqueInvestigacion enfoque) {
        List<String> items = new java.util.ArrayList<>();
        for (Seccion s : seccionesActivas(enfoque)) {
            items.addAll(s.campos());
        }
        items.add("plan");
        return items;
    }

    /** Etiqueta legible de cada campo (para títulos del documento generado). */
    public static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("titulo", "Título del proyecto"),
            Map.entry("resumen", "Resumen"),
            Map.entry("palabras", "Palabras clave"),
            Map.entry("situacion", "Situación problemática"),
            Map.entry("formulacion", "Formulación del problema"),
            Map.entry("justificacion", "Justificación de la investigación"),
            Map.entry("objGeneral", "Objetivo general"),
            Map.entry("antecedentes", "Antecedentes del problema"),
            Map.entry("bases", "Bases teóricas"),
            Map.entry("glosario", "Definición de términos"),
            Map.entry("hipotesis", "Hipótesis"),
            Map.entry("variables", "Variables"),
            Map.entry("operacional", "Operacionalización de variables"),
            Map.entry("categorias", "Categorías apriorísticas"),
            Map.entry("supuestos", "Supuestos de la investigación"),
            Map.entry("diseno", "Tipo y diseño de investigación"),
            Map.entry("poblacion", "Población y muestra"),
            Map.entry("tecnicas", "Técnicas e instrumentos"),
            Map.entry("analisis", "Plan de análisis de datos"),
            Map.entry("eticos", "Aspectos éticos"),
            Map.entry("referencias", "Referencias bibliográficas"),
            Map.entry("anexos", "Anexos"));

    public static String etiqueta(String clave) {
        return ETIQUETAS.getOrDefault(clave, clave);
    }
}
