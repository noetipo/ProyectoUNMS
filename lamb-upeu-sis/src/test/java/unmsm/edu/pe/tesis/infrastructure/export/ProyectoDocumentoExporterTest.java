package unmsm.edu.pe.tesis.infrastructure.export;

import org.junit.jupiter.api.Test;
import unmsm.edu.pe.tesis.application.dto.ActividadItem;
import unmsm.edu.pe.tesis.application.dto.ObjetivoItem;
import unmsm.edu.pe.tesis.application.dto.PartidaItem;
import unmsm.edu.pe.tesis.application.dto.ProyectoEditorResponse;
import unmsm.edu.pe.tesis.domain.enums.EstiloCita;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica que el exportador genere archivos PDF y Word válidos (no vacíos, con su firma). */
class ProyectoDocumentoExporterTest {

    private final ProyectoDocumentoExporter exporter = new ProyectoDocumentoExporter();

    private ProyectoEditorResponse editorDemo() {
        return ProyectoEditorResponse.builder()
                .estudianteNombre("Ana Quispe").codigoSistema("EST-001").programaNombre("Doctorado en Salud")
                .asesorNombre("Dra. Silvia Caceres").lineaNombre("Salud publica").nivel("DOCTORADO")
                .titulo("Anemia infantil en zonas rurales")
                .enfoque("CUANTITATIVO")
                .financiamiento("Autofinanciado")
                .campos(Map.of(
                        "titulo", "Anemia infantil en zonas rurales",
                        "resumen", "Estudio observacional de corte transversal.",
                        "palabras", "anemia; altitud; prevalencia",
                        "situacion", "La anemia afecta al 43% de menores.",
                        "hipotesis", "La prevalencia es mayor en zonas rurales."))
                .objetivos(List.of(ObjetivoItem.builder().orden(0).texto("Estimar la prevalencia").build()))
                .actividades(List.of(ActividadItem.builder().nombre("Recoleccion de datos")
                        .fase("TRABAJO_CAMPO").estado("EN_CURSO")
                        .fechaInicio(LocalDate.of(2026, 3, 1)).fechaFin(LocalDate.of(2026, 8, 31)).build()))
                .partidas(List.of(PartidaItem.builder().rubro("Insumos").descripcion("Tiras reactivas")
                        .monto(new BigDecimal("1800.00")).build()))
                .presupuestoTotal(new BigDecimal("1800.00"))
                .build();
    }

    private final List<String> refs = List.of(
            "Garcia JM, Perez M. Anemia infantil. Rev Peru Salud. 2023;40(2):123-130.",
            "Hernandez R. Metodologia de la investigacion. Mexico: McGraw-Hill; 2014.");

    @Test
    void pdf_esUnPdfValidoNoVacio() {
        byte[] pdf = exporter.pdf(editorDemo(), refs, EstiloCita.VANCOUVER);
        assertNotNull(pdf);
        assertTrue(pdf.length > 300, "El PDF debe tener contenido");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.ISO_8859_1), "Debe empezar con la firma %PDF");
    }

    @Test
    void docx_esUnZipOoxmlNoVacio() {
        byte[] docx = exporter.docx(editorDemo(), refs, EstiloCita.APA);
        assertNotNull(docx);
        assertTrue(docx.length > 300, "El Word debe tener contenido");
        // Los .docx son ZIP: empiezan con la firma "PK" (0x50 0x4B).
        assertEquals('P', (char) docx[0]);
        assertEquals('K', (char) docx[1]);
    }

    @Test
    void generaEnLosTresEstilos_sinLanzar() {
        for (EstiloCita e : EstiloCita.values()) {
            assertDoesNotThrow(() -> exporter.pdf(editorDemo(), refs, e));
            assertDoesNotThrow(() -> exporter.docx(editorDemo(), refs, e));
        }
    }

    @Test
    void sinReferencias_noRompe() {
        assertDoesNotThrow(() -> exporter.pdf(editorDemo(), List.of(), EstiloCita.VANCOUVER));
        assertDoesNotThrow(() -> exporter.docx(editorDemo(), null, EstiloCita.IEEE));
    }
}
