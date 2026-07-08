package unmsm.edu.pe.tutorias.infrastructure.export;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import unmsm.edu.pe.tutorias.application.dto.FilaExportReporte;
import unmsm.edu.pe.tutorias.application.dto.ReporteResumen;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Exporta el reporte de tutores a Excel (Apache POI). */
@ApplicationScoped
public class ReporteTutoresExcelExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] export(ReporteResumen resumen, List<FilaExportReporte> filas) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            CellStyle head = wb.createCellStyle();
            head.setFont(boldFont);

            // Hoja Resumen
            Sheet r = wb.createSheet("Resumen");
            titulo(r, 0, head, "Reporte de tutores");
            kv(r, 2, "Tutores", resumen.getTutores());
            kv(r, 3, "Estudiantes con tutor", resumen.getEstudiantesConTutor());
            kv(r, 4, "Estudiantes sin tutor", resumen.getEstudiantesSinTutor());
            kv(r, 5, "Promedio por tutor", resumen.getPromedioPorTutor());
            r.setColumnWidth(0, 7000);
            r.setColumnWidth(1, 4000);

            // Hoja Detalle
            Sheet d = wb.createSheet("Detalle");
            String[] cols = {"Tutor", "Grado", "Cupo", "Estudiante", "Código", "Matrícula", "Programa", "Inicio tutoría"};
            int[] widths = {8000, 3000, 2000, 8000, 4000, 4000, 8000, 4000};
            Row h = d.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(head);
                d.setColumnWidth(i, widths[i]);
            }
            int rn = 1;
            for (FilaExportReporte f : filas) {
                Row row = d.createRow(rn++);
                row.createCell(0).setCellValue(nz(f.tutor()));
                row.createCell(1).setCellValue(nz(f.grado()));
                row.createCell(2).setCellValue(f.cupo());
                row.createCell(3).setCellValue(nz(f.estudiante()));
                row.createCell(4).setCellValue(nz(f.codigo()));
                row.createCell(5).setCellValue(nz(f.matricula()));
                row.createCell(6).setCellValue(nz(f.programa()));
                row.createCell(7).setCellValue(f.inicio() != null ? FMT.format(f.inicio()) : "");
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando el Excel del reporte", e);
        }
    }

    private void titulo(Sheet s, int rowIdx, CellStyle style, String v) {
        Cell c = s.createRow(rowIdx).createCell(0);
        c.setCellValue(v);
        c.setCellStyle(style);
    }

    private void kv(Sheet s, int rowIdx, String k, long v) {
        Row row = s.createRow(rowIdx);
        row.createCell(0).setCellValue(k);
        row.createCell(1).setCellValue(v);
    }

    private String nz(String s) {
        return s != null ? s : "";
    }
}
