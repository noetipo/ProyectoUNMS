package unmsm.edu.pe.tesis.infrastructure.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import unmsm.edu.pe.tesis.application.dto.ActividadItem;
import unmsm.edu.pe.tesis.application.dto.ObjetivoItem;
import unmsm.edu.pe.tesis.application.dto.PartidaItem;
import unmsm.edu.pe.tesis.application.dto.ProyectoEditorResponse;
import unmsm.edu.pe.tesis.application.util.ProyectoDefinicion;
import unmsm.edu.pe.tesis.domain.enums.EnfoqueInvestigacion;
import unmsm.edu.pe.tesis.domain.enums.EstiloCita;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el documento del proyecto de tesis (PDF con OpenPDF y Word con Apache POI),
 * con carátula, secciones I–VI y las referencias ya formateadas en el estilo elegido.
 */
@ApplicationScoped
public class ProyectoDocumentoExporter {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final boolean NUM_REFS = true; // Vancouver/IEEE numeran; APA no (se ajusta abajo)

    // ══════════════════════════════════ PDF ══════════════════════════════════
    public byte[] pdf(ProyectoEditorResponse e, List<String> referencias, EstiloCita estilo) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 60, 60, 54, 54);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Carátula
            doc.add(centro("UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS", pf(11, true, null)));
            doc.add(centro("Escuela de Posgrado", pf(10, false, Color.GRAY)));
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);
            doc.add(centro("PROYECTO DE TESIS", pf(16, true, null)));
            doc.add(Chunk.NEWLINE);
            doc.add(centro(nz(e.getTitulo(), "(sin título)"), pf(13, true, new Color(0x8C, 0x1D, 0x2E))));
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);
            pdfMeta(doc, "Autor", e.getEstudianteNombre());
            pdfMeta(doc, "Código", e.getCodigoSistema());
            pdfMeta(doc, "Programa", e.getProgramaNombre());
            pdfMeta(doc, "Asesor", e.getAsesorNombre());
            pdfMeta(doc, "Línea de investigación", e.getLineaNombre());
            pdfMeta(doc, "Enfoque", "CUALITATIVO".equals(e.getEnfoque()) ? "Cualitativo" : "Cuantitativo / mixto");
            pdfMeta(doc, "Fecha", DIA.format(LocalDate.now()));
            doc.newPage();

            // Resumen y palabras clave
            if (has(campo(e, "resumen"))) {
                doc.add(h2Pdf("Resumen"));
                doc.add(cuerpoPdf(campo(e, "resumen")));
            }
            if (has(campo(e, "palabras"))) {
                Paragraph p = new Paragraph();
                p.add(new Chunk("Palabras clave: ", pf(11, true, null)));
                p.add(new Chunk(campo(e, "palabras"), pf(11, false, null)));
                p.setSpacingAfter(10);
                doc.add(p);
            }

            EnfoqueInvestigacion enf = enfoque(e);
            for (ProyectoDefinicion.Seccion sec : ProyectoDefinicion.seccionesActivas(enf)) {
                if ("g".equals(sec.id())) continue; // los datos generales van en carátula/resumen
                doc.add(h2Pdf(sec.titulo()));
                for (String k : sec.campos()) {
                    if ("referencias".equals(k)) continue; // se listan formateadas al final
                    String v = campo(e, k);
                    if (!has(v)) continue;
                    doc.add(h3Pdf(ProyectoDefinicion.etiqueta(k)));
                    doc.add(cuerpoPdf(v));
                }
                if ("p1".equals(sec.id()) && e.getObjetivos() != null && !e.getObjetivos().isEmpty()) {
                    doc.add(h3Pdf("Objetivos específicos"));
                    int i = 1;
                    for (ObjetivoItem o : e.getObjetivos()) {
                        if (has(o.getTexto())) doc.add(cuerpoPdf("OE" + (i++) + ". " + o.getTexto()));
                    }
                }
                if ("p4".equals(sec.id())) pdfAspectosAdministrativos(doc, e); // V. va tras metodología
            }

            // Referencias (formateadas)
            doc.add(h2Pdf("Referencias bibliográficas (" + estilo + ")"));
            if (referencias == null || referencias.isEmpty()) {
                doc.add(cuerpoPdf("— aún no se han registrado referencias."));
            } else {
                int n = 1;
                for (String ref : referencias) {
                    boolean num = estilo.esNumerico();
                    Paragraph p = pdfConItalica(num ? "[" + (n++) + "] " + ref : ref);
                    p.setSpacingAfter(6);
                    p.setFirstLineIndent(num ? 0 : -18);
                    p.setIndentationLeft(num ? 0 : 18);
                    doc.add(p);
                }
            }
            if (has(campo(e, "anexos"))) {
                doc.add(h3Pdf("Anexos"));
                doc.add(cuerpoPdf(campo(e, "anexos")));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando el PDF del proyecto", ex);
        }
    }

    private void pdfAspectosAdministrativos(Document doc, ProyectoEditorResponse e) {
        doc.add(h2Pdf("V. Aspectos administrativos"));
        doc.add(h3Pdf("Cronograma de actividades"));
        if (e.getActividades() != null && !e.getActividades().isEmpty()) {
            for (ActividadItem a : e.getActividades()) {
                String rango = rango(a);
                doc.add(cuerpoPdf("• " + nz(a.getNombre(), "") + (rango.isBlank() ? "" : "  (" + rango + ")")
                        + " — " + estadoLegible(a.getEstado())));
            }
        } else {
            doc.add(cuerpoPdf("— sin actividades."));
        }
        doc.add(h3Pdf("Presupuesto por partidas"));
        if (e.getPartidas() != null && !e.getPartidas().isEmpty()) {
            for (PartidaItem pa : e.getPartidas()) {
                doc.add(cuerpoPdf("• " + nz(pa.getRubro(), "") + ": " + nz(pa.getDescripcion(), "")
                        + " — S/ " + (pa.getMonto() != null ? pa.getMonto().toPlainString() : "0.00")));
            }
            doc.add(cuerpoPdf("Total: S/ " + (e.getPresupuestoTotal() != null ? e.getPresupuestoTotal().toPlainString() : "0.00")
                    + "  ·  Financiamiento: " + nz(e.getFinanciamiento(), "—")));
        } else {
            doc.add(cuerpoPdf("— sin partidas."));
        }
    }

    // ── helpers PDF ──
    private Font pf(int size, boolean bold, Color c) {
        Font f = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, size);
        if (c != null) f.setColor(c);
        return f;
    }

    private Paragraph centro(String t, Font f) {
        Paragraph p = new Paragraph(t, f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private void pdfMeta(Document doc, String k, String v) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(k + ": ", pf(11, true, null)));
        p.add(new Chunk(nz(v, "—"), pf(11, false, null)));
        p.setSpacingAfter(3);
        doc.add(p);
    }

    private Paragraph h2Pdf(String t) {
        Paragraph p = new Paragraph(t, pf(13, true, new Color(0x8C, 0x1D, 0x2E)));
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        return p;
    }

    private Paragraph h3Pdf(String t) {
        Paragraph p = new Paragraph(t, pf(11, true, null));
        p.setSpacingBefore(6);
        p.setSpacingAfter(2);
        return p;
    }

    private Paragraph cuerpoPdf(String t) {
        Paragraph p = new Paragraph(t, pf(11, false, null));
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingAfter(4);
        return p;
    }

    /** Convierte «i»…«/i» en tramos en cursiva. */
    private Paragraph pdfConItalica(String t) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        for (String[] tramo : tramos(t)) {
            Font f = FontFactory.getFont("1".equals(tramo[1]) ? FontFactory.HELVETICA_OBLIQUE : FontFactory.HELVETICA, 11);
            p.add(new Chunk(tramo[0], f));
        }
        return p;
    }

    // ══════════════════════════════════ WORD (.docx) ══════════════════════════════════
    public byte[] docx(ProyectoEditorResponse e, List<String> referencias, EstiloCita estilo) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            centroW(doc, "UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS", 12, true);
            centroW(doc, "Escuela de Posgrado", 10, false);
            centroW(doc, "", 10, false);
            centroW(doc, "PROYECTO DE TESIS", 18, true);
            centroW(doc, nz(e.getTitulo(), "(sin título)"), 14, true);
            centroW(doc, "", 10, false);
            metaW(doc, "Autor", e.getEstudianteNombre());
            metaW(doc, "Código", e.getCodigoSistema());
            metaW(doc, "Programa", e.getProgramaNombre());
            metaW(doc, "Asesor", e.getAsesorNombre());
            metaW(doc, "Línea de investigación", e.getLineaNombre());
            metaW(doc, "Enfoque", "CUALITATIVO".equals(e.getEnfoque()) ? "Cualitativo" : "Cuantitativo / mixto");
            metaW(doc, "Fecha", DIA.format(LocalDate.now()));
            saltoPagina(doc);

            if (has(campo(e, "resumen"))) { h2W(doc, "Resumen"); cuerpoW(doc, campo(e, "resumen")); }
            if (has(campo(e, "palabras"))) {
                XWPFParagraph p = doc.createParagraph();
                run(p, "Palabras clave: ", 11, true, false);
                run(p, campo(e, "palabras"), 11, false, false);
            }

            EnfoqueInvestigacion enf = enfoque(e);
            for (ProyectoDefinicion.Seccion sec : ProyectoDefinicion.seccionesActivas(enf)) {
                if ("g".equals(sec.id())) continue;
                h2W(doc, sec.titulo());
                for (String k : sec.campos()) {
                    if ("referencias".equals(k)) continue;
                    String v = campo(e, k);
                    if (!has(v)) continue;
                    h3W(doc, ProyectoDefinicion.etiqueta(k));
                    cuerpoW(doc, v);
                }
                if ("p1".equals(sec.id()) && e.getObjetivos() != null && !e.getObjetivos().isEmpty()) {
                    h3W(doc, "Objetivos específicos");
                    int i = 1;
                    for (ObjetivoItem o : e.getObjetivos()) {
                        if (has(o.getTexto())) cuerpoW(doc, "OE" + (i++) + ". " + o.getTexto());
                    }
                }
                if ("p4".equals(sec.id())) docxAspectos(doc, e);
            }

            h2W(doc, "Referencias bibliográficas (" + estilo + ")");
            if (referencias == null || referencias.isEmpty()) {
                cuerpoW(doc, "— aún no se han registrado referencias.");
            } else {
                int n = 1;
                for (String ref : referencias) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setAlignment(ParagraphAlignment.BOTH);
                    if (estilo.esNumerico()) run(p, "[" + (n++) + "] ", 11, false, false);
                    else { p.setIndentationHanging(360); }
                    for (String[] tr : tramos(ref)) run(p, tr[0], 11, false, "1".equals(tr[1]));
                }
            }
            if (has(campo(e, "anexos"))) { h3W(doc, "Anexos"); cuerpoW(doc, campo(e, "anexos")); }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error generando el Word del proyecto", ex);
        }
    }

    private void docxAspectos(XWPFDocument doc, ProyectoEditorResponse e) {
        h2W(doc, "V. Aspectos administrativos");
        h3W(doc, "Cronograma de actividades");
        if (e.getActividades() != null && !e.getActividades().isEmpty()) {
            for (ActividadItem a : e.getActividades()) {
                String rango = rango(a);
                cuerpoW(doc, "• " + nz(a.getNombre(), "") + (rango.isBlank() ? "" : "  (" + rango + ")")
                        + " — " + estadoLegible(a.getEstado()));
            }
        } else cuerpoW(doc, "— sin actividades.");
        h3W(doc, "Presupuesto por partidas");
        if (e.getPartidas() != null && !e.getPartidas().isEmpty()) {
            for (PartidaItem pa : e.getPartidas()) {
                cuerpoW(doc, "• " + nz(pa.getRubro(), "") + ": " + nz(pa.getDescripcion(), "")
                        + " — S/ " + (pa.getMonto() != null ? pa.getMonto().toPlainString() : "0.00"));
            }
            cuerpoW(doc, "Total: S/ " + (e.getPresupuestoTotal() != null ? e.getPresupuestoTotal().toPlainString() : "0.00")
                    + "  ·  Financiamiento: " + nz(e.getFinanciamiento(), "—"));
        } else cuerpoW(doc, "— sin partidas.");
    }

    // ── helpers Word ──
    private void run(XWPFParagraph p, String text, int size, boolean bold, boolean italic) {
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily("Calibri");
        r.setFontSize(size);
        r.setBold(bold);
        r.setItalic(italic);
    }

    private void centroW(XWPFDocument doc, String t, int size, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        run(p, t, size, bold, false);
    }

    private void metaW(XWPFDocument doc, String k, String v) {
        XWPFParagraph p = doc.createParagraph();
        run(p, k + ": ", 11, true, false);
        run(p, nz(v, "—"), 11, false, false);
    }

    private void h2W(XWPFDocument doc, String t) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(160);
        XWPFRun r = p.createRun();
        r.setText(t); r.setBold(true); r.setFontSize(13); r.setFontFamily("Calibri");
        r.setColor("8C1D2E");
    }

    private void h3W(XWPFDocument doc, String t) {
        XWPFParagraph p = doc.createParagraph();
        run(p, t, 11, true, false);
    }

    private void cuerpoW(XWPFDocument doc, String t) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        run(p, t, 11, false, false);
    }

    private void saltoPagina(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().addBreak(BreakType.PAGE);
    }

    // ══════════════════════════════════ util común ══════════════════════════════════
    /** Divide un texto con marcas «i»…«/i» en tramos [texto, "1"|"0"] (1 = cursiva). */
    private List<String[]> tramos(String t) {
        List<String[]> out = new java.util.ArrayList<>();
        if (t == null) return out;
        int i = 0;
        while (i < t.length()) {
            int ini = t.indexOf("«i»", i);
            if (ini < 0) { out.add(new String[]{t.substring(i), "0"}); break; }
            if (ini > i) out.add(new String[]{t.substring(i, ini), "0"});
            int fin = t.indexOf("«/i»", ini);
            if (fin < 0) { out.add(new String[]{t.substring(ini + 3), "1"}); break; }
            out.add(new String[]{t.substring(ini + 3, fin), "1"});
            i = fin + 4;
        }
        return out;
    }

    private EnfoqueInvestigacion enfoque(ProyectoEditorResponse e) {
        try {
            return EnfoqueInvestigacion.valueOf(e.getEnfoque());
        } catch (Exception ex) {
            return EnfoqueInvestigacion.CUANTITATIVO;
        }
    }

    private String campo(ProyectoEditorResponse e, String k) {
        return e.getCampos() != null ? e.getCampos().get(k) : null;
    }

    private String rango(ActividadItem a) {
        if (a.getFechaInicio() == null) return "";
        String ini = a.getFechaInicio().format(DIA);
        String fin = a.getFechaFin() != null ? a.getFechaFin().format(DIA) : ini;
        return ini + " – " + fin;
    }

    private String estadoLegible(String s) {
        if (s == null) return "Pendiente";
        return switch (s) {
            case "HECHA" -> "Hecha";
            case "EN_CURSO" -> "En curso";
            default -> "Pendiente";
        };
    }

    private boolean has(String s) { return s != null && !s.isBlank(); }

    private String nz(String s, String def) { return has(s) ? s.trim() : def; }
}
