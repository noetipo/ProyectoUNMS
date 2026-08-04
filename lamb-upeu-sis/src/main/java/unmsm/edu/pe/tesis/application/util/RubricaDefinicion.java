package unmsm.edu.pe.tesis.application.util;

import java.util.List;

/**
 * Rúbrica oficial de evaluación del proyecto por los revisores (UNMSM · Facultad de Medicina · Posgrado).
 * Dos rúbricas según el enfoque: CUALITATIVO y CUANTITATIVO/MIXTO. Cada criterio se califica con
 * <b>Cumple / Cumple parcialmente / No cumple</b>, cada nivel con su puntaje. Total 100; aprobado ≥ 65.
 */
public final class RubricaDefinicion {

    private RubricaDefinicion() {
    }

    /** Nivel de cumplimiento de un criterio. */
    public enum Nivel { CUMPLE, PARCIAL, NO_CUMPLE }

    /** Puntaje mínimo para aprobar el proyecto. */
    public static final int APROBADO_MIN = 65;

    /** Un criterio con sus tres puntajes (cumple es el máximo). */
    public record Criterio(String key, String titulo, String descripcion, int cumple, int parcial, int noCumple) {
        public int puntajeMaximo() { return cumple; }
        public int puntaje(Nivel n) {
            return switch (n) {
                case CUMPLE -> cumple;
                case PARCIAL -> parcial;
                case NO_CUMPLE -> noCumple;
            };
        }
    }

    /** Una sección de la rúbrica (rubro) con sus criterios. */
    public record Seccion(String key, String titulo, List<Criterio> criterios) {
        public int subtotal() { return criterios.stream().mapToInt(Criterio::puntajeMaximo).sum(); }
    }

    /** Una rúbrica completa (por enfoque). */
    public record Rubrica(String enfoque, String titulo, List<Seccion> secciones) {
        public int total() { return secciones.stream().mapToInt(Seccion::subtotal).sum(); }
        public List<Criterio> criterios() {
            return secciones.stream().flatMap(s -> s.criterios().stream()).toList();
        }
        public Criterio criterio(String key) {
            return criterios().stream().filter(c -> c.key().equals(key)).findFirst().orElse(null);
        }
    }

    // ── Rúbrica CUANTITATIVA / MIXTA (total 100) ──────────────────────────────
    public static final Rubrica CUANTITATIVA = new Rubrica("CUANTITATIVO",
            "Evaluación del Proyecto de Tesis — Enfoque Cuantitativo y Mixto", List.of(
            new Seccion("problema", "I. Planteamiento del problema", List.of(
                    new Criterio("situacion", "Situación problemática",
                            "Vincula el problema (explicativo/correlacional) con sus aspectos: importancia, novedad, interés y viabilidad; contextualizado social y teóricamente; describe qué se ha investigado y qué falta.", 15, 10, 5),
                    new Criterio("formulacion", "Formulación del problema",
                            "Pregunta específica derivada de la situación; objeto reconocible; plantea algo nuevo; identifica claramente las variables; relación con el título.", 8, 5, 3),
                    new Criterio("justificacion", "Justificación de la investigación",
                            "Conveniencia teórica, práctica, metodológica y social; beneficios y beneficiarios; aporte principal del trabajo.", 5, 4, 3),
                    new Criterio("objetivos", "Objetivos de la investigación",
                            "Relación con la pregunta; medibles y alcanzables en el tiempo e instrumentos; variables claramente identificadas.", 7, 5, 3))),
            new Seccion("marco", "II. Marco teórico e hipótesis", List.of(
                    new Criterio("antecedentes", "Antecedentes del problema",
                            "Investigaciones vinculadas al problema, en orden cronológico o temático, a manera de resumen (no “copia y pega”).", 5, 3, 2),
                    new Criterio("bases", "Bases teóricas",
                            "Enfoque teórico que sustenta el desarrollo, vinculado a las variables, relacionado al objetivo e hipótesis, debidamente estructurado.", 4, 3, 2),
                    new Criterio("glosario", "Definición conceptual de términos / Glosario",
                            "Términos o conceptos empleados que requieren precisarse, acompañados de su referencia bibliográfica.", 3, 2, 1),
                    new Criterio("hipotesis", "Hipótesis de la investigación",
                            "Respuesta anticipada al problema y base del diseño; asociativas o no asociativas (con o sin relación de dependencia).", 3, 2, 1),
                    new Criterio("variables", "Identificación de las variables",
                            "Describe las variables del problema, objetivos e hipótesis; considera variables extrañas o confusoras.", 3, 2, 1),
                    new Criterio("operacional", "Operacionalización de las variables",
                            "Conversión de la definición conceptual a operacional (reglas de medición); incluye el valor final de las variables.", 5, 3, 2),
                    new Criterio("matriz", "Matriz de consistencia",
                            "Consistencia entre problema, hipótesis, variables y diseño; puede incluir técnicas e instrumentos de medición.", 2, 1, 0))),
            new Seccion("metodologia", "III. Metodología", List.of(
                    new Criterio("tipoDiseno", "Tipo y diseño de la investigación",
                            "Explica el tipo de investigación que permite lograr los objetivos y probar las hipótesis planteadas.", 4, 3, 2),
                    new Criterio("poblacion", "Población de estudio",
                            "Describe la población o universo para la cual serán válidas las conclusiones que se obtengan.", 3, 2, 1),
                    new Criterio("unidad", "Unidad de análisis",
                            "Describe los sujetos u objetos de estudio que serán materia de la investigación.", 3, 2, 1),
                    new Criterio("tamano", "Tamaño de la muestra",
                            "Describe el tamaño de muestra según el tipo de análisis (descriptivo o inferencial), con criterios estadísticos.", 3, 2, 1),
                    new Criterio("seleccion", "Selección de la muestra",
                            "Describe con claridad el método de selección de la muestra, vinculado a la representatividad.", 4, 3, 2),
                    new Criterio("recoleccion", "Plan de recolección de datos",
                            "Técnicas e instrumentos con validez y confiabilidad (o estudio piloto). En estudios mixtos: técnicas y análisis cualitativo.", 3, 2, 1),
                    new Criterio("analisis", "Plan de análisis de los datos",
                            "Cómo se describen los datos y se infieren las conclusiones, según el tipo de variables, escala de medición y muestra.", 3, 2, 1),
                    new Criterio("eticos", "Aspectos éticos en la investigación",
                            "Cómo se cumplirán los aspectos éticos con sujetos humanos o animales; consentimiento informado; organismo revisor.", 2, 1, 0))),
            new Seccion("presupuesto", "IV. Presupuesto, cronograma, bibliografía", List.of(
                    new Criterio("presupuesto", "Presupuesto",
                            "Distribuido por rubros (Remuneraciones, Bienes y Servicios); aquí se evalúa si el proyecto es viable de realizar.", 1, 0, 0),
                    new Criterio("cronograma", "Cronograma de actividades",
                            "Cronograma gráfico que representa las actividades a realizar con sus respectivos tiempos.", 1, 0, 0),
                    new Criterio("referencias", "Referencias bibliográficas",
                            "Según estilo APA; actualizada, nacional e internacional; incluye libros, revistas científicas, tesis, entre otros.", 5, 3, 2))),
            new Seccion("formales", "V. Aspectos formales", List.of(
                    new Criterio("presentacion", "Presentación ordenada, limpia",
                            "Presentación ordenada y limpia del documento.", 3, 2, 1),
                    new Criterio("redaccion", "Redacción, sintaxis y ortografía",
                            "Redacción, sintaxis y ortografía correctas.", 5, 3, 2)))));

    // ── Rúbrica CUALITATIVA (total 100) ───────────────────────────────────────
    public static final Rubrica CUALITATIVA = new Rubrica("CUALITATIVO",
            "Proyecto de Investigación con enfoque Cualitativo", List.of(
            new Seccion("problema", "I. El problema", List.of(
                    new Criterio("situacion", "Situación del problema",
                            "Evidencia la construcción del objeto de estudio (contacto teórico y práctico con la situación); importancia, novedad, interés y viabilidad; describe qué se ha investigado y qué falta.", 10, 8, 5),
                    new Criterio("formulacion", "Formulación del problema",
                            "Pregunta de investigación derivada de la situación; objeto reconocible y definido; plantea algo nuevo; formulada de manera clara.", 6, 4, 2),
                    new Criterio("justificacion", "Justificación de la investigación",
                            "Conveniencia teórica, práctica o metodológica; pertinencia, finalidad, importancia, beneficios y beneficiarios; aporte principal.", 3, 2, 1),
                    new Criterio("objetivos", "Objetivos de la investigación",
                            "Finalidad del estudio, en estrecha relación con la pregunta; claros y adecuados al abordaje cualitativo; generales y específicos.", 5, 3, 1))),
            new Seccion("marco", "II. Marco teórico referencial", List.of(
                    new Criterio("fundamentacion", "Fundamentación filosófica, sociológica y epistemológica",
                            "Soporte filosófico-social-epistemológico y presupuestos teóricos que sustentan el estudio y el posicionamiento del autor.", 15, 10, 5),
                    new Criterio("antecedentes", "Antecedentes del estudio",
                            "Estudios de los últimos 10 años relacionados; orden cronológico; revisión crítica (estado del arte, vacíos); resumen parafraseado; matriz de antecedentes.", 10, 5, 3),
                    new Criterio("bases", "Bases conceptuales",
                            "Soporte teórico-conceptual inicial que dará sustento al estudio, debidamente estructurado y sustentado.", 10, 5, 3))),
            new Seccion("metodologia", "III. Abordaje metodológico", List.of(
                    new Criterio("abordaje", "Abordaje y tipo de estudio",
                            "Referencial teórico-metodológico que sustenta el estudio; abordaje del tema adecuado para la problemática.", 5, 3, 2),
                    new Criterio("escenario", "Escenario del estudio",
                            "Descripción pormenorizada del lugar donde se obtendrá la información, con los aspectos relevantes del contexto social.", 3, 2, 1),
                    new Criterio("sujetos", "Sujetos de estudio",
                            "Describe los sujetos, su selección y el tipo de muestreo cualitativo; criterios de inclusión y exclusión.", 4, 2, 1),
                    new Criterio("recoleccion", "Plan de recolección de datos",
                            "Plan desde el contacto con los investigados; técnicas e instrumentos cualitativos adecuados al abordaje; instrumentos en anexos.", 6, 4, 2),
                    new Criterio("analisis", "Plan de análisis e interpretación de los datos",
                            "Tipo y proceso del análisis cualitativo; procedimientos y lógica para organizar, sintetizar, conceptualizar y relacionar los datos.", 6, 4, 2),
                    new Criterio("eticos", "Aspectos éticos en la investigación",
                            "Organismo o comité de ética; cómo se cumplirán los aspectos éticos con las personas seleccionadas; consentimiento informado.", 2, 1, 0))),
            new Seccion("presupuesto", "IV. Presupuesto, cronograma, referencias", List.of(
                    new Criterio("presupuesto", "Presupuesto",
                            "Distribuido por rubros (Remuneraciones, Bienes y Servicios); financiamiento si lo hubiera; evalúa la viabilidad.", 1, 0, 0),
                    new Criterio("cronograma", "Cronograma de actividades",
                            "Cronograma gráfico que representa las distintas actividades y sus tiempos de ejecución.", 1, 0, 0),
                    new Criterio("referencias", "Referencias bibliográficas",
                            "Orden alfabético; antigüedad no mayor a 10 años; libros, artículos científicos, tesis, bases de datos; formato APA.", 5, 3, 2))),
            new Seccion("formales", "V. Aspectos formales", List.of(
                    new Criterio("titulo", "Título de la tesis",
                            "Guarda relación con la propuesta de investigación; no más de 15 palabras; sin país ni año de ejecución.", 3, 2, 1),
                    new Criterio("redaccion", "Redacción, sintaxis y ortografía",
                            "Redacción, sintaxis y ortografía correctas.", 5, 3, 2)))));

    /** Rúbrica que corresponde al enfoque del proyecto (CUALITATIVO → cualitativa; resto → cuantitativa/mixta). */
    public static Rubrica porEnfoque(String enfoque) {
        return "CUALITATIVO".equalsIgnoreCase(enfoque) ? CUALITATIVA : CUANTITATIVA;
    }

    /** ¿El puntaje total aprueba el proyecto? */
    public static boolean aprobado(int total) {
        return total >= APROBADO_MIN;
    }
}
