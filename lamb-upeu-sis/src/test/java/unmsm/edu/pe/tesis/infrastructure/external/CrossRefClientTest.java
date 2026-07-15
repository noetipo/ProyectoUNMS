package unmsm.edu.pe.tesis.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.tesis.application.dto.ReferenciaRequest;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica el mapeo del JSON de CrossRef a una referencia (sin llamadas HTTP). */
class CrossRefClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void mapea_articuloDeRevista() {
        JsonNode m = json("""
                {
                  "type": "journal-article",
                  "title": ["Nanometre-scale thermometry in a living cell"],
                  "container-title": ["Nature"],
                  "volume": "500", "issue": "7460", "page": "54-58",
                  "author": [
                    {"given": "G.", "family": "Kucsko"},
                    {"given": "Peter C.", "family": "Maurer"}
                  ],
                  "issued": {"date-parts": [[2013, 7, 31]]}
                }
                """);

        ReferenciaRequest r = CrossRefClient.mapear(m, "10.1038/nature12373");

        assertEquals("ARTICULO", r.getTipo());
        assertEquals("Nanometre-scale thermometry in a living cell", r.getTitulo());
        assertEquals("Nature", r.getFuente());
        assertEquals("500", r.getVolumen());
        assertEquals("7460", r.getNumero());
        assertEquals("54-58", r.getPaginas());
        assertEquals("2013", r.getAnio());
        assertEquals("10.1038/nature12373", r.getDoi());
        // "G." -> "G" ; "Peter C." -> "PC"
        assertEquals("Kucsko, G; Maurer, PC", r.getAutores());
    }

    @Test
    void mapea_libro_yUsaAnioDePublishedCuandoNoHayIssued() {
        JsonNode m = json("""
                {
                  "type": "book",
                  "title": ["Metodologia de la investigacion"],
                  "publisher": "McGraw-Hill",
                  "author": [{"given": "Roberto", "family": "Hernandez"}],
                  "published-print": {"date-parts": [[2014]]}
                }
                """);

        ReferenciaRequest r = CrossRefClient.mapear(m, "10.0/libro");

        assertEquals("LIBRO", r.getTipo());
        assertEquals("McGraw-Hill", r.getEditorial());
        assertEquals("2014", r.getAnio());
        assertEquals("Hernandez, R", r.getAutores());
    }

    @Test
    void mapea_autorInstitucional_porCampoName() {
        JsonNode m = json("""
                {
                  "type": "report",
                  "title": ["Informe mundial sobre la diabetes"],
                  "author": [{"name": "Organizacion Mundial de la Salud"}],
                  "issued": {"date-parts": [[2016]]}
                }
                """);

        ReferenciaRequest r = CrossRefClient.mapear(m, "10.0/oms");

        assertEquals("INFORME", r.getTipo());
        assertEquals("Organizacion Mundial de la Salud", r.getAutores());
    }

    @Test
    void mapea_sinCamposOpcionales_noRompe() {
        JsonNode m = json("""
                { "type": "journal-article", "title": ["Solo titulo"] }
                """);

        ReferenciaRequest r = CrossRefClient.mapear(m, "10.0/x");

        assertEquals("Solo titulo", r.getTitulo());
        assertNull(r.getAutores());
        assertNull(r.getAnio());
        assertNull(r.getVolumen());
    }

    // ── mapeo CSL-JSON (fallback doi.org, cubre DataCite/tesis/datasets) ──
    @Test
    void mapeaCsl_articuloConTituloComoTexto() {
        JsonNode m = json("""
                {
                  "type": "article-journal",
                  "title": "Anemia en la sierra",
                  "container-title": "Revista Andina",
                  "volume": "12", "issue": "1", "page": "10-20",
                  "author": [{"family": "Chen", "given": "Xinyu"}],
                  "issued": {"date-parts": [[2018]]}
                }
                """);

        ReferenciaRequest r = CrossRefClient.mapearCsl(m, "10.5281/zenodo.1");

        assertEquals("ARTICULO", r.getTipo());
        assertEquals("Anemia en la sierra", r.getTitulo());   // título como texto plano, no arreglo
        assertEquals("Revista Andina", r.getFuente());
        assertEquals("12", r.getVolumen());
        assertEquals("2018", r.getAnio());
        assertEquals("Chen, X", r.getAutores());
    }

    @Test
    void mapeaCsl_tesisYDataset() {
        assertEquals("TESIS", CrossRefClient.mapearCsl(json("{\"type\":\"thesis\",\"title\":\"T\"}"), "10.0/t").getTipo());
        assertEquals("INFORME", CrossRefClient.mapearCsl(json("{\"type\":\"dataset\",\"title\":\"D\"}"), "10.0/d").getTipo());
        assertEquals("PAGINA_WEB", CrossRefClient.mapearCsl(json("{\"type\":\"webpage\",\"title\":\"W\"}"), "10.0/w").getTipo());
    }

    @Test
    void mapeaCsl_autorLiteral() {
        JsonNode m = json("""
                { "type": "report", "title": "Informe OMS",
                  "author": [{"literal": "Organizacion Mundial de la Salud"}] }
                """);
        assertEquals("Organizacion Mundial de la Salud", CrossRefClient.mapearCsl(m, "10.0/oms").getAutores());
    }

    // ── mapeo OpenAlex (búsqueda por título, cobertura amplia) ──
    @Test
    void mapeaOpenAlex_articuloConNombresCompletos() {
        JsonNode w = json("""
                {
                  "title": "A Novel Approach for Invoice Management using Blockchain",
                  "publication_year": 2023,
                  "doi": "https://doi.org/10.1/x",
                  "type": "article",
                  "primary_location": { "source": { "display_name": "IJRASET" } },
                  "biblio": { "volume": "11", "issue": "4", "first_page": "100", "last_page": "108" },
                  "authorships": [
                    { "author": { "display_name": "Nikhil Sontakke" } },
                    { "author": { "display_name": "Shivansh Rastogi" } }
                  ]
                }
                """);

        ReferenciaRequest r = CrossRefClient.mapearOpenAlex(w);

        assertEquals("ARTICULO", r.getTipo());
        assertEquals("A Novel Approach for Invoice Management using Blockchain", r.getTitulo());
        assertEquals("2023", r.getAnio());
        assertEquals("10.1/x", r.getDoi());               // sin el prefijo doi.org
        assertEquals("IJRASET", r.getFuente());
        assertEquals("11", r.getVolumen());
        assertEquals("100-108", r.getPaginas());
        // nombres completos -> "Apellido, Iniciales"
        assertEquals("Sontakke, N; Rastogi, S", r.getAutores());
    }

    @Test
    void mapeaOpenAlex_tipoDissertationEsTesis() {
        assertEquals("TESIS", CrossRefClient.mapearOpenAlex(json("{\"title\":\"T\",\"type\":\"dissertation\"}")).getTipo());
    }

    // ── normalizar() ──
    @Test
    void normalizar_quitaPrefijoUrl() {
        assertEquals("10.1038/nature12373", CrossRefClient.normalizar("https://doi.org/10.1038/nature12373"));
    }

    @Test
    void normalizar_quitaPrefijoDoi() {
        assertEquals("10.1234/abc", CrossRefClient.normalizar("doi: 10.1234/abc"));
    }

    @Test
    void normalizar_recortaEspacios() {
        assertEquals("10.1/x", CrossRefClient.normalizar("  10.1/x  "));
    }

    @Test
    void normalizar_conNull_devuelveVacio() {
        assertEquals("", CrossRefClient.normalizar(null));
    }
}
