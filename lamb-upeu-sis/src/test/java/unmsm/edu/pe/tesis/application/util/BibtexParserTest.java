package unmsm.edu.pe.tesis.application.util;

import org.junit.jupiter.api.Test;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ReferenciaRequest;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica el parseo de BibTeX a referencia (tesis, artículos), incluidos acentos LaTeX. */
class BibtexParserTest {

    @Test
    void tesis_conAcentosLatex() {
        String bib = """
                @mastersthesis{penttila2019,
                  title={Identifying barriers in e-invoicing process},
                  author={Penttil{\\"a}, Jenni and Garc{\\'i}a, Jos{\\'e} M.},
                  year={2019}, school={Aalto University}, address={Espoo}
                }
                """;
        ReferenciaRequest r = BibtexParser.parse(bib);

        assertEquals("TESIS", r.getTipo());
        assertEquals("Identifying barriers in e-invoicing process", r.getTitulo());
        assertEquals("2019", r.getAnio());
        assertEquals("Aalto University", r.getEditorial());
        assertEquals("Espoo", r.getCiudad());
        assertEquals("Penttilä, J; García, JM", r.getAutores());   // acentos + "Apellido, Iniciales"
    }

    @Test
    void articulo_conRevistaVolumenPaginas() {
        String bib = """
                @article{smith2020,
                  author = {John Smith and Jane Doe},
                  title = {A study on X},
                  journal = {Journal of Y},
                  year = {2020}, volume = {40}, number = {2}, pages = {123--130},
                  doi = {10.1/x}
                }
                """;
        ReferenciaRequest r = BibtexParser.parse(bib);

        assertEquals("ARTICULO", r.getTipo());
        assertEquals("Journal of Y", r.getFuente());
        assertEquals("40", r.getVolumen());
        assertEquals("2", r.getNumero());
        assertEquals("123-130", r.getPaginas());        // "--" -> "-"
        assertEquals("10.1/x", r.getDoi());
        assertEquals("Smith, J; Doe, J", r.getAutores()); // "Nombre Apellido" -> "Apellido, Inicial"
    }

    @Test
    void phdthesisEsTesis_yAnioDesdeDate() {
        String bib = "@phdthesis{x, title={T}, author={Ana Lopez}, date={2021-05}}";
        ReferenciaRequest r = BibtexParser.parse(bib);
        assertEquals("TESIS", r.getTipo());
        assertEquals("2021", r.getAnio());
        assertEquals("Lopez, A", r.getAutores());
    }

    @Test
    void textoInvalido_lanzaValidation() {
        assertThrows(ValidationException.class, () -> BibtexParser.parse("esto no es bibtex"));
        assertThrows(ValidationException.class, () -> BibtexParser.parse(""));
    }
}
