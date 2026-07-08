package unmsm.edu.pe.tesis.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Renderiza una plantilla de asesoría (texto literal con marcadores) a .docx (Apache POI)
 * y a .pdf (OpenPDF) a partir del MISMO modelo de datos, de modo que Word y PDF sean
 * coherentes: mismo texto literal, misma estructura de párrafos.
 */
@ApplicationScoped
public class DocumentoAsesoriaRenderer {

    private static final String FUENTE = "Times New Roman";
    private static final float TAM = 12f;

    // ── Word (.docx) ─────────────────────────────────────────────────────────
    public byte[] docx(String plantilla, Map<String, String> datos) {
        String texto = sustituir(plantilla, datos);
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String[] lineas = texto.split("\n", -1);
            for (int i = 0; i < lineas.length; i++) {
                String linea = lineas[i];
                XWPFParagraph p = doc.createParagraph();
                p.setSpacingAfter(120);
                if (esCentrado(linea, i)) {
                    p.setAlignment(ParagraphAlignment.CENTER);
                } else {
                    p.setAlignment(ParagraphAlignment.BOTH);
                }
                XWPFRun r = p.createRun();
                r.setFontFamily(FUENTE);
                r.setFontSize((int) TAM);
                if (i == 0) r.setBold(true);
                r.setText(linea);
            }
            doc.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando el documento Word (.docx)", ex);
        }
    }

    // ── PDF ──────────────────────────────────────────────────────────────────
    public byte[] pdf(String plantilla, Map<String, String> datos) {
        String texto = sustituir(plantilla, datos);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 70, 70, 60, 60);
            PdfWriter.getInstance(document, out);
            document.open();
            Font base = FontFactory.getFont(FontFactory.TIMES_ROMAN, TAM);
            Font negrita = FontFactory.getFont(FontFactory.TIMES_ROMAN, TAM, Font.BOLD);
            String[] lineas = texto.split("\n", -1);
            for (int i = 0; i < lineas.length; i++) {
                String linea = lineas[i];
                Paragraph p = new Paragraph(linea.isEmpty() ? " " : linea, i == 0 ? negrita : base);
                p.setSpacingAfter(6f);
                if (esCentrado(linea, i)) {
                    p.setAlignment(Paragraph.ALIGN_CENTER);
                } else {
                    p.setAlignment(Paragraph.ALIGN_JUSTIFIED);
                }
                document.add(p);
            }
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando el documento PDF", ex);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private String sustituir(String plantilla, Map<String, String> datos) {
        String s = plantilla;
        for (Map.Entry<String, String> e : datos.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        // Colapsa espacios/tabs múltiples (p. ej. cuando un marcador queda vacío) a uno solo,
        // preservando saltos de línea. Las plantillas no usan dobles espacios intencionales.
        s = s.replaceAll("[ \\t]{2,}", " ");
        return s.stripTrailing();
    }

    /** Título (línea 0) y la línea del lema (entre comillas “...”) van centrados. */
    private boolean esCentrado(String linea, int indice) {
        if (indice == 0) return true;
        String t = linea.trim();
        return t.startsWith("“") && t.endsWith("”"); // “ ... ”
    }
}
