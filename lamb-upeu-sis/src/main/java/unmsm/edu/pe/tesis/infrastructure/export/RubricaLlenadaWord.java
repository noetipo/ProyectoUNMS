package unmsm.edu.pe.tesis.infrastructure.export;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.tesis.application.util.RubricaDefinicion;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/** Genera un Word (.docx) de la rúbrica oficial ya llenada con los niveles y puntajes del revisor. */
public final class RubricaLlenadaWord {

    private RubricaLlenadaWord() {
    }

    private static final String GUINDA = "8C1D2E";
    private static final String GRIS = "E7E7EA";

    /**
     * @param nivelPorCriterio criterio → nivel (CUMPLE/PARCIAL/NO_CUMPLE); vacío = rúbrica en blanco
     * @param obsPorCriterio   criterio → observación / sugerencia de subsanación (opcional)
     */
    public static byte[] generar(RubricaDefinicion.Rubrica rubrica, Map<String, String> nivelPorCriterio,
                                 Map<String, String> obsPorCriterio, int total, boolean aprobado,
                                 String revisor, String comentario, String estudiante, String titulo) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean evaluado = nivelPorCriterio != null && !nivelPorCriterio.isEmpty();
            Map<String, String> obs = obsPorCriterio != null ? obsPorCriterio : Map.of();

            par(doc, rubrica.titulo(), true, 14, null);
            par(doc, "Universidad Nacional Mayor de San Marcos · Facultad de Medicina · Unidad de Posgrado", false, 9, "666666");
            par(doc, "Doctorando: " + nz(estudiante), false, 10, null);
            par(doc, "Título: " + nz(titulo), false, 10, null);
            par(doc, "Docente revisor: " + nz(revisor), false, 10, null);
            par(doc, "", false, 6, null);

            XWPFTable table = doc.createTable();
            table.setWidth("100%");
            XWPFTableRow h = table.getRow(0);
            cell(h.getCell(0), "Indicador", true, GUINDA, true);
            addCells(h, 4);
            cell(h.getCell(1), "Nivel", true, GUINDA, true);
            cell(h.getCell(2), "Puntaje", true, GUINDA, true);
            cell(h.getCell(3), "Máx.", true, GUINDA, true);
            cell(h.getCell(4), "Observaciones y sugerencias", true, GUINDA, true);

            for (RubricaDefinicion.Seccion s : rubrica.secciones()) {
                XWPFTableRow sr = table.createRow();
                for (int i = 0; i <= 4; i++) cell(sr.getCell(i), i == 0 ? s.titulo() : "", true, GRIS, false);
                int sub = 0;
                for (RubricaDefinicion.Criterio c : s.criterios()) {
                    String nStr = nivelPorCriterio != null ? nivelPorCriterio.get(c.key()) : null;
                    RubricaDefinicion.Nivel n = nStr != null ? safe(nStr) : null;
                    int pt = n != null ? c.puntaje(n) : 0;
                    sub += pt;
                    XWPFTableRow cr = table.createRow();
                    cell(cr.getCell(0), c.titulo(), false, null, false);
                    cell(cr.getCell(1), nivelLabel(n), false, null, false);
                    cell(cr.getCell(2), n != null ? String.valueOf(pt) : "—", false, null, false);
                    cell(cr.getCell(3), String.valueOf(c.puntajeMaximo()), false, null, false);
                    cell(cr.getCell(4), nz(obs.get(c.key())), false, null, false);
                }
                XWPFTableRow subR = table.createRow();
                cell(subR.getCell(0), "Subtotal", true, null, false);
                cell(subR.getCell(1), "", true, null, false);
                cell(subR.getCell(2), evaluado ? String.valueOf(sub) : "—", true, null, false);
                cell(subR.getCell(3), String.valueOf(s.subtotal()), true, null, false);
                cell(subR.getCell(4), "", true, null, false);
            }

            XWPFTableRow tr = table.createRow();
            cell(tr.getCell(0), "TOTAL", true, GUINDA, true);
            cell(tr.getCell(1), "", true, GUINDA, true);
            cell(tr.getCell(2), evaluado ? String.valueOf(total) : "—", true, GUINDA, true);
            cell(tr.getCell(3), String.valueOf(rubrica.total()), true, GUINDA, true);
            cell(tr.getCell(4), "", true, GUINDA, true);

            par(doc, "", false, 6, null);
            if (evaluado) {
                par(doc, "Calificación final: " + (aprobado ? "APROBADO" : "DESAPROBADO")
                        + " (aprueba con " + RubricaDefinicion.APROBADO_MIN + " o más puntos)", true, 12, null);
            } else {
                par(doc, "Rúbrica sin calificar (aún no se registró la evaluación en el sistema).", false, 10, "666666");
            }
            if (comentario != null && !comentario.isBlank()) {
                par(doc, "Observaciones y sugerencias de subsanación:", true, 10, null);
                par(doc, comentario.trim(), false, 10, null);
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar la rúbrica llenada");
        }
    }

    private static void addCells(XWPFTableRow row, int n) {
        for (int i = 0; i < n; i++) row.addNewTableCell();
    }

    private static void cell(XWPFTableCell cell, String text, boolean bold, String bgHex, boolean white) {
        if (bgHex != null) cell.setColor(bgHex);
        if (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "");
        r.setBold(bold);
        r.setFontSize(9);
        if (white) r.setColor("FFFFFF");
    }

    private static void par(XWPFDocument doc, String text, boolean bold, int size, String colorHex) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(40);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(size);
        if (colorHex != null) r.setColor(colorHex);
    }

    private static RubricaDefinicion.Nivel safe(String s) {
        try { return RubricaDefinicion.Nivel.valueOf(s.trim().toUpperCase()); } catch (Exception e) { return null; }
    }

    private static String nivelLabel(RubricaDefinicion.Nivel n) {
        if (n == null) return "—";
        return switch (n) { case CUMPLE -> "Cumple"; case PARCIAL -> "Cumple parcialmente"; case NO_CUMPLE -> "No cumple"; };
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
