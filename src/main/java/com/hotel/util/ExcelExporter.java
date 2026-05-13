package com.hotel.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para exportar JTable a archivos Excel (.xlsx) con Apache POI.
 *
 * Uso:
 *   ExcelExporter.exportar(tabla, "Clientes", ventanaPadre);
 *
 * Características:
 *   - Encabezado coloreado con el nombre del módulo
 *   - Filas alternas para mejor legibilidad
 *   - Columna ID (índice 0) omitida automáticamente
 *   - Respeta el orden/filtro actual del TableRowSorter
 *   - Ofrece abrir el archivo tras exportar
 *
 * @author Fernando
 * @version 1.0
 */
public final class ExcelExporter {

    private static final DateTimeFormatter FMT_HORA =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Constructor privado — clase de utilidades */
    private ExcelExporter() {}

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    /**
     * Muestra JFileChooser y exporta la tabla a .xlsx.
     *
     * @param tabla      La JTable a exportar (respeta filtros y orden)
     * @param modulo     Nombre del módulo (aparece como título en el Excel)
     * @param parent     Componente padre para el JFileChooser y los diálogos
     */
    public static void exportar(JTable tabla, String modulo, Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar " + modulo + " a Excel");
        chooser.setSelectedFile(new File(modulo + "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Archivos Excel (*.xlsx)", "xlsx"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File archivo = chooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".xlsx")) {
            archivo = new File(archivo.getAbsolutePath() + ".xlsx");
        }

        final File archivoFinal = archivo;

        // Generar en hilo aparte para no congelar la UI
        new Thread(() -> {
            try {
                generarExcel(tabla, modulo, archivoFinal);

                final File af = archivoFinal;
                SwingUtilities.invokeLater(() -> {
                    int op = JOptionPane.showConfirmDialog(parent,
                        "✅  Archivo exportado correctamente:\n" + af.getName() +
                        "\n\n¿Deseas abrirlo ahora?",
                        "Exportación exitosa", JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                    if (op == JOptionPane.YES_OPTION) {
                        try { Desktop.getDesktop().open(af); } catch (Exception ex) {
                            JOptionPane.showMessageDialog(parent,
                                "No se pudo abrir el archivo automáticamente.\n" +
                                "Ruta: " + af.getAbsolutePath(), "Aviso",
                                JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(parent,
                        "Error al exportar:\n" + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE));
            }
        }, "excel-export").start();
    }

    // =========================================================
    // GENERACIÓN DEL ARCHIVO
    // =========================================================

    private static void generarExcel(JTable tabla, String modulo, File archivo) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            XSSFSheet hoja = wb.createSheet(modulo);

            // --- Estilos ---
            XSSFCellStyle estiloTitulo = crearEstiloTitulo(wb);
            XSSFCellStyle estiloHeader = crearEstiloHeader(wb);
            XSSFCellStyle estiloFilaPar   = crearEstiloFila(wb, new XSSFColor(
                new byte[]{(byte)240, (byte)244, (byte)255}, null));
            XSSFCellStyle estiloFilaImpar = crearEstiloFila(wb, null);
            XSSFCellStyle estiloFecha = crearEstiloFila(wb, new XSSFColor(
                new byte[]{(byte)250, (byte)250, (byte)250}, null));

            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            int colCount = modelo.getColumnCount();

            // Columnas visibles (omite columna 0 = ID)
            int colInicio = (colCount > 0) ? 1 : 0;
            int colVisibles = colCount - colInicio;

            // --- Fila de título (fusionada) ---
            Row filaTitulo = hoja.createRow(0);
            filaTitulo.setHeightInPoints(28);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("📋  " + modulo + "   —   exportado el " +
                LocalDateTime.now().format(FMT_HORA));
            celdaTitulo.setCellStyle(estiloTitulo);
            if (colVisibles > 1) {
                hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, colVisibles - 1));
            }

            // --- Fila de encabezados ---
            Row filaHeader = hoja.createRow(1);
            filaHeader.setHeightInPoints(22);
            for (int c = colInicio; c < colCount; c++) {
                Cell cell = filaHeader.createCell(c - colInicio);
                cell.setCellValue(modelo.getColumnName(c));
                cell.setCellStyle(estiloHeader);
            }

            // --- Filas de datos (respeta filtro/orden actual) ---
            int filaExcel = 2;
            int rowCount = tabla.getRowCount(); // filas visibles (con sorter)
            for (int r = 0; r < rowCount; r++) {
                int modelRow = tabla.convertRowIndexToModel(r);
                Row fila = hoja.createRow(filaExcel++);
                fila.setHeightInPoints(18);
                XSSFCellStyle estilo = (r % 2 == 0) ? estiloFilaPar : estiloFilaImpar;
                for (int c = colInicio; c < colCount; c++) {
                    Cell cell = fila.createCell(c - colInicio);
                    Object valor = modelo.getValueAt(modelRow, c);
                    if (valor != null) cell.setCellValue(valor.toString());
                    cell.setCellStyle(estilo);
                }
            }

            // --- Autoajustar columnas ---
            for (int c = 0; c < colVisibles; c++) {
                hoja.autoSizeColumn(c);
                int ancho = hoja.getColumnWidth(c);
                hoja.setColumnWidth(c, Math.min(ancho + 1024, 20000)); // máx ~14cm
            }

            // --- Guardar ---
            try (FileOutputStream out = new FileOutputStream(archivo)) {
                wb.write(out);
            }
        }
    }

    // =========================================================
    // ESTILOS
    // =========================================================

    private static XSSFCellStyle crearEstiloTitulo(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 13);
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26,(byte)35,(byte)126}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        return s;
    }

    private static XSSFCellStyle crearEstiloHeader(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(new byte[]{(byte)26,(byte)35,(byte)126}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)232,(byte)234,(byte)246}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)26,(byte)35,(byte)126}, null));
        return s;
    }

    private static XSSFCellStyle crearEstiloFila(XSSFWorkbook wb, XSSFColor bgColor) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        if (bgColor != null) {
            s.setFillForegroundColor(bgColor);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)220,(byte)224,(byte)240}, null));
        return s;
    }
}
