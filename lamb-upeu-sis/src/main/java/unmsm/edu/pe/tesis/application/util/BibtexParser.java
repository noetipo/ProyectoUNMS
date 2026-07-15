package unmsm.edu.pe.tesis.application.util;

import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ReferenciaRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsea una entrada BibTeX (la que exportan Google Scholar y la mayoría de repositorios de tesis)
 * a una referencia, para rellenar la ficha sin escribir todo a mano.
 */
public final class BibtexParser {

    private BibtexParser() {
    }

    private static final Pattern TIPO = Pattern.compile("@(\\w+)\\s*\\{", Pattern.CASE_INSENSITIVE);
    // key = {valor (una anidación)} | "valor" | número
    private static final Pattern CAMPO = Pattern.compile(
            "(\\w+)\\s*=\\s*(\\{(?:[^{}]|\\{[^{}]*\\})*\\}|\"[^\"]*\"|\\d+)", Pattern.DOTALL);

    public static ReferenciaRequest parse(String bibtex) {
        if (bibtex == null || bibtex.isBlank()) {
            throw new ValidationException("Pega una entrada BibTeX (empieza con @…{ )");
        }
        if (!bibtex.contains("@") || !bibtex.contains("{")) {
            throw new ValidationException("No parece BibTeX. Copia la cita en formato BibTeX (Google Scholar → Citar → BibTeX).");
        }
        Matcher tm = TIPO.matcher(bibtex);
        String tipoBib = tm.find() ? tm.group(1).toLowerCase() : "misc";

        Map<String, String> f = new HashMap<>();
        Matcher cm = CAMPO.matcher(bibtex);
        while (cm.find()) {
            f.put(cm.group(1).toLowerCase(), desenvolver(cm.group(2)));
        }

        ReferenciaRequest r = new ReferenciaRequest();
        r.setTipo(tipoBibtex(tipoBib));
        r.setTitulo(f.get("title"));
        r.setAnio(soloAnio(coalesce(f.get("year"), f.get("date"))));
        r.setFuente(coalesce(f.get("journal"), f.get("journaltitle"), f.get("booktitle")));
        r.setEditorial(coalesce(f.get("publisher"), f.get("school"), f.get("institution")));
        r.setCiudad(f.get("address"));
        r.setVolumen(f.get("volume"));
        r.setNumero(f.get("number"));
        String pags = f.get("pages");
        r.setPaginas(pags != null ? pags.replace("--", "-").trim() : null);
        r.setDoi(f.get("doi"));
        r.setUrl(f.get("url"));
        r.setAutores(autoresBibtex(f.get("author")));

        if (r.getTitulo() == null || r.getTitulo().isBlank()) {
            throw new ValidationException("No se pudo leer el título del BibTeX; revisa que esté completo.");
        }
        return r;
    }

    private static String tipoBibtex(String t) {
        return switch (t) {
            case "book", "booklet" -> "LIBRO";
            case "inbook", "incollection" -> "CAPITULO_LIBRO";
            case "mastersthesis", "phdthesis", "thesis" -> "TESIS";
            case "techreport", "report" -> "INFORME";
            case "online", "electronic", "misc", "www" -> "PAGINA_WEB";
            default -> "ARTICULO"; // article, inproceedings, conference, etc.
        };
    }

    /** BibTeX: "Apellido, Nombre and Apellido, Nombre" o "Nombre Apellido and …" → "Apellido, Iniciales; …". */
    private static String autoresBibtex(String autores) {
        if (autores == null || autores.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        for (String parte : autores.split("(?i)\\s+and\\s+")) {
            String p = parte.trim();
            if (p.isEmpty() || p.equalsIgnoreCase("others")) continue;
            String apellido;
            String nombres;
            int coma = p.indexOf(',');
            if (coma >= 0) {                       // "Apellido, Nombres"
                apellido = p.substring(0, coma).trim();
                nombres = p.substring(coma + 1).trim();
            } else {                               // "Nombres Apellido"
                int esp = p.lastIndexOf(' ');
                if (esp > 0) {
                    apellido = p.substring(esp + 1).trim();
                    nombres = p.substring(0, esp).trim();
                } else {
                    apellido = p;
                    nombres = "";
                }
            }
            String ini = iniciales(nombres);
            sb.append(apellido);
            if (!ini.isEmpty()) sb.append(", ").append(ini);
            sb.append("; ");
        }
        String out = sb.toString().trim();
        return out.endsWith(";") ? out.substring(0, out.length() - 1).trim() : (out.isEmpty() ? null : out);
    }

    private static String iniciales(String nombres) {
        if (nombres == null || nombres.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String token : nombres.split("[\\s.\\-]+")) {
            if (!token.isBlank()) sb.append(Character.toUpperCase(token.charAt(0)));
        }
        return sb.toString();
    }

    /** Quita comillas externas, convierte acentos LaTeX y limpia llaves/comandos. */
    private static String desenvolver(String v) {
        if (v == null) return null;
        String s = v.trim();
        if ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("\"") && s.endsWith("\""))) {
            s = s.substring(1, s.length() - 1);
        }
        s = limpiarLatex(s);
        return s == null || s.isEmpty() ? null : s;
    }

    // {\"a} | \"{a} | \"a  → carácter acentuado
    private static final Pattern ACENTO = Pattern.compile("\\{?\\\\([\"'`^~])\\{?([a-zA-Z])\\}?\\}?");

    private static String limpiarLatex(String s) {
        if (s == null) return null;
        Matcher m = ACENTO.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(acentuar(m.group(1).charAt(0), m.group(2).charAt(0))));
        }
        m.appendTail(sb);
        String out = sb.toString()
                .replace("{\\ss}", "ß").replace("\\ss", "ß")
                .replace("{\\o}", "ø").replace("\\o", "ø")
                .replace("{\\aa}", "å").replace("\\aa", "å")
                .replaceAll("\\\\[a-zA-Z]+", "")   // comandos restantes (\emph, etc.)
                .replace("\\", "").replace("{", "").replace("}", "")
                .replaceAll("\\s+", " ").trim();
        return out;
    }

    private static String acentuar(char acc, char c) {
        String k = acc + String.valueOf(c);
        return switch (k) {
            case "\"a" -> "ä"; case "\"o" -> "ö"; case "\"u" -> "ü"; case "\"e" -> "ë"; case "\"i" -> "ï";
            case "\"A" -> "Ä"; case "\"O" -> "Ö"; case "\"U" -> "Ü";
            case "'a" -> "á"; case "'e" -> "é"; case "'i" -> "í"; case "'o" -> "ó"; case "'u" -> "ú"; case "'n" -> "ń";
            case "'A" -> "Á"; case "'E" -> "É"; case "'I" -> "Í"; case "'O" -> "Ó"; case "'U" -> "Ú";
            case "~n" -> "ñ"; case "~N" -> "Ñ"; case "~a" -> "ã"; case "~o" -> "õ";
            case "`a" -> "à"; case "`e" -> "è"; case "`i" -> "ì"; case "`o" -> "ò"; case "`u" -> "ù";
            case "^a" -> "â"; case "^e" -> "ê"; case "^i" -> "î"; case "^o" -> "ô"; case "^u" -> "û";
            default -> String.valueOf(c);
        };
    }

    private static String soloAnio(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("(\\d{4})").matcher(s);
        return m.find() ? m.group(1) : s.trim();
    }

    private static String coalesce(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
