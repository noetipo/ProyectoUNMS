package unmsm.edu.pe.tesis.application.util;

import unmsm.edu.pe.tesis.domain.entities.ProyectoReferencia;
import unmsm.edu.pe.tesis.domain.enums.EstiloCita;
import unmsm.edu.pe.tesis.domain.enums.TipoReferencia;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatea una {@link ProyectoReferencia} en el estilo elegido (APA 7 / Vancouver / IEEE),
 * a la manera de un gestor tipo Mendeley. Los autores se ingresan como
 * "Apellido, Iniciales; Apellido, Iniciales" (ej. "García, JM; Pérez, M").
 */
public final class CitationFormatter {

    private CitationFormatter() {
    }

    private record Autor(String apellidos, String iniciales) {
    }

    /** Cita completa de una referencia en el estilo dado. */
    public static String formatear(ProyectoReferencia r, EstiloCita estilo) {
        if (r == null) return "";
        TipoReferencia tipo = r.getTipo() != null ? r.getTipo() : TipoReferencia.ARTICULO;
        return switch (estilo != null ? estilo : EstiloCita.APA) {
            case APA -> apa(r, tipo);
            case VANCOUVER -> vancouver(r, tipo);
            case IEEE -> ieee(r, tipo);
            case HARVARD -> harvard(r, tipo);
            case MLA -> mla(r, tipo);
            case CHICAGO -> chicago(r, tipo);
        };
    }

    /**
     * Cita para insertar dentro del texto (no la entrada de bibliografía):
     * APA → {@code (García et al., 2023)}; Vancouver/IEEE → {@code [n]} (n = posición en la lista).
     */
    public static String citaEnTexto(ProyectoReferencia r, EstiloCita estilo, int numero) {
        if (r == null) return "";
        EstiloCita e = estilo != null ? estilo : EstiloCita.APA;
        if (e.esNumerico()) return "[" + numero + "]";
        return switch (e) {
            case APA -> autorAnio(r, " & ", ", ", true);      // (García & Pérez, 2023)
            case HARVARD -> autorAnio(r, " and ", ", ", true); // (García and Pérez, 2023)
            case CHICAGO -> autorAnio(r, " and ", " ", true);  // (García and Pérez 2023)
            case MLA -> autorAnio(r, " and ", "", false);      // (García and Pérez)
            default -> "[" + numero + "]";
        };
    }

    /**
     * Cita narrativa (el autor forma parte de la oración): {@code Pérez (2023)} / {@code Pérez et al. [1]}.
     */
    public static String citaNarrativa(ProyectoReferencia r, EstiloCita estilo, int numero) {
        if (r == null) return "";
        EstiloCita e = estilo != null ? estilo : EstiloCita.APA;
        String autores = autoresNarrativos(parse(r.getAutores()), r);
        if (e.esNumerico()) return autores.isBlank() ? "[" + numero + "]" : autores + " [" + numero + "]";
        if (e == EstiloCita.MLA) return autores;            // MLA narrativa: solo el autor
        String anio = nz(r.getAnio(), "s. f.");
        return autores.isBlank() ? "(" + anio + ")" : autores + " (" + anio + ")";
    }

    private static String autoresNarrativos(List<Autor> a, ProyectoReferencia r) {
        if (a.isEmpty()) return has(r.getTitulo()) ? recorte(r.getTitulo()) : "";
        if (a.size() == 1) return a.get(0).apellidos();
        if (a.size() == 2) return a.get(0).apellidos() + " and " + a.get(1).apellidos();
        return a.get(0).apellidos() + " et al.";
    }

    /**
     * Cita autor-año entre paréntesis. {@code union} = separador antes del último autor;
     * {@code sepAnio} = separador autor→año; {@code conAnio} = incluir el año.
     */
    private static String autorAnio(ProyectoReferencia r, String union, String sepAnio, boolean conAnio) {
        String anio = nz(r.getAnio(), "s. f.");
        List<Autor> a = parse(r.getAutores());
        String autores;
        if (a.isEmpty()) {
            autores = has(r.getTitulo()) ? recorte(r.getTitulo()) : "Anónimo";
        } else if (a.size() == 1) {
            autores = a.get(0).apellidos();
        } else if (a.size() == 2) {
            autores = a.get(0).apellidos() + union + a.get(1).apellidos();
        } else {
            autores = a.get(0).apellidos() + " et al.";
        }
        return conAnio ? "(" + autores + sepAnio + anio + ")" : "(" + autores + ")";
    }

    /** Primeras palabras del título (para citar por título cuando no hay autor). */
    private static String recorte(String t) {
        String[] palabras = t.trim().split("\\s+");
        int n = Math.min(3, palabras.length);
        return String.join(" ", java.util.Arrays.copyOfRange(palabras, 0, n));
    }

    // ─────────────────────────── APA 7 ───────────────────────────
    private static String apa(ProyectoReferencia r, TipoReferencia tipo) {
        StringBuilder sb = new StringBuilder();
        String autores = autoresApa(parse(r.getAutores()));
        if (!autores.isBlank()) sb.append(autores).append(' ');
        sb.append('(').append(nz(r.getAnio(), "s. f.")).append("). ");
        switch (tipo) {
            case LIBRO -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append('.');
            }
            case CAPITULO_LIBRO -> {
                sb.append(nz(r.getTitulo())).append(". En ").append(italic(nz(r.getFuente()))).append(". ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append('.');
            }
            case TESIS -> {
                sb.append(italic(nz(r.getTitulo()))).append(" [Tesis]. ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append('.');
            }
            case PAGINA_WEB -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getFuente())) sb.append(r.getFuente()).append(". ");
            }
            case INFORME -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append('.');
            }
            default -> { // ARTICULO
                sb.append(nz(r.getTitulo())).append(". ");
                if (has(r.getFuente())) sb.append(italic(r.getFuente()));
                if (has(r.getVolumen())) sb.append(", ").append(italic(r.getVolumen()));
                if (has(r.getNumero())) sb.append('(').append(r.getNumero()).append(')');
                if (has(r.getPaginas())) sb.append(", ").append(r.getPaginas());
                sb.append('.');
            }
        }
        String enlace = enlace(r);
        if (enlace != null) sb.append(' ').append(enlace);
        return limpiar(sb.toString());
    }

    // ─────────────────────────── Vancouver ───────────────────────────
    private static String vancouver(ProyectoReferencia r, TipoReferencia tipo) {
        StringBuilder sb = new StringBuilder();
        String autores = autoresVancouver(parse(r.getAutores()));
        if (!autores.isBlank()) sb.append(autores).append(". ");
        switch (tipo) {
            case LIBRO, TESIS, INFORME -> {
                sb.append(nz(r.getTitulo())).append(". ");
                if (has(r.getCiudad())) sb.append(r.getCiudad()).append(": ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append("; ");
                sb.append(nz(r.getAnio())).append('.');
            }
            case CAPITULO_LIBRO -> {
                sb.append(nz(r.getTitulo())).append(". En: ").append(nz(r.getFuente())).append(". ");
                if (has(r.getCiudad())) sb.append(r.getCiudad()).append(": ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append("; ");
                sb.append(nz(r.getAnio()));
                if (has(r.getPaginas())) sb.append(". p. ").append(r.getPaginas());
                sb.append('.');
            }
            case PAGINA_WEB -> {
                sb.append(nz(r.getTitulo())).append(" [Internet]. ").append(nz(r.getAnio()));
                if (has(r.getFechaAcceso())) sb.append(" [citado ").append(r.getFechaAcceso()).append(']');
                sb.append('.');
                if (has(r.getUrl())) sb.append(" Disponible en: ").append(r.getUrl());
            }
            default -> { // ARTICULO
                sb.append(nz(r.getTitulo())).append(". ");
                if (has(r.getFuente())) sb.append(r.getFuente()).append(". ");
                sb.append(nz(r.getAnio()));
                if (has(r.getVolumen())) sb.append(';').append(r.getVolumen());
                if (has(r.getNumero())) sb.append('(').append(r.getNumero()).append(')');
                if (has(r.getPaginas())) sb.append(':').append(r.getPaginas());
                sb.append('.');
            }
        }
        if (has(r.getDoi()) && tipo == TipoReferencia.ARTICULO) sb.append(" doi:").append(r.getDoi());
        return limpiar(sb.toString());
    }

    // ─────────────────────────── IEEE ───────────────────────────
    private static String ieee(ProyectoReferencia r, TipoReferencia tipo) {
        StringBuilder sb = new StringBuilder();
        String autores = autoresIeee(parse(r.getAutores()));
        if (!autores.isBlank()) sb.append(autores).append(", ");
        switch (tipo) {
            case LIBRO, TESIS, INFORME -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getCiudad())) sb.append(r.getCiudad()).append(": ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append(", ");
                sb.append(nz(r.getAnio())).append('.');
            }
            case PAGINA_WEB -> {
                sb.append('"').append(nz(r.getTitulo())).append(",\" ");
                if (has(r.getFuente())) sb.append(r.getFuente()).append(", ");
                sb.append(nz(r.getAnio())).append(". [En línea].");
                if (has(r.getUrl())) sb.append(" Disponible: ").append(r.getUrl());
            }
            default -> { // ARTICULO / CAPITULO
                sb.append('"').append(nz(r.getTitulo())).append(",\" ");
                if (has(r.getFuente())) sb.append(italic(r.getFuente())).append(", ");
                if (has(r.getVolumen())) sb.append("vol. ").append(r.getVolumen()).append(", ");
                if (has(r.getNumero())) sb.append("no. ").append(r.getNumero()).append(", ");
                if (has(r.getPaginas())) sb.append("pp. ").append(r.getPaginas()).append(", ");
                sb.append(nz(r.getAnio())).append('.');
            }
        }
        return limpiar(sb.toString());
    }

    // ─────────────────────────── Harvard ───────────────────────────
    private static String harvard(ProyectoReferencia r, TipoReferencia tipo) {
        StringBuilder sb = new StringBuilder();
        String aut = autoresApellidoInicial(parse(r.getAutores()), " and ");
        if (!aut.isBlank()) sb.append(aut).append(' ');
        sb.append('(').append(nz(r.getAnio(), "s. f.")).append(") ");
        switch (tipo) {
            case LIBRO, TESIS, INFORME -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getCiudad())) sb.append(r.getCiudad()).append(": ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append('.');
            }
            default -> {
                sb.append('\'').append(nz(r.getTitulo())).append("', ");
                if (has(r.getFuente())) sb.append(italic(r.getFuente()));
                if (has(r.getVolumen())) sb.append(", ").append(r.getVolumen());
                if (has(r.getNumero())) sb.append('(').append(r.getNumero()).append(')');
                if (has(r.getPaginas())) sb.append(", pp. ").append(r.getPaginas());
                sb.append('.');
            }
        }
        return limpiar(sb.toString());
    }

    // ─────────────────────────── MLA 9 ───────────────────────────
    private static String mla(ProyectoReferencia r, TipoReferencia tipo) {
        StringBuilder sb = new StringBuilder();
        String aut = autoresMlaChicago(parse(r.getAutores()));
        if (!aut.isBlank()) sb.append(aut).append(". ");
        switch (tipo) {
            case LIBRO, TESIS, INFORME -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append(", ");
                sb.append(nz(r.getAnio())).append('.');
            }
            default -> {
                sb.append('"').append(nz(r.getTitulo())).append(".\" ");
                if (has(r.getFuente())) sb.append(italic(r.getFuente())).append(", ");
                if (has(r.getVolumen())) sb.append("vol. ").append(r.getVolumen()).append(", ");
                if (has(r.getNumero())) sb.append("no. ").append(r.getNumero()).append(", ");
                sb.append(nz(r.getAnio()));
                if (has(r.getPaginas())) sb.append(", pp. ").append(r.getPaginas());
                sb.append('.');
            }
        }
        return limpiar(sb.toString());
    }

    // ─────────────────────────── Chicago (autor-año) ───────────────────────────
    private static String chicago(ProyectoReferencia r, TipoReferencia tipo) {
        StringBuilder sb = new StringBuilder();
        String aut = autoresMlaChicago(parse(r.getAutores()));
        if (!aut.isBlank()) sb.append(aut).append(". ");
        sb.append(nz(r.getAnio(), "s. f.")).append(". ");
        switch (tipo) {
            case LIBRO, TESIS, INFORME -> {
                sb.append(italic(nz(r.getTitulo()))).append(". ");
                if (has(r.getCiudad())) sb.append(r.getCiudad()).append(": ");
                if (has(r.getEditorial())) sb.append(r.getEditorial()).append('.');
            }
            default -> {
                sb.append('"').append(nz(r.getTitulo())).append(".\" ");
                if (has(r.getFuente())) sb.append(italic(r.getFuente())).append(' ');
                if (has(r.getVolumen())) sb.append(r.getVolumen());
                if (has(r.getNumero())) sb.append(" (").append(r.getNumero()).append(')');
                if (has(r.getPaginas())) sb.append(": ").append(r.getPaginas());
                sb.append('.');
            }
        }
        return limpiar(sb.toString());
    }

    // ─────────────────────────── autores ───────────────────────────
    private static List<Autor> parse(String autores) {
        List<Autor> out = new ArrayList<>();
        if (autores == null || autores.isBlank()) return out;
        for (String parte : autores.split(";")) {
            String p = parte.trim();
            if (p.isEmpty()) continue;
            int coma = p.indexOf(',');
            if (coma >= 0) {
                out.add(new Autor(p.substring(0, coma).trim(), p.substring(coma + 1).trim()));
            } else {
                // sin coma: intenta separar el último token como iniciales
                int esp = p.lastIndexOf(' ');
                if (esp > 0 && p.substring(esp + 1).length() <= 3) {
                    out.add(new Autor(p.substring(0, esp).trim(), p.substring(esp + 1).trim()));
                } else {
                    out.add(new Autor(p, ""));
                }
            }
        }
        return out;
    }

    /** Iniciales con puntos y espacios: "JM" -> "J. M.". */
    private static String inicialesPunteadas(String ini) {
        if (ini == null || ini.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : ini.replace(".", "").replace(" ", "").toCharArray()) {
            sb.append(Character.toUpperCase(c)).append(". ");
        }
        return sb.toString().trim();
    }

    /** Iniciales pegadas sin puntos: "J. M." -> "JM". */
    private static String inicialesPegadas(String ini) {
        if (ini == null) return "";
        return ini.replace(".", "").replace(" ", "").toUpperCase();
    }

    private static String autoresApa(List<Autor> a) {
        if (a.isEmpty()) return "";
        List<String> partes = new ArrayList<>();
        for (Autor au : a) {
            String ini = inicialesPunteadas(au.iniciales());
            partes.add(ini.isBlank() ? au.apellidos() : au.apellidos() + ", " + ini);
        }
        return unir(partes, ", ", ", & ");
    }

    private static String autoresVancouver(List<Autor> a) {
        if (a.isEmpty()) return "";
        List<String> partes = new ArrayList<>();
        int limite = Math.min(a.size(), 6);
        for (int i = 0; i < limite; i++) {
            Autor au = a.get(i);
            String ini = inicialesPegadas(au.iniciales());
            partes.add(ini.isBlank() ? au.apellidos() : au.apellidos() + " " + ini);
        }
        String base = String.join(", ", partes);
        return a.size() > 6 ? base + ", et al" : base;
    }

    private static String autoresIeee(List<Autor> a) {
        if (a.isEmpty()) return "";
        List<String> partes = new ArrayList<>();
        for (Autor au : a) {
            String ini = inicialesPunteadas(au.iniciales());
            partes.add(ini.isBlank() ? au.apellidos() : ini + " " + au.apellidos());
        }
        return unir(partes, ", ", " and ");
    }

    /** "Apellido, I. I." para todos (Harvard); {@code sepFinal} = " and " / ", & ". */
    private static String autoresApellidoInicial(List<Autor> a, String sepFinal) {
        if (a.isEmpty()) return "";
        List<String> partes = new ArrayList<>();
        for (Autor au : a) {
            String ini = inicialesPunteadas(au.iniciales());
            partes.add(ini.isBlank() ? au.apellidos() : au.apellidos() + ", " + ini);
        }
        return unir(partes, ", ", sepFinal);
    }

    /** MLA/Chicago: 1.º "Apellido, I. I."; resto "I. I. Apellido"; 3+ → "Apellido, I. I., et al.". */
    private static String autoresMlaChicago(List<Autor> a) {
        if (a.isEmpty()) return "";
        Autor p = a.get(0);
        String primero = inicialesPunteadas(p.iniciales()).isBlank()
                ? p.apellidos() : p.apellidos() + ", " + inicialesPunteadas(p.iniciales());
        if (a.size() == 1) return primero;
        if (a.size() >= 3) return primero + ", et al.";
        Autor s = a.get(1);
        String segundo = inicialesPunteadas(s.iniciales()).isBlank()
                ? s.apellidos() : inicialesPunteadas(s.iniciales()) + " " + s.apellidos();
        return primero + ", and " + segundo;
    }

    private static String unir(List<String> partes, String sep, String sepFinal) {
        if (partes.isEmpty()) return "";
        if (partes.size() == 1) return partes.get(0);
        String cuerpo = String.join(sep, partes.subList(0, partes.size() - 1));
        return cuerpo + sepFinal + partes.get(partes.size() - 1);
    }

    // ─────────────────────────── util ───────────────────────────
    /** Marca de énfasis (cursiva) que el exportador interpreta; se elimina en texto plano. */
    private static String italic(String s) {
        return "«i»" + s + "«/i»";
    }

    private static String enlace(ProyectoReferencia r) {
        if (has(r.getDoi())) return "https://doi.org/" + r.getDoi().replaceFirst("^https?://doi.org/", "");
        if (has(r.getUrl())) return r.getUrl();
        return null;
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return has(s) ? s.trim() : "";
    }

    private static String nz(String s, String def) {
        return has(s) ? s.trim() : def;
    }

    private static String limpiar(String s) {
        return s.replaceAll("\\s+\\.", ".").replaceAll("\\.\\.", ".").replaceAll("\\s{2,}", " ").trim();
    }
}
