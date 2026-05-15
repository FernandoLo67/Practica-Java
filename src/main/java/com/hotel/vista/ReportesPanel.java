package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.dao.impl.FacturaDAOImpl;
import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.util.ConexionDB;
import com.hotel.util.HotelConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Panel de Reportes — resumen ejecutivo del sistema.
 *
 * Muestra:
 *   - Tarjetas KPI: clientes, habitaciones disponibles, reservaciones activas, ingresos
 *   - Ocupación actual por tipo de habitación
 *   - Reservaciones por estado (gráfico de barras simple)
 *   - Top 5 clientes más frecuentes
 *
 * @author Fernando
 * @version 1.0
 */
public class ReportesPanel extends JPanel {

    private final ClienteDAOImpl      clienteDAO;
    private final HabitacionDAOImpl   habitacionDAO;
    private final ReservacionDAOImpl  reservacionDAO;
    private final FacturaDAOImpl      facturaDAO;

    private final Color COLOR_PRIMARIO = com.hotel.util.Tema.COLOR_PRIMARIO;
    private final Color COLOR_FONDO    = com.hotel.util.Tema.COLOR_FONDO;

    // Filtro de fechas
    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private static final SimpleDateFormat FMT_UI  = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat FMT_SQL = new SimpleDateFormat("yyyy-MM-dd");

    public ReportesPanel() {
        this.clienteDAO     = new ClienteDAOImpl();
        this.habitacionDAO  = new HabitacionDAOImpl();
        this.reservacionDAO = new ReservacionDAOImpl();
        this.facturaDAO     = new FacturaDAOImpl();
        setLayout(new BorderLayout());
        setBackground(COLOR_FONDO);
        initComponents();
    }

    private void initComponents() {
        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearContenido(),  BorderLayout.CENTER);
    }

    private JPanel crearEncabezado() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        // Fila superior: título + botones
        JPanel filaSup = new JPanel(new BorderLayout());
        filaSup.setOpaque(false);

        JLabel lbl = new JLabel("📊  Reportes y Estadísticas");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(COLOR_PRIMARIO);

        JButton btnRefresh = botonHeader("🔄 Actualizar", new Color(100, 116, 139));
        btnRefresh.addActionListener(e -> { removeAll(); initComponents(); revalidate(); repaint(); });

        JButton btnPDF = botonHeader("📄 Exportar PDF", new Color(183, 28, 28));
        btnPDF.addActionListener(e -> exportarPDF());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnRefresh);
        botones.add(btnPDF);

        filaSup.add(lbl,     BorderLayout.WEST);
        filaSup.add(botones, BorderLayout.EAST);

        // Fila inferior: filtro de fechas
        JPanel filaFechas = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filaFechas.setOpaque(false);

        // Valores por defecto: primer día del mes hasta hoy
        Calendar cal = Calendar.getInstance();
        String hoy = FMT_UI.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String primerDia = FMT_UI.format(cal.getTime());

        filaFechas.add(label("Desde:"));
        txtFechaDesde = campoPequeno(primerDia);
        filaFechas.add(txtFechaDesde);
        filaFechas.add(label("Hasta:"));
        txtFechaHasta = campoPequeno(hoy);
        filaFechas.add(txtFechaHasta);

        JButton btnFiltrar = botonHeader("🔍 Filtrar", COLOR_PRIMARIO);
        btnFiltrar.addActionListener(e -> { removeAll(); initComponents(); revalidate(); repaint(); });
        filaFechas.add(btnFiltrar);

        JButton btnHoy     = botonSmall("Hoy");
        JButton btnMes     = botonSmall("Este mes");
        JButton btnAnio    = botonSmall("Este ano");
        btnHoy.addActionListener(e  -> setRango(0,  0));
        btnMes.addActionListener(e  -> setRango(30, 0));
        btnAnio.addActionListener(e -> setRango(365,0));
        filaFechas.add(btnHoy);
        filaFechas.add(btnMes);
        filaFechas.add(btnAnio);

        p.add(filaSup,    BorderLayout.NORTH);
        p.add(filaFechas, BorderLayout.SOUTH);
        return p;
    }

    private void setRango(int diasAtras, int diasAdelante) {
        Calendar desde = Calendar.getInstance();
        desde.add(Calendar.DAY_OF_YEAR, -diasAtras);
        Calendar hasta = Calendar.getInstance();
        hasta.add(Calendar.DAY_OF_YEAR, diasAdelante);
        txtFechaDesde.setText(FMT_UI.format(desde.getTime()));
        txtFechaHasta.setText(FMT_UI.format(hasta.getTime()));
        removeAll(); initComponents(); revalidate(); repaint();
    }

    /** Devuelve las fechas del filtro en formato SQL yyyy-MM-dd. */
    private String[] getFechasSql() {
        try {
            String desde = FMT_SQL.format(FMT_UI.parse(txtFechaDesde.getText().trim()));
            String hasta = FMT_SQL.format(FMT_UI.parse(txtFechaHasta.getText().trim()));
            return new String[]{ desde, hasta };
        } catch (ParseException e) {
            // Si el formato es inválido, usar el mes actual
            Calendar c = Calendar.getInstance();
            String hoy = FMT_SQL.format(c.getTime());
            c.set(Calendar.DAY_OF_MONTH, 1);
            return new String[]{ FMT_SQL.format(c.getTime()), hoy };
        }
    }

    private JButton botonHeader(String texto, Color bg) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(7, 12, 7, 12));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton botonSmall(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setBackground(new Color(235, 239, 255));
        b.setForeground(COLOR_PRIMARIO);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(5, 10, 5, 10));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(60, 70, 100));
        return l;
    }

    private JTextField campoPequeno(String valor) {
        JTextField f = new JTextField(valor, 10);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        f.setPreferredSize(new Dimension(100, 28));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 200, 225), 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return f;
    }

    private JScrollPane crearContenido() {
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBackground(COLOR_FONDO);
        contenido.setBorder(new EmptyBorder(16, 20, 20, 20));

        contenido.add(crearKPIs());
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(crearFilaGraficos());
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(crearTopClientes());

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // =========================================================
    // KPIs
    // =========================================================

    private JPanel crearKPIs() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 16, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        String[] fechas = getFechasSql();
        int totalClientes     = clienteDAO.contarTodos();
        int disponibles       = habitacionDAO.contarPorEstado("DISPONIBLE");
        int ocupadas          = habitacionDAO.contarPorEstado("OCUPADA");
        double ingresosPeriodo = 0;
        int    resPeriodo      = 0;

        try (Connection conn = ConexionDB.getConexion()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total),0) FROM facturas " +
                    "WHERE estado='PAGADA' AND fecha_emision BETWEEN ? AND ?")) {
                ps.setDate(1, Date.valueOf(fechas[0]));
                ps.setDate(2, Date.valueOf(fechas[1]));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) ingresosPeriodo = rs.getDouble(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reservaciones " +
                    "WHERE fecha_reserva BETWEEN ? AND ?")) {
                ps.setDate(1, Date.valueOf(fechas[0]));
                ps.setDate(2, Date.valueOf(fechas[1]));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) resPeriodo = rs.getInt(1);
            }
        } catch (Exception ignored) {}

        panel.add(kpi("👥", "Total Clientes",     String.valueOf(totalClientes),          new Color(63, 81, 181)));
        panel.add(kpi("📅", "Reservas (periodo)", String.valueOf(resPeriodo),             new Color(46, 125, 50)));
        panel.add(kpi("🔴", "Hab. Ocupadas",      String.valueOf(ocupadas),               new Color(198, 40, 40)));
        panel.add(kpi("💰", "Ingresos (periodo)", String.format("Q %.2f", ingresosPeriodo), new Color(230, 81, 0)));

        return panel;
    }

    private JPanel kpi(String icono, String titulo, String valor, Color color) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(14, 18, 14, 18)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;

        JLabel lblTit = new JLabel(icono + "  " + titulo);
        lblTit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTit.setForeground(new Color(100, 110, 140));
        p.add(lblTit, g);

        g.gridy = 1;
        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblVal.setForeground(color);
        p.add(lblVal, g);

        return p;
    }

    // =========================================================
    // FILA DE GRÁFICOS
    // =========================================================

    private JPanel crearFilaGraficos() {
        JPanel fila = new JPanel(new GridLayout(1, 2, 16, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        fila.add(crearGraficoReservaciones());
        fila.add(crearOcupacionPorTipo());
        return fila;
    }

    private JPanel crearGraficoReservaciones() {
        JPanel panel = crearPanelCard("📅  Reservaciones por Estado");

        int[] valores = {
            reservacionDAO.contarPorEstado("PENDIENTE"),
            reservacionDAO.contarPorEstado("CONFIRMADA"),
            reservacionDAO.contarPorEstado("CHECKIN"),
            reservacionDAO.contarPorEstado("CHECKOUT"),
            reservacionDAO.contarPorEstado("CANCELADA")
        };
        String[] etiquetas = {"PENDIENTE", "CONFIRMADA", "CHECKIN", "CHECKOUT", "CANCELADA"};
        Color[]  colores   = {
            new Color(230, 81, 0), new Color(46, 125, 50),
            new Color(21, 101, 192), new Color(100, 100, 100), new Color(198, 40, 40)
        };

        panel.add(new GraficoBarras(valores, etiquetas, colores), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearOcupacionPorTipo() {
        JPanel panel = crearPanelCard("🛏  Ocupación por Tipo de Habitación");
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        lista.setBorder(new EmptyBorder(8, 8, 8, 8));

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT t.nombre, COUNT(h.id) AS total, " +
                "SUM(CASE WHEN h.estado='OCUPADA' THEN 1 ELSE 0 END) AS ocupadas " +
                "FROM habitaciones h INNER JOIN tipo_habitacion t ON h.id_tipo = t.id " +
                "GROUP BY t.nombre ORDER BY t.nombre");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String tipo    = rs.getString("nombre");
                int total      = rs.getInt("total");
                int ocupadas   = rs.getInt("ocupadas");
                double pct     = total > 0 ? (ocupadas * 100.0 / total) : 0;

                JPanel fila = new JPanel(new BorderLayout(8, 0));
                fila.setOpaque(false);
                fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                fila.setBorder(new EmptyBorder(4, 0, 4, 0));

                JLabel lblNombre = new JLabel(tipo);
                lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblNombre.setForeground(COLOR_PRIMARIO);
                lblNombre.setPreferredSize(new Dimension(90, 20));

                JPanel barraContenedor = new JPanel(new BorderLayout());
                barraContenedor.setOpaque(false);
                BarraProgreso barra = new BarraProgreso((int) pct);
                barraContenedor.add(barra, BorderLayout.CENTER);

                JLabel lblInfo = new JLabel(ocupadas + "/" + total + "  (" + String.format("%.0f", pct) + "%)");
                lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lblInfo.setForeground(new Color(80, 90, 120));
                lblInfo.setPreferredSize(new Dimension(80, 20));

                fila.add(lblNombre,       BorderLayout.WEST);
                fila.add(barraContenedor, BorderLayout.CENTER);
                fila.add(lblInfo,         BorderLayout.EAST);
                lista.add(fila);
            }
        } catch (Exception e) {
            lista.add(new JLabel("Error al cargar datos"));
        }

        panel.add(lista, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // TOP CLIENTES
    // =========================================================

    private JPanel crearTopClientes() {
        JPanel panel = crearPanelCard("🏆  Top 5 Clientes con más Reservaciones");
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JPanel lista = new JPanel(new GridLayout(0, 1, 0, 4));
        lista.setOpaque(false);
        lista.setBorder(new EmptyBorder(8, 8, 8, 8));

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT CONCAT(c.nombre, ' ', c.apellido) AS nombre, COUNT(r.id) AS total " +
                "FROM reservaciones r INNER JOIN clientes c ON r.id_cliente = c.id " +
                "GROUP BY c.id ORDER BY total DESC LIMIT 5");
             ResultSet rs = ps.executeQuery()) {

            int pos = 1;
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int total     = rs.getInt("total");
                String medalla = pos == 1 ? "🥇" : pos == 2 ? "🥈" : pos == 3 ? "🥉" : "  " + pos + ".";

                JPanel fila = new JPanel(new BorderLayout());
                fila.setOpaque(false);

                JLabel lblNom = new JLabel(medalla + "  " + nombre);
                lblNom.setFont(new Font("Segoe UI", pos <= 3 ? Font.BOLD : Font.PLAIN, 13));
                lblNom.setForeground(COLOR_PRIMARIO);

                JLabel lblCant = new JLabel(total + " reservacion(es)");
                lblCant.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lblCant.setForeground(new Color(100, 110, 140));

                fila.add(lblNom,  BorderLayout.WEST);
                fila.add(lblCant, BorderLayout.EAST);
                lista.add(fila);
                pos++;
            }
            if (pos == 1) {
                lista.add(new JLabel("  Sin datos disponibles"));
            }
        } catch (Exception e) {
            lista.add(new JLabel("Error al cargar datos"));
        }

        panel.add(lista, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private JPanel crearPanelCard(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(14, 16, 14, 16)
        ));
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(COLOR_PRIMARIO);
        lbl.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.add(lbl, BorderLayout.NORTH);
        return p;
    }

    // =========================================================
    // EXPORTAR PDF
    // =========================================================

    /**
     * Genera un reporte en PDF con los datos actuales del sistema.
     * Usa Apache PDFBox para crear el documento y Java Desktop para abrirlo.
     */
    private void exportarPDF() {
        // Seleccionar destino
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte como PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
        String nombre = "Reporte_Hotel_" +
            new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".pdf";
        chooser.setSelectedFile(new File(nombre));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File archivo = chooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
            archivo = new File(archivo.getAbsolutePath() + ".pdf");
        }
        // Verificar que el filtro de fechas existe (puede ser null si viene del refreshed state)
        if (txtFechaDesde == null) txtFechaDesde = campoPequeno("01/01/2024");
        if (txtFechaHasta == null) txtFechaHasta = campoPequeno(new SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()));

        final File destino = archivo;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Generar en hilo aparte para no bloquear la UI
        new Thread(() -> {
            try {
                generarPDF(destino);
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    int ok = JOptionPane.showConfirmDialog(ReportesPanel.this,
                        "PDF generado correctamente.\n" + destino.getName() +
                        "\n\n¿Deseas abrirlo ahora?",
                        "Exportación exitosa", JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                    if (ok == JOptionPane.YES_OPTION) {
                        try {
                            java.awt.Desktop.getDesktop().open(destino);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(ReportesPanel.this,
                                "El PDF fue guardado pero no se pudo abrir automáticamente.\n" +
                                "Ruta: " + destino.getAbsolutePath(),
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(ReportesPanel.this,
                        "Error al generar el PDF:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "pdf-generator").start();
    }

    private void generarPDF(File archivo) throws IOException {
        // Obtener datos actuales
        int totalClientes    = clienteDAO.contarTodos();
        int disponibles      = habitacionDAO.contarPorEstado("DISPONIBLE");
        int ocupadas         = habitacionDAO.contarPorEstado("OCUPADA");
        int resActivas       = reservacionDAO.contarPorEstado("PENDIENTE")
                             + reservacionDAO.contarPorEstado("CONFIRMADA")
                             + reservacionDAO.contarPorEstado("CHECKIN");
        double ingresos      = facturaDAO.sumarTotalPorEstado("PAGADA");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float ancho = page.getMediaBox().getWidth();
            float margen = 50;
            float y = page.getMediaBox().getHeight() - margen;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ── Cabecera ──────────────────────────────────────
                cs.setNonStrokingColor(26f/255, 35f/255, 126f/255);
                cs.addRect(0, y - 10, ancho, 55);
                cs.fill();

                String[] fechasFiltro = getFechasSql();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.newLineAtOffset(margen, y + 20);
                cs.showText(HotelConfig.getNombre() + " - Reporte Ejecutivo");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.setNonStrokingColor(0.7f, 0.8f, 1f);
                cs.newLineAtOffset(margen, y + 4);
                cs.showText("Periodo: " + txtFechaDesde.getText() + " al " + txtFechaHasta.getText()
                    + "   Generado: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                cs.endText();

                y -= 70;

                // ── Sección KPIs ─────────────────────────────────
                cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(margen, y);
                cs.showText("Resumen General");
                cs.endText();
                y -= 8;

                // Línea separadora
                cs.setStrokingColor(0.7f, 0.75f, 0.9f);
                cs.setLineWidth(1);
                cs.moveTo(margen, y);
                cs.lineTo(ancho - margen, y);
                cs.stroke();
                y -= 20;

                // Tabla KPIs
                String[][] kpis = {
                    {"Clientes registrados",         String.valueOf(totalClientes)},
                    {"Habitaciones disponibles",     String.valueOf(disponibles)},
                    {"Habitaciones ocupadas",        String.valueOf(ocupadas)},
                    {"Reservaciones activas",        String.valueOf(resActivas)},
                    {"Ingresos totales (facturados)", String.format("Q %.2f", ingresos)}
                };
                for (String[] fila : kpis) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                    cs.newLineAtOffset(margen + 10, y);
                    cs.showText(fila[0]);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.setNonStrokingColor(0.1f, 0.14f, 0.5f);
                    cs.newLineAtOffset(ancho - margen - 80, y);
                    cs.showText(fila[1]);
                    cs.endText();

                    y -= 22;
                }

                // ── Sección Reservaciones por estado ─────────────
                y -= 20;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                cs.newLineAtOffset(margen, y);
                cs.showText("Reservaciones por Estado");
                cs.endText();
                y -= 8;
                cs.setStrokingColor(0.7f, 0.75f, 0.9f);
                cs.moveTo(margen, y); cs.lineTo(ancho - margen, y); cs.stroke();
                y -= 20;

                String[] estados = {"PENDIENTE","CONFIRMADA","CHECKIN","CHECKOUT","CANCELADA"};
                for (String est : estados) {
                    int cnt = reservacionDAO.contarPorEstado(est);
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                    cs.newLineAtOffset(margen + 10, y);
                    cs.showText(est);
                    cs.endText();
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.setNonStrokingColor(0.1f, 0.14f, 0.5f);
                    cs.newLineAtOffset(ancho - margen - 60, y);
                    cs.showText(String.valueOf(cnt));
                    cs.endText();
                    y -= 22;
                }

                // ── Top clientes ─────────────────────────────────
                y -= 20;
                if (y > 150) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                    cs.newLineAtOffset(margen, y);
                    cs.showText("Top 5 Clientes");
                    cs.endText();
                    y -= 8;
                    cs.setStrokingColor(0.7f, 0.75f, 0.9f);
                    cs.moveTo(margen, y); cs.lineTo(ancho - margen, y); cs.stroke();
                    y -= 20;

                    try (Connection conn = ConexionDB.getConexion();
                         PreparedStatement ps = conn.prepareStatement(
                             "SELECT CONCAT(c.nombre,' ',c.apellido) AS n, COUNT(r.id) AS t " +
                             "FROM reservaciones r JOIN clientes c ON r.id_cliente=c.id " +
                             "GROUP BY c.id ORDER BY t DESC LIMIT 5");
                         ResultSet rs = ps.executeQuery()) {
                        int pos = 1;
                        while (rs.next() && y > 80) {
                            String cliente = pos + ". " + rs.getString("n");
                            String reservas = rs.getInt("t") + " reservacion(es)";
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, 12);
                            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f);
                            cs.newLineAtOffset(margen + 10, y);
                            cs.showText(cliente);
                            cs.endText();
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, 11);
                            cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
                            cs.newLineAtOffset(ancho - margen - 110, y);
                            cs.showText(reservas);
                            cs.endText();
                            y -= 22;
                            pos++;
                        }
                    } catch (Exception ignored) {}
                }

                // ── Pie de página ─────────────────────────────────
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.newLineAtOffset(margen, 30);
                cs.showText(HotelConfig.getNombre() + "  -  Reporte generado automaticamente  -  " +
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                cs.endText();
            }

            doc.save(archivo);
        }
    }

    // =========================================================
    // COMPONENTE: Gráfico de Barras
    // =========================================================

    static class GraficoBarras extends JPanel {
        private final int[]    valores;
        private final String[] etiquetas;
        private final Color[]  colores;

        GraficoBarras(int[] valores, String[] etiquetas, Color[] colores) {
            this.valores   = valores;
            this.etiquetas = etiquetas;
            this.colores   = colores;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 180));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int margenIzq = 10, margenDer = 10, margenSup = 20, margenInf = 40;
            int areaW = w - margenIzq - margenDer;
            int areaH = h - margenSup - margenInf;

            int max = 1;
            for (int v : valores) if (v > max) max = v;

            int n = valores.length;
            int anchoTotal = areaW / n;
            int anchoBarra = (int) (anchoTotal * 0.6);
            int espacioIzq = (anchoTotal - anchoBarra) / 2;

            for (int i = 0; i < n; i++) {
                int x = margenIzq + i * anchoTotal + espacioIzq;
                int alturaBarra = valores[i] == 0 ? 2 : (int) ((double) valores[i] / max * areaH);
                int y = margenSup + areaH - alturaBarra;

                g2.setColor(colores[i]);
                g2.fillRoundRect(x, y, anchoBarra, alturaBarra, 6, 6);

                // Valor encima
                g2.setColor(new Color(50, 60, 80));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String val = String.valueOf(valores[i]);
                int sw = g2.getFontMetrics().stringWidth(val);
                g2.drawString(val, x + (anchoBarra - sw) / 2, y - 4);

                // Etiqueta debajo
                g2.setColor(new Color(80, 90, 110));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                String et = etiquetas[i].length() > 7 ? etiquetas[i].substring(0, 7) : etiquetas[i];
                int ew = g2.getFontMetrics().stringWidth(et);
                g2.drawString(et, x + (anchoBarra - ew) / 2, h - margenInf + 14);
            }
        }
    }

    // =========================================================
    // COMPONENTE: Barra de Progreso
    // =========================================================

    static class BarraProgreso extends JPanel {
        private final int porcentaje;

        BarraProgreso(int porcentaje) {
            this.porcentaje = Math.min(100, Math.max(0, porcentaje));
            setOpaque(false);
            setPreferredSize(new Dimension(0, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(220, 225, 240));
            g2.fillRoundRect(0, 2, w, h - 4, h - 4, h - 4);
            if (porcentaje > 0) {
                Color color = porcentaje > 75 ? new Color(198, 40, 40)
                            : porcentaje > 40 ? new Color(230, 81, 0)
                            : new Color(46, 125, 50);
                g2.setColor(color);
                g2.fillRoundRect(0, 2, (int) (w * porcentaje / 100.0), h - 4, h - 4, h - 4);
            }
        }
    }
}
