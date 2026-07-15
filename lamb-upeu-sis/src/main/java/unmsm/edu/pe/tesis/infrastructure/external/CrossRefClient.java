package unmsm.edu.pe.tesis.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ReferenciaRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Autocompleta una referencia a partir de su DOI. Estrategia en dos pasos:
 * 1) CrossRef (rápido, con filter+select; cubre casi todos los artículos y libros).
 * 2) Fallback al resolvedor DOI (https://doi.org/{doi} con CSL-JSON), que funciona para
 *    CUALQUIER agencia — DataCite (tesis, datasets, Zenodo), etc.
 */
@ApplicationScoped
public class CrossRefClient {

    @Inject
    ObjectMapper mapper;

    // ── Configuración (application.yml → doi.autocompletar.*) ──
    @ConfigProperty(name = "doi.autocompletar.crossref-url", defaultValue = "https://api.crossref.org/works")
    String crossrefUrl;
    @ConfigProperty(name = "doi.autocompletar.resolver-url", defaultValue = "https://doi.org")
    String resolverUrl;
    @ConfigProperty(name = "doi.autocompletar.openalex-url", defaultValue = "https://api.openalex.org/works")
    String openalexUrl;
    @ConfigProperty(name = "doi.autocompletar.mailto", defaultValue = "posgrado@unmsm.edu.pe")
    String mailto;
    @ConfigProperty(name = "doi.autocompletar.user-agent", defaultValue = "UNMSM-Tesis/1.0 (mailto:posgrado@unmsm.edu.pe)")
    String userAgent;
    @ConfigProperty(name = "doi.autocompletar.select",
            defaultValue = "title,author,container-title,volume,issue,page,issued,published-print,published-online,type,publisher")
    String select;
    @ConfigProperty(name = "doi.autocompletar.connect-timeout", defaultValue = "6s")
    Duration connectTimeout;
    @ConfigProperty(name = "doi.autocompletar.request-timeout", defaultValue = "12s")
    Duration requestTimeout;
    @ConfigProperty(name = "doi.autocompletar.fallback-enabled", defaultValue = "true")
    boolean fallbackEnabled;

    private HttpClient http;

    @PostConstruct
    void init() {
        http = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public ReferenciaRequest porDoi(String doiRaw) {
        String doi = normalizar(doiRaw);
        if (doi.isBlank()) {
            throw new ValidationException("Ingresa un DOI para autocompletar");
        }
        ReferenciaRequest r = consultarCrossRef(doi);        // rápido, ~la mayoría de artículos/libros
        if (r == null && fallbackEnabled) {
            r = consultarDoiOrg(doi);                        // fallback universal (DataCite, etc.)
        }
        if (r == null) {
            throw new BusinessException("No se encontró el DOI: " + doi
                    + ". Verifica que esté bien escrito o completa los campos manualmente.");
        }
        return r;
    }

    /** Búsqueda de candidatos por título/autor (sin DOI), para autocompletar tesis, libros, etc. */
    public java.util.List<ReferenciaRequest> buscarPorTitulo(String q) {
        if (q == null || q.isBlank()) return java.util.List.of();
        try {
            String query = URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
            String url = crossrefUrl + "?mailto=" + URLEncoder.encode(mailto, StandardCharsets.UTF_8)
                    + "&rows=6&query.bibliographic=" + query + "&select=" + select + ",DOI";
            HttpResponse<String> resp = get(url, "application/json");
            if (resp.statusCode() != 200) return java.util.List.of();
            JsonNode items = mapper.readTree(resp.body()).path("message").path("items");
            java.util.List<ReferenciaRequest> out = new java.util.ArrayList<>();
            if (items.isArray()) {
                for (JsonNode it : items) {
                    ReferenciaRequest r = mapear(it, it.path("DOI").asText(null));
                    if (r.getTitulo() != null) out.add(r);
                }
            }
            return out;
        } catch (IOException ex) {
            return java.util.List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return java.util.List.of();
        }
    }

    /** Búsqueda por título en OpenAlex (cobertura amplia: conferencias, preprints, tesis). */
    public java.util.List<ReferenciaRequest> buscarEnOpenAlex(String q) {
        if (q == null || q.isBlank()) return java.util.List.of();
        try {
            String consulta = URLEncoder.encode(q.trim(), StandardCharsets.UTF_8).replace("+", "%20");
            String url = openalexUrl + "?per-page=6&mailto=" + URLEncoder.encode(mailto, StandardCharsets.UTF_8)
                    + "&select=title,display_name,publication_year,doi,type,authorships,primary_location,biblio"
                    + "&filter=title.search:" + consulta;
            HttpResponse<String> resp = get(url, "application/json");
            if (resp.statusCode() != 200) return java.util.List.of();
            JsonNode results = mapper.readTree(resp.body()).path("results");
            java.util.List<ReferenciaRequest> out = new java.util.ArrayList<>();
            if (results.isArray()) {
                for (JsonNode w : results) {
                    ReferenciaRequest r = mapearOpenAlex(w);
                    if (r.getTitulo() != null) out.add(r);
                }
            }
            return out;
        } catch (IOException ex) {
            return java.util.List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return java.util.List.of();
        }
    }

    static ReferenciaRequest mapearOpenAlex(JsonNode w) {
        ReferenciaRequest r = new ReferenciaRequest();
        String doi = w.path("doi").asText(null);
        r.setDoi(doi != null ? doi.replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "") : null);
        r.setTipo(tipoOpenAlex(w.path("type").asText("")));
        String title = w.path("title").asText(null);
        if (title == null || title.isBlank()) title = w.path("display_name").asText(null);
        r.setTitulo(nz(title));
        int y = w.path("publication_year").asInt(0);
        if (y > 0) r.setAnio(String.valueOf(y));
        r.setFuente(nz(w.path("primary_location").path("source").path("display_name").asText(null)));
        JsonNode biblio = w.path("biblio");
        r.setVolumen(nz(biblio.path("volume").asText(null)));
        r.setNumero(nz(biblio.path("issue").asText(null)));
        String fp = biblio.path("first_page").asText(null), lp = biblio.path("last_page").asText(null);
        if (fp != null && !fp.isBlank()) {
            r.setPaginas(lp != null && !lp.isBlank() && !lp.equals(fp) ? fp + "-" + lp : fp);
        }
        r.setAutores(autoresOpenAlex(w.path("authorships")));
        return r;
    }

    private static String tipoOpenAlex(String t) {
        return switch (t) {
            case "book" -> "LIBRO";
            case "book-chapter" -> "CAPITULO_LIBRO";
            case "dissertation" -> "TESIS";
            case "report", "dataset" -> "INFORME";
            default -> "ARTICULO"; // article, preprint, paper-conference, etc.
        };
    }

    /** OpenAlex trae nombres completos ("Nikhil Sontakke"); los pasa a "Apellido, Iniciales". */
    private static String autoresOpenAlex(JsonNode authorships) {
        if (authorships == null || !authorships.isArray() || authorships.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        int max = Math.min(authorships.size(), 12);
        for (int i = 0; i < max; i++) {
            String name = authorships.get(i).path("author").path("display_name").asText("").trim();
            if (name.isEmpty()) continue;
            String[] parts = name.split("\\s+");
            String apellido = parts[parts.length - 1];
            StringBuilder ini = new StringBuilder();
            for (int j = 0; j < parts.length - 1; j++) {
                if (!parts[j].isBlank()) ini.append(Character.toUpperCase(parts[j].charAt(0)));
            }
            sb.append(apellido);
            if (ini.length() > 0) sb.append(", ").append(ini);
            sb.append("; ");
        }
        String out = sb.toString().trim();
        return out.endsWith(";") ? out.substring(0, out.length() - 1).trim() : out;
    }

    /** CrossRef: endpoint de lista con filter+select (payload mínimo, ~3 KB). Devuelve null si no está o falla. */
    private ReferenciaRequest consultarCrossRef(String doi) {
        try {
            String filtro = URLEncoder.encode(doi, StandardCharsets.UTF_8);
            String url = crossrefUrl + "?mailto=" + URLEncoder.encode(mailto, StandardCharsets.UTF_8)
                    + "&rows=1&filter=doi:" + filtro + "&select=" + select;
            HttpResponse<String> resp = get(url, "application/json");
            if (resp.statusCode() != 200) return null;
            JsonNode items = mapper.readTree(resp.body()).path("message").path("items");
            return (items.isArray() && !items.isEmpty()) ? mapear(items.get(0), doi) : null;
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Fallback: resolvedor DOI con negociación de contenido CSL-JSON (cualquier agencia). */
    private ReferenciaRequest consultarDoiOrg(String doi) {
        try {
            String enc = doi.replace(" ", "%20");
            HttpResponse<String> resp = get(resolverUrl + "/" + enc, "application/vnd.citationstyles.csl+json");
            if (resp.statusCode() != 200) return null;
            return mapearCsl(mapper.readTree(resp.body()), doi);
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private HttpResponse<String> get(String url, String accept) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("User-Agent", userAgent)
                .header("Accept", accept)
                .GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // ─────────────────────── mapeo CrossRef (work) ───────────────────────
    static ReferenciaRequest mapear(JsonNode m, String doi) {
        ReferenciaRequest r = new ReferenciaRequest();
        r.setDoi(doi);
        r.setTipo(tipo(m.path("type").asText("")));
        r.setTitulo(primer(m.path("title")));
        r.setFuente(primer(m.path("container-title")));
        r.setVolumen(texto(m, "volume"));
        r.setNumero(texto(m, "issue"));
        r.setPaginas(texto(m, "page"));
        r.setEditorial(texto(m, "publisher"));
        r.setAnio(anio(m));
        r.setAutores(autores(m.path("author"), "name"));
        return r;
    }

    private static String tipo(String crossref) {
        return switch (crossref) {
            case "book", "monograph", "reference-book" -> "LIBRO";
            case "book-chapter", "book-part", "book-section" -> "CAPITULO_LIBRO";
            case "dissertation" -> "TESIS";
            case "report", "report-component", "posted-content" -> "INFORME";
            default -> "ARTICULO";
        };
    }

    // ─────────────────────── mapeo CSL-JSON (doi.org) ───────────────────────
    static ReferenciaRequest mapearCsl(JsonNode m, String doi) {
        ReferenciaRequest r = new ReferenciaRequest();
        r.setDoi(doi);
        r.setTipo(tipoCsl(m.path("type").asText("")));
        r.setTitulo(textoFlex(m, "title"));            // en CSL suele ser texto, no arreglo
        r.setFuente(textoFlex(m, "container-title"));
        r.setVolumen(textoFlex(m, "volume"));
        r.setNumero(textoFlex(m, "issue"));
        r.setPaginas(textoFlex(m, "page"));
        r.setEditorial(textoFlex(m, "publisher"));
        r.setAnio(anio(m));
        r.setAutores(autores(m.path("author"), "literal"));
        return r;
    }

    private static String tipoCsl(String t) {
        return switch (t) {
            case "book" -> "LIBRO";
            case "chapter" -> "CAPITULO_LIBRO";
            case "thesis" -> "TESIS";
            case "report", "dataset" -> "INFORME";
            case "webpage", "post", "post-weblog" -> "PAGINA_WEB";
            default -> "ARTICULO"; // article-journal, article, paper-conference, etc.
        };
    }

    // ─────────────────────── helpers comunes ───────────────────────
    private static String primer(JsonNode arr) {
        if (arr != null && arr.isArray() && arr.size() > 0) {
            return arr.get(0).asText(null);
        }
        return null;
    }

    private static String texto(JsonNode m, String campo) {
        JsonNode n = m.path(campo);
        return n.isMissingNode() || n.isNull() ? null : nz(n.asText(null));
    }

    /** Lee un campo que puede venir como texto o como arreglo (robusto para CSL). */
    private static String textoFlex(JsonNode m, String campo) {
        JsonNode n = m.path(campo);
        if (n.isArray()) return primer(n);
        return n.isMissingNode() || n.isNull() ? null : nz(n.asText(null));
    }

    private static String anio(JsonNode m) {
        for (String clave : new String[]{"issued", "published-print", "published-online", "published", "created"}) {
            JsonNode partes = m.path(clave).path("date-parts");
            if (partes.isArray() && partes.size() > 0 && partes.get(0).isArray() && partes.get(0).size() > 0) {
                return partes.get(0).get(0).asText(null);
            }
        }
        return null;
    }

    /**
     * Une autores como "Apellido, Iniciales; Apellido, Iniciales".
     * {@code campoInstitucional} = "name" (CrossRef) o "literal" (CSL) para autores no personales.
     */
    private static String autores(JsonNode autores, String campoInstitucional) {
        if (autores == null || !autores.isArray() || autores.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (JsonNode a : autores) {
            String family = a.path("family").asText("").trim();
            String given = a.path("given").asText("").trim();
            if (family.isEmpty() && given.isEmpty()) {
                String inst = a.path(campoInstitucional).asText("").trim();
                if (!inst.isEmpty()) sb.append(inst).append("; ");
                continue;
            }
            String ini = iniciales(given);
            sb.append(family);
            if (!ini.isEmpty()) sb.append(", ").append(ini);
            sb.append("; ");
        }
        String out = sb.toString().trim();
        return out.endsWith(";") ? out.substring(0, out.length() - 1).trim() : out;
    }

    private static String iniciales(String given) {
        if (given == null || given.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String token : given.split("[\\s.\\-]+")) {
            if (!token.isBlank()) sb.append(Character.toUpperCase(token.charAt(0)));
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    static String normalizar(String doi) {
        if (doi == null) return "";
        String d = doi.trim();
        d = d.replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "");
        d = d.replaceFirst("(?i)^doi:\\s*", "");
        return d.trim();
    }
}
