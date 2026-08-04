package unmsm.edu.pe.tesis.infrastructure.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.tesis.application.dto.SeguimientoAlumnoItem;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Exporta el tablero de seguimiento a Excel (.xlsx) con las filas ya filtradas. */
public final class SeguimientoExcel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] CABECERAS = {
            "Doctorando", "Código", "Programa", "Título de tesis", "Etapa", "Etapa (nombre)",
            "Avance %", "Qué falta", "Responsable", "Días sin movimiento", "Último hito",
            "Asesor", "Tutor", "Fecha de defensa", "Dictamen vence el"
    };

    private SeguimientoExcel() {
    }

    public static byte[] generar(List<SeguimientoAlumnoItem> alumnos) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Seguimiento");
            escribirCabecera(wb, hoja);

            int f = 1;
            for (SeguimientoAlumnoItem a : alumnos) {
                Row r = hoja.createRow(f++);
                int c = 0;
                texto(r, c++, a.getApellidos() + ", " + a.getNombres());
                texto(r, c++, a.getCodigoSistema());
                texto(r, c++, a.getProgramaNombre());
                texto(r, c++, a.getTituloTesis());
                numero(r, c++, a.getEtapaNumero());
                texto(r, c++, a.getEtapaTitulo());
                numero(r, c++, a.getAvancePct());
                texto(r, c++, a.getPendiente());
                texto(r, c++, a.getResponsable());
                if (a.getDiasEnEtapa() != null) {
                    numero(r, c, a.getDiasEnEtapa());
                }
                c++;
                texto(r, c++, fecha(a.getFechaUltimoHito()));
                texto(r, c++, a.getAsesorNombre());
                texto(r, c++, a.getTutorNombre());
                texto(r, c++, fecha(a.getFechaDefensa()));
                texto(r, c, fecha(a.getVigenciaVenceEl()));
            }

            for (int i = 0; i < CABECERAS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el Excel de seguimiento: " + e.getMessage());
        }
    }

    private static void escribirCabecera(Workbook wb, Sheet hoja) {
        CellStyle estilo = wb.createCellStyle();
        Font negrita = wb.createFont();
        negrita.setBold(true);
        negrita.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(negrita);
        estilo.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);

        Row r = hoja.createRow(0);
        for (int i = 0; i < CABECERAS.length; i++) {
            Cell celda = r.createCell(i);
            celda.setCellValue(CABECERAS[i]);
            celda.setCellStyle(estilo);
        }
    }

    private static void texto(Row r, int col, String valor) {
        r.createCell(col).setCellValue(valor == null ? "" : valor);
    }

    private static void numero(Row r, int col, int valor) {
        r.createCell(col).setCellValue(valor);
    }

    private static String fecha(LocalDate d) {
        return d == null ? "" : d.format(FECHA);
    }
}
