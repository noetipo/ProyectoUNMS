package unmsm.edu.pe.tesis.application.util;

import org.junit.jupiter.api.Test;
import unmsm.edu.pe.tesis.domain.entities.ProyectoReferencia;
import unmsm.edu.pe.tesis.domain.enums.EstiloCita;
import unmsm.edu.pe.tesis.domain.enums.TipoReferencia;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica el formateo de referencias en APA 7, Vancouver e IEEE. */
class CitationFormatterTest {

    /** Quita las marcas de cursiva «i»…«/i» que interpreta el exportador. */
    private String plano(ProyectoReferencia r, EstiloCita estilo) {
        return CitationFormatter.formatear(r, estilo).replace("«i»", "").replace("«/i»", "");
    }

    private ProyectoReferencia articulo() {
        return ProyectoReferencia.builder()
                .tipo(TipoReferencia.ARTICULO)
                .autores("Garcia, JM; Perez, M")
                .anio("2023")
                .titulo("Anemia infantil en zonas rurales del Peru")
                .fuente("Revista Peruana de Salud Publica")
                .volumen("40").numero("2").paginas("123-130")
                .doi("10.1234/rpsp.2023.402")
                .build();
    }

    private ProyectoReferencia libro() {
        return ProyectoReferencia.builder()
                .tipo(TipoReferencia.LIBRO)
                .autores("Hernandez, R; Fernandez, C")
                .anio("2014")
                .titulo("Metodologia de la investigacion")
                .editorial("McGraw-Hill").ciudad("Mexico")
                .build();
    }

    // ── Artículo de revista ──
    @Test
    void articulo_vancouver() {
        assertEquals(
                "Garcia JM, Perez M. Anemia infantil en zonas rurales del Peru. Revista Peruana de Salud Publica. 2023;40(2):123-130. doi:10.1234/rpsp.2023.402",
                plano(articulo(), EstiloCita.VANCOUVER));
    }

    @Test
    void articulo_apa() {
        assertEquals(
                "Garcia, J. M., & Perez, M. (2023). Anemia infantil en zonas rurales del Peru. Revista Peruana de Salud Publica, 40(2), 123-130. https://doi.org/10.1234/rpsp.2023.402",
                plano(articulo(), EstiloCita.APA));
    }

    @Test
    void articulo_ieee() {
        assertEquals(
                "J. M. Garcia and M. Perez, \"Anemia infantil en zonas rurales del Peru,\" Revista Peruana de Salud Publica, vol. 40, no. 2, pp. 123-130, 2023.",
                plano(articulo(), EstiloCita.IEEE));
    }

    // ── Libro ──
    @Test
    void libro_vancouver() {
        assertEquals(
                "Hernandez R, Fernandez C. Metodologia de la investigacion. Mexico: McGraw-Hill; 2014.",
                plano(libro(), EstiloCita.VANCOUVER));
    }

    @Test
    void libro_apa() {
        assertEquals(
                "Hernandez, R., & Fernandez, C. (2014). Metodologia de la investigacion. McGraw-Hill.",
                plano(libro(), EstiloCita.APA));
    }

    // ── Autores: iniciales dobles, sin coma, y regla "et al" de Vancouver ──
    @Test
    void apa_inicialesDobles_llevanPuntoYEspacio() {
        ProyectoReferencia r = articulo();
        r.setAutores("Garcia, JM");
        assertTrue(plano(r, EstiloCita.APA).startsWith("Garcia, J. M. (2023)."),
                "Las iniciales 'JM' deben formatearse como 'J. M.'");
    }

    @Test
    void autor_sinComa_seUsaComoApellido() {
        ProyectoReferencia r = articulo();
        r.setAutores("OMS");
        assertTrue(plano(r, EstiloCita.VANCOUVER).startsWith("OMS."),
                "Un autor sin coma (institucional) se usa tal cual");
    }

    @Test
    void vancouver_masDeSeisAutores_agregaEtAl() {
        ProyectoReferencia r = articulo();
        r.setAutores("A, A; B, B; C, C; D, D; E, E; F, F; G, G");
        String out = plano(r, EstiloCita.VANCOUVER);
        assertTrue(out.contains("et al"), "Con más de 6 autores debe aparecer 'et al'");
        assertFalse(out.contains("G G"), "El 7.º autor no debe listarse (queda cubierto por 'et al')");
    }

    // ── Cita en el texto ──
    @Test
    void citaEnTexto_vancouverEIeee_usanNumero() {
        assertEquals("[3]", CitationFormatter.citaEnTexto(articulo(), EstiloCita.VANCOUVER, 3));
        assertEquals("[7]", CitationFormatter.citaEnTexto(articulo(), EstiloCita.IEEE, 7));
    }

    @Test
    void citaEnTexto_apaDosAutores() {
        assertEquals("(Garcia & Perez, 2023)", CitationFormatter.citaEnTexto(articulo(), EstiloCita.APA, 1));
    }

    @Test
    void citaEnTexto_apaUnAutor() {
        ProyectoReferencia r = articulo();
        r.setAutores("Garcia, JM");
        assertEquals("(Garcia, 2023)", CitationFormatter.citaEnTexto(r, EstiloCita.APA, 1));
    }

    @Test
    void citaEnTexto_apaTresOMasAutores_usanEtAl() {
        ProyectoReferencia r = articulo();
        r.setAutores("Garcia, J; Perez, M; Lopez, R");
        assertEquals("(Garcia et al., 2023)", CitationFormatter.citaEnTexto(r, EstiloCita.APA, 1));
    }

    // ── Estilos adicionales: Harvard / MLA / Chicago ──
    @Test
    void harvard_articuloYCita() {
        String bib = plano(articulo(), EstiloCita.HARVARD);
        assertTrue(bib.contains("Garcia, J. M. and Perez, M. (2023)"), bib);
        assertTrue(bib.contains("'Anemia infantil en zonas rurales del Peru'"), bib);
        assertTrue(bib.contains("pp. 123-130"), bib);
        assertEquals("(Garcia and Perez, 2023)", CitationFormatter.citaEnTexto(articulo(), EstiloCita.HARVARD, 1));
    }

    @Test
    void mla_articuloYCita() {
        String bib = plano(articulo(), EstiloCita.MLA);
        assertTrue(bib.contains("Garcia, J. M., and M. Perez."), bib);
        assertTrue(bib.contains("\"Anemia infantil en zonas rurales del Peru.\""), bib);
        assertTrue(bib.contains("vol. 40, no. 2, 2023, pp. 123-130"), bib);
        assertEquals("(Garcia and Perez)", CitationFormatter.citaEnTexto(articulo(), EstiloCita.MLA, 1));
    }

    @Test
    void chicago_articuloYCita() {
        String bib = plano(articulo(), EstiloCita.CHICAGO);
        assertTrue(bib.contains("Garcia, J. M., and M. Perez. 2023."), bib);
        assertTrue(bib.contains("40 (2): 123-130"), bib);
        assertEquals("(Garcia and Perez 2023)", CitationFormatter.citaEnTexto(articulo(), EstiloCita.CHICAGO, 1));
    }

    @Test
    void soloVancouverEIeeeSonNumericos() {
        assertTrue(EstiloCita.VANCOUVER.esNumerico());
        assertTrue(EstiloCita.IEEE.esNumerico());
        assertFalse(EstiloCita.APA.esNumerico());
        assertFalse(EstiloCita.HARVARD.esNumerico());
        assertFalse(EstiloCita.MLA.esNumerico());
        assertFalse(EstiloCita.CHICAGO.esNumerico());
        // los autor-año NO usan [n]
        assertFalse(CitationFormatter.citaEnTexto(articulo(), EstiloCita.HARVARD, 5).startsWith("["));
    }

    // ── Cita narrativa (autor en la oración) ──
    @Test
    void citaNarrativa_apaConDosAutores() {
        assertEquals("Garcia and Perez (2023)", CitationFormatter.citaNarrativa(articulo(), EstiloCita.APA, 1));
    }

    @Test
    void citaNarrativa_unAutor() {
        ProyectoReferencia r = articulo();
        r.setAutores("Garcia, JM");
        assertEquals("Garcia (2023)", CitationFormatter.citaNarrativa(r, EstiloCita.APA, 1));
    }

    @Test
    void citaNarrativa_tresOMas_usanEtAl() {
        ProyectoReferencia r = articulo();
        r.setAutores("Garcia, J; Perez, M; Lopez, R");
        assertEquals("Garcia et al. (2023)", CitationFormatter.citaNarrativa(r, EstiloCita.APA, 1));
    }

    @Test
    void citaNarrativa_numerico_usaAutorMasNumero() {
        assertEquals("Garcia and Perez [3]", CitationFormatter.citaNarrativa(articulo(), EstiloCita.VANCOUVER, 3));
    }

    @Test
    void citaNarrativa_mla_soloAutor() {
        assertEquals("Garcia and Perez", CitationFormatter.citaNarrativa(articulo(), EstiloCita.MLA, 1));
    }

    // ── Robustez ──
    @Test
    void referenciaNula_devuelveVacio() {
        assertEquals("", CitationFormatter.formatear(null, EstiloCita.APA));
        assertEquals("", CitationFormatter.citaEnTexto(null, EstiloCita.VANCOUVER, 1));
    }

    @Test
    void estiloNulo_noLanza() {
        assertDoesNotThrow(() -> CitationFormatter.formatear(articulo(), null));
    }
}
