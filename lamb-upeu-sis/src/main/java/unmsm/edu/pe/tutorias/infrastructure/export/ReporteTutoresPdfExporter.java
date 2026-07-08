package unmsm.edu.pe.tutorias.infrastructure.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tutorias.application.dto.FilaExportReporte;
import unmsm.edu.pe.tutorias.application.dto.ReporteResumen;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Exporta el reporte de tutores a PDF (OpenPDF), agrupando por tutor. */
@ApplicationScoped
public class ReporteTutoresPdfExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color HEADER_BG = new Color(80, 90, 110);

    public byte[] export(ReporteResumen resumen, List<FilaExportReporte> filas) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Font tutorFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font cell = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font cellHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

            doc.add(new Paragraph("Reporte de tutores", title));
            doc.add(new Paragraph(String.format(
                    "Tutores: %d · Estudiantes con tutor: %d · Sin tutor: %d · Promedio por tutor: %d",
                    resumen.getTutores(), resumen.getEstudiantesConTutor(),
                    resumen.getEstudiantesSinTutor(), resumen.getPromedioPorTutor()), small));
            doc.add(Chunk.NEWLINE);

            LinkedHashMap<UUID, List<FilaExportReporte>> grupos = new LinkedHashMap<>();
            for (FilaExportReporte f : filas) {
                grupos.computeIfAbsent(f.tutorId(), k -> new ArrayList<>()).add(f);
            }

            for (Map.Entry<UUID, List<FilaExportReporte>> e : grupos.entrySet()) {
                List<FilaExportReporte> g = e.getValue();
                FilaExportReporte t = g.get(0);
                doc.add(new Paragraph(t.tutor() + (t.grado() != null ? " · " + t.grado() : "")
                        + "  (" + g.size() + " estudiante(s), cupo " + t.cupo() + ")", tutorFont));

                PdfPTable table = new PdfPTable(new float[]{3.2f, 1.6f, 1.6f, 3f, 1.4f});
                table.setWidthPercentage(100);
                header(table, cellHead, "Estudiante", "Código", "Matrícula", "Programa", "Inicio");
                for (FilaExportReporte f : g) {
                    table.addCell(celda(f.estudiante(), cell));
                    table.addCell(celda(f.codigo(), cell));
                    table.addCell(celda(f.matricula(), cell));
                    table.addCell(celda(f.programa(), cell));
                    table.addCell(celda(f.inicio() != null ? FMT.format(f.inicio()) : "", cell));
                }
                doc.add(table);
                doc.add(Chunk.NEWLINE);
            }
            if (grupos.isEmpty()) {
                doc.add(new Paragraph("Sin datos para el filtro seleccionado.", small));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando el PDF del reporte", ex);
        }
    }

    private void header(PdfPTable t, Font f, String... hs) {
        for (String h : hs) {
            PdfPCell c = new PdfPCell(new Phrase(h, f));
            c.setBackgroundColor(HEADER_BG);
            c.setPadding(4);
            t.addCell(c);
        }
    }

    private PdfPCell celda(String v, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "", f));
        c.setPadding(3);
        return c;
    }
}
