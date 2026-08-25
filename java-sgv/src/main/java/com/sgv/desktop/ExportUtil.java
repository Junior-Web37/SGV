package com.sgv.desktop;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilitário central de exportação para Excel (.xlsx) com identidade visual SGV.
 * Cabeçalho azul-escuro com letra branca, linhas alternadas cinza-claro,
 * colunas auto-dimensionadas e rodapé com data/hora de geração.
 */
public final class ExportUtil {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ExportUtil() {
    }

    /** Exporta o conteúdo actual de uma TableView genérica para .xlsx. */
    public static void exportTableToExcel(Window owner, String defaultFileName, String sheetName, TableView<?> table) {
        if (table == null) {
            SgvDialog.warning("Exportar", "Não há tabela para exportar.");
            return;
        }
        String[] headers = new String[table.getColumns().size()];
        for (int i = 0; i < headers.length; i++) {
            TableColumn<?, ?> c = table.getColumns().get(i);
            headers[i] = c.getText() != null && !c.getText().isBlank() ? c.getText() : ("Coluna " + (i + 1));
        }

        List<?> items = table.getItems();
        Object[][] rows = new Object[items.size()][];
        for (int r = 0; r < items.size(); r++) {
            Object item = items.get(r);
            Object[] row = new Object[headers.length];
            for (int cIdx = 0; cIdx < headers.length; cIdx++) {
                TableColumn<Object, ?> col = (TableColumn<Object, ?>) (TableColumn<?, ?>) table.getColumns().get(cIdx);
                try {
                    Object v = col.getCellData(item);
                    row[cIdx] = v != null ? v : "";
                } catch (Exception ex) {
                    row[cIdx] = "";
                }
            }
            rows[r] = row;
        }
        exportRows(owner, defaultFileName, sheetName, headers, rows);
    }

    /** Exporta uma matriz de linhas arbitrária para .xlsx com o estilo SGV. */
    public static void exportRows(Window owner, String defaultFileName, String sheetName, String[] headers, Object[][] rows) {
        if (rows == null || rows.length == 0) {
            SgvDialog.warning("Exportar", "Não há dados para exportar neste momento.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar ficheiro Excel");
        fc.setInitialFileName(defaultFileName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File file = fc.showSaveDialog(owner);
        if (file == null) return;

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Dados");

            // Cabeçalho SGV
            org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
            XSSFColor darkSlate = new XSSFColor(new byte[]{(byte) 30, (byte) 41, (byte) 59}, null);
            headerStyle.setFillForegroundColor(darkSlate);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);

            // Linhas alternadas
            org.apache.poi.ss.usermodel.CellStyle altStyle = wb.createCellStyle();
            XSSFColor lightGray = new XSSFColor(new byte[]{(byte) 241, (byte) 245, (byte) 249}, null);
            altStyle.setFillForegroundColor(lightGray);
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.ss.usermodel.Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(12000, Math.max(3500, headers[i].length() * 260)));
            }

            for (int r = 0; r < rows.length; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                Object[] values = rows[r];
                for (int c = 0; c < values.length && c < headers.length; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    Object v = values[c];
                    if (v instanceof Number num && !(v instanceof Boolean)) {
                        cell.setCellValue(num.doubleValue());
                    } else {
                        cell.setCellValue(v != null ? String.valueOf(v) : "");
                    }
                    if (r % 2 == 1) cell.setCellStyle(altStyle);
                }
            }

            // Rodapé informativo
            int footRow = rows.length + 2;
            org.apache.poi.ss.usermodel.Row fr = sheet.createRow(footRow);
            org.apache.poi.ss.usermodel.Cell fc1 = fr.createCell(0);
            fc1.setCellValue("Gerado pelo SGV PRO em " + LocalDateTime.now().format(TS));
            org.apache.poi.ss.usermodel.Font fFont = wb.createFont();
            fFont.setItalic(true);
            fFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            org.apache.poi.ss.usermodel.CellStyle footStyle = wb.createCellStyle();
            footStyle.setFont(fFont);
            fc1.setCellStyle(footStyle);
            sheet.createFreezePane(0, 1);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                wb.write(fos);
            }
            SgvDialog.toast("✓ " + rows.length + " registo(s) exportado(s) com sucesso");
        } catch (Exception ex) {
            SgvDialog.error("Erro na Exportação", "Não foi possível gerar o ficheiro:\n" + ex.getMessage());
        }
    }
}
