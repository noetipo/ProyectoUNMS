package unmsm.edu.pe.tesis.infrastructure.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.application.dto.DatosDocumentoAsesoria;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.lowagie.text.pdf.PdfWriter;

/**
 * Genera los PDF del proceso de asesoría (al vuelo, sin persistir): la Solicitud de
 * asesoría y la Carta de aceptación del asesor. Usa OpenPDF, igual que el reporte de tutores.
 */
@ApplicationScoped
public class AsesoriaPdfExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Font title() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15); }
    private Font h2() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11); }
    private Font body() { return FontFactory.getFont(FontFactory.HELVETICA, 11); }
    private Font small() { return FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY); }
    private Font placeholder() { return FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.LIGHT_GRAY); }

    /** Solicitud de asesoría (la firma el estudiante). */
    public byte[] solicitud(DatosDocumentoAsesoria d) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            doc.add(centrado("SOLICITUD DE ASESORÍA DE TESIS", title()));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Fecha: " + fecha(d.getFecha()), body()));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Señor(a) docente:", body()));
            doc.add(new Paragraph(nz(d.getAsesorNombre()) + (d.getAsesorGrado() != null ? " (" + d.getAsesorGrado() + ")" : ""), h2()));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                    "Por medio de la presente, yo " + nz(d.getEstudianteNombre())
                            + (d.getEstudianteCodigo() != null ? " (código " + d.getEstudianteCodigo() + ")" : "")
                            + ", estudiante del programa " + nz(d.getProgramaNombre())
                            + ", solicito a usted asumir la asesoría de mi trabajo de tesis, cuyo detalle es:", body()));
            doc.add(Chunk.NEWLINE);

            campo(doc, "Título del tema", d.getTemaTitulo());
            campo(doc, "Línea de investigación", d.getLineaNombre());
            campo(doc, "Nivel", d.getNivel());
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Agradezco de antemano su aceptación.", body()));
            firma(doc, "Firma del estudiante");
            piePlaceholders(doc);

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando la Solicitud de asesoría en PDF", ex);
        }
    }

    /** Carta de aceptación del asesor (la firma el docente). */
    public byte[] cartaAceptacion(DatosDocumentoAsesoria d) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            doc.add(centrado("CARTA DE ACEPTACIÓN DEL ASESOR", title()));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Fecha: " + fecha(d.getFecha()), body()));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                    "Yo, " + nz(d.getAsesorNombre())
                            + (d.getAsesorGrado() != null ? " (" + d.getAsesorGrado() + ")" : "")
                            + ", declaro mi aceptación para asumir la asesoría del trabajo de tesis del/la estudiante "
                            + nz(d.getEstudianteNombre())
                            + (d.getEstudianteCodigo() != null ? " (código " + d.getEstudianteCodigo() + ")" : "")
                            + ", del programa " + nz(d.getProgramaNombre()) + ".", body()));
            doc.add(Chunk.NEWLINE);

            campo(doc, "Título del tema", d.getTemaTitulo());
            campo(doc, "Línea de investigación", d.getLineaNombre());
            campo(doc, "Nivel", d.getNivel());
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph(
                    "En señal de conformidad, firmo la presente carta de aceptación.", body()));
            firma(doc, "Firma del asesor");
            piePlaceholders(doc);

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando la Carta de aceptación en PDF", ex);
        }
    }

    // ── helpers de layout ──
    private void encabezado(Document doc) {
        doc.add(centrado("UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS", h2()));
        doc.add(centrado("Escuela de Posgrado · Facultad de Medicina", small()));
        doc.add(Chunk.NEWLINE);
    }

    private Paragraph centrado(String txt, Font f) {
        Paragraph p = new Paragraph(txt, f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private void campo(Document doc, String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta + ": ", h2()));
        p.add(new Chunk(nz(valor), body()));
        doc.add(p);
    }

    private void firma(Document doc, String etiqueta) {
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
        doc.add(centrado("_______________________________", body()));
        doc.add(centrado(etiqueta, small()));
    }

    private void piePlaceholders(Document doc) {
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("[ Firma / sello / N° de documento oficial: pendiente de configuración ]", placeholder()));
    }

    private String fecha(LocalDate f) {
        return f != null ? FMT.format(f) : FMT.format(LocalDate.now());
    }

    private String nz(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }
}
