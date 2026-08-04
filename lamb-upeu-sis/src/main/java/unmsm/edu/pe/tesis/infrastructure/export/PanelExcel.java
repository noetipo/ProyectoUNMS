package unmsm.edu.pe.tesis.infrastructure.export;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.tesis.application.dto.PanelResponse;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Reporte del panel en Excel. Lleva lo mismo que la pantalla: las cifras del programa (sin datos
 * personales) y los pendientes de quien lo descarga, para poder imprimirlo o adjuntarlo a un acta.
 */
public final class PanelExcel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PanelExcel() {
    }

    public static byte[] generar(PanelResponse panel) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle titulo = estilo(wb, true, 13);
            CellStyle cabecera = cabecera(wb);
            CellStyle normal = estilo(wb, false, 11);

            Sheet h = wb.createSheet("Panel");
            h.setColumnWidth(0, 12000);
            h.setColumnWidth(1, 5000);
            h.setColumnWidth(2, 9000);

            int f = 0;
            f = fila(h, f, titulo, "Panel de Tesis y Titulación");
            f = fila(h, f, normal, "Generado el " + LocalDate.now().format(FECHA)
                    + " · vista de " + nz(panel.getRolLabel()));
            f++;

            // ── Cifras del programa ──
            f = fila(h, f, cabecera, "El programa en números", "Valor", "");
            var ag = panel.getAgregados();
            if (ag != null) {
                f = fila(h, f, normal, "Doctorandos activos", String.valueOf(ag.getDoctorandosActivos()), "");
                f = fila(h, f, normal, "Proyectos en elaboración o defensa", String.valueOf(ag.getEnRevision()), "");
                f = fila(h, f, normal, "Sin movimiento hace más de 30 días", String.valueOf(ag.getDetenidos()), "");
                f = fila(h, f, normal, "Procesos finalizados", String.valueOf(ag.getSustentados()), "");
                f = fila(h, f, normal, "Docentes activos", String.valueOf(ag.getDocentes()), "");
                f++;

                f = fila(h, f, cabecera, "Doctorandos por etapa", "Cantidad", "");
                if (ag.getPorEtapa() != null) {
                    for (var c : ag.getPorEtapa()) {
                        f = fila(h, f, normal, (c.getOrden() != null ? c.getOrden() + ". " : "") + c.getEtiqueta(),
                                String.valueOf(c.getValor()), "");
                    }
                }
                f++;

                f = fila(h, f, cabecera, "Por línea de investigación", "Cantidad", "");
                if (ag.getPorLinea() != null) {
                    for (var c : ag.getPorLinea()) {
                        f = fila(h, f, normal, c.getEtiqueta(), String.valueOf(c.getValor()), "");
                    }
                }
                f++;
            }

            // ── Lo pendiente de quien descarga ──
            f = fila(h, f, cabecera, "Mis pendientes", "Prioridad", "Detalle");
            if (panel.getPendientes() != null && !panel.getPendientes().isEmpty()) {
                for (var p : panel.getPendientes()) {
                    f = fila(h, f, normal, p.getTitulo(), nz(p.getPrioridad()), nz(p.getDetalle()));
                }
            } else {
                fila(h, f, normal, "Sin pendientes", "", "");
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private static int fila(Sheet h, int f, CellStyle st, String... valores) {
        Row r = h.createRow(f);
        for (int i = 0; i < valores.length; i++) {
            var c = r.createCell(i);
            c.setCellValue(valores[i]);
            c.setCellStyle(st);
        }
        return f + 1;
    }

    private static CellStyle estilo(Workbook wb, boolean negrita, int puntos) {
        CellStyle st = wb.createCellStyle();
        Font ft = wb.createFont();
        ft.setBold(negrita);
        ft.setFontHeightInPoints((short) puntos);
        st.setFont(ft);
        return st;
    }

    private static CellStyle cabecera(Workbook wb) {
        CellStyle st = estilo(wb, true, 11);
        st.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return st;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
