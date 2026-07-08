package unmsm.edu.pe.tesis.application.util;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Utilidades de formato compartidas por los documentos de asesoría (Solicitud y Carta).
 * Centralizan fecha, grado→tratamiento, nivel→texto, condición→texto, tipo→texto y facultad,
 * para que ambos documentos (y ambos formatos, .docx/.pdf) sean coherentes.
 */
public final class FormatoDocumentos {

    private static final Locale ES = Locale.forLanguageTag("es");

    private FormatoDocumentos() {}

    /** "d de MMMM" en español, mes en minúscula. Ej: "5 de julio". */
    public static String fechaLarga(LocalDate fecha) {
        if (fecha == null) fecha = LocalDate.now();
        String mes = fecha.getMonth().getDisplayName(TextStyle.FULL, ES).toLowerCase(ES);
        return fecha.getDayOfMonth() + " de " + mes;
    }

    public static String anio(LocalDate fecha) {
        return String.valueOf((fecha != null ? fecha : LocalDate.now()).getYear());
    }

    /** NIVEL_ADJ: DOCTORADO → "doctoral"; MAESTRIA → "de maestría". */
    public static String nivelAdjetivo(String nivelEnum) {
        if (nivelEnum == null) return "";
        return switch (nivelEnum.toUpperCase()) {
            case "DOCTORADO" -> "doctoral";
            case "MAESTRIA", "MAESTRÍA" -> "de maestría";
            default -> "";
        };
    }

    /** NIVEL: "Doctorado" / "Maestría". */
    public static String nivelTexto(String nivelEnum) {
        if (nivelEnum == null) return "";
        return switch (nivelEnum.toUpperCase()) {
            case "DOCTORADO" -> "Doctorado";
            case "MAESTRIA", "MAESTRÍA" -> "Maestría";
            default -> nivelEnum;
        };
    }

    /** Tratamiento por grado principal y sexo. DOCTOR → Dr./Dra.; MAGISTER → Mg.; etc. */
    public static String tratamientoPorGrado(String gradoEnum, String sexoEnum) {
        if (gradoEnum == null) return "";
        boolean mujer = sexoEnum != null && sexoEnum.toUpperCase().startsWith("M") && sexoEnum.equalsIgnoreCase("MUJER");
        return switch (gradoEnum.toUpperCase()) {
            case "DOCTOR", "POST_DOCTORADO" -> mujer ? "Dra." : "Dr.";
            case "MAGISTER", "MAGÍSTER" -> "Mg.";
            case "LICENCIADO" -> "Lic.";
            case "SEGUNDA_ESPECIALIDAD" -> "Esp.";
            case "BACHILLER" -> "Bach.";
            default -> "";
        };
    }

    /** Tratamiento por sexo: MUJER → "doña"; HOMBRE → "don". */
    public static String tratamientoPorSexo(String sexoEnum) {
        if (sexoEnum == null) return "";
        return sexoEnum.equalsIgnoreCase("MUJER") ? "doña" : "don";
    }

    /** Sección del dictamen por nivel: DOCTORADO → "DOCTORADO"; MAESTRIA → "MAESTRÍA". */
    public static String nivelSeccion(String nivelEnum) {
        if (nivelEnum == null) return "";
        return switch (nivelEnum.toUpperCase()) {
            case "DOCTORADO" -> "DOCTORADO";
            case "MAESTRIA", "MAESTRÍA" -> "MAESTRÍA";
            default -> nivelEnum.toUpperCase();
        };
    }

    /** "Doctorado en Salud Pública" (nivel + programa sin prefijo). */
    public static String programaConNivel(String nivelEnum, String programaNombre) {
        String nivel = nivelTexto(nivelEnum);
        String prog = programaSinNivel(programaNombre);
        if (nivel.isBlank()) return prog;
        if (prog.isBlank()) return nivel;
        return nivel + " en " + prog;
    }

    /** CONDICION docente: NOMBRADO → "ordinario"; CONTRATADO → "contratado". */
    public static String condicionTexto(String condicionEnum) {
        if (condicionEnum == null) return "";
        return switch (condicionEnum.toUpperCase()) {
            case "NOMBRADO" -> "ordinario";
            case "CONTRATADO" -> "contratado";
            default -> condicionEnum.toLowerCase();
        };
    }

    /** TIPO_ASESOR: ASESOR → "asesor"; COASESOR → "co-asesor". */
    public static String tipoAsesorTexto(String tipoEnum) {
        if (tipoEnum == null) return "asesor";
        return switch (tipoEnum.toUpperCase()) {
            case "COASESOR", "CO_ASESOR" -> "co-asesor";
            default -> "asesor";
        };
    }

    /**
     * Nombre de facultad sin el prefijo "Facultad de " (la plantilla ya lo antepone).
     * Ej: "Facultad de Medicina" → "Medicina".
     */
    public static String facultadSinPrefijo(String facultadNombre) {
        if (facultadNombre == null) return "";
        String s = facultadNombre.trim();
        String lower = s.toLowerCase();
        if (lower.startsWith("facultad de ")) return s.substring("Facultad de ".length()).trim();
        if (lower.startsWith("facultad ")) return s.substring("Facultad ".length()).trim();
        return s;
    }

    /**
     * Nombre del programa sin el prefijo del nivel (la plantilla ya antepone "{NIVEL} en").
     * Ej: "DOCTORADO EN SALUD PÚBLICA" → "Salud Pública".
     */
    public static String programaSinNivel(String programaNombre) {
        if (programaNombre == null) return "";
        String s = programaNombre.trim();
        String upper = s.toUpperCase();
        for (String pref : new String[]{"DOCTORADO EN ", "MAESTRÍA EN ", "MAESTRIA EN "}) {
            if (upper.startsWith(pref)) { s = s.substring(pref.length()); break; }
        }
        return tituloCase(s);
    }

    private static final java.util.Set<String> CONECTORES =
            java.util.Set.of("de", "del", "la", "las", "los", "y", "e", "en", "el");

    /** Title Case respetando conectores en minúscula (excepto la primera palabra). */
    private static String tituloCase(String s) {
        if (s == null || s.isBlank()) return "";
        String[] palabras = s.trim().toLowerCase(ES).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < palabras.length; i++) {
            String w = palabras[i];
            if (i > 0) sb.append(" ");
            if (i > 0 && CONECTORES.contains(w)) {
                sb.append(w);
            } else {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return sb.toString();
    }

    public static String nz(String s) {
        return s == null ? "" : s;
    }
}
