package com.hotel.vista;

import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionDB;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Panel de Dashboard — vista ejecutiva con datos en tiempo real.
 *
 * Secciones:
 *   - 4 tarjetas KPI: habitaciones disponibles, reservas activas,
 *     check-ins de hoy, ingresos del mes
 *   - Agenda del día: tabla con check-ins y check-outs pendientes
 *   - Última actividad: top 5 clientes con más reservaciones
 *
 * Usa SwingWorker para cargar los datos sin congelar la interfaz.
 *
 * @author Fernando
 * @version 2.0
 */
public class DashboardPanel extends JPanel {

    private final Usuario usuarioActual;

    // KPI labels (se actualizan con SwingWorker)
    private JLabel lblHabDisponibles;
    private JLabel lblReservasActivas;
    private JLabel lblCheckinsHoy;
    private JLabel lblIngresosMes;
    private JLabel lblEstado;

    // Tablas de agenda
    private DefaultTableModel modeloCheckins;
    private DefaultTableModel modeloCheckouts;

    // Panel principal de agenda (para actualizar fácilmente)
    private JPanel panelAgenda;

    // Gráficas JFreeChart
    private DefaultCategoryDataset datasetBarras;
    private DefaultPieDataset      datasetPie;

    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FMT_MONEDA =
        NumberFormat.getCurrencyInstance(new Locale("es", "GT"));

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DashboardPanel(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);

        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCuerpo(),      BorderLayout.CENTER);

        cargarDatos();
    }

    // =========================================================
    // UI — ENCABEZADO
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = UIHelper.panelEncabezado();

        JPanel izq = new JPanel(new GridLayout(2, 1, 0, 2));
        izq.setOpaque(false);
        JLabel titulo = UIHelper.titulo("🏠  Dashboard");
        JLabel subtitulo = new JLabel("Resumen en tiempo real · " +
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy",
                new Locale("es", "ES"))));
        subtitulo.setFont(Tema.FUENTE_SMALL);
        subtitulo.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
        izq.add(titulo);
        izq.add(subtitulo);
        p.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        lblEstado = new JLabel("Cargando...");
        lblEstado.setFont(Tema.FUENTE_SMALL);
        lblEstado.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
        JButton btnRefresh = UIHelper.botonSecundario("↺  Actualizar");
        btnRefresh.addActionListener(e -> cargarDatos());
        der.add(lblEstado);
        der.add(btnRefresh);
        p.add(der, BorderLayout.EAST);

        return p;
    }

    // =========================================================
    // UI — CUERPO
    // =========================================================

    private JScrollPane crearCuerpo() {
        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBackground(Tema.COLOR_FONDO);
        cuerpo.setBorder(new EmptyBorder(16, 18, 16, 18));

        cuerpo.add(crearFilaKPIs());
        cuerpo.add(Box.createVerticalStrut(18));
        cuerpo.add(crearBienvenida());
        cuerpo.add(Box.createVerticalStrut(18));

        panelAgenda = new JPanel(new GridLayout(1, 2, 14, 0));
        panelAgenda.setOpaque(false);
        panelAgenda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        panelAgenda.add(crearTablaCheckins());
        panelAgenda.add(crearTablaCheckouts());
        cuerpo.add(panelAgenda);

        cuerpo.add(Box.createVerticalStrut(18));
        cuerpo.add(crearGraficas());

        JScrollPane scroll = new JScrollPane(cuerpo);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Tema.COLOR_FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // =========================================================
    // KPIs
    // =========================================================

    private JPanel crearFilaKPIs() {
        JPanel fila = new JPanel(new GridLayout(1, 4, 14, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        lblHabDisponibles = new JLabel("...");
        lblReservasActivas = new JLabel("...");
        lblCheckinsHoy     = new JLabel("...");
        lblIngresosMes     = new JLabel("...");

        fila.add(UIHelper.tarjetaKPI("🟢", "Hab. Disponibles", lblHabDisponibles, Tema.COLOR_EXITO));
        fila.add(UIHelper.tarjetaKPI("📅", "Reservas Activas",  lblReservasActivas, Tema.COLOR_INFO));
        fila.add(UIHelper.tarjetaKPI("✅", "Check-ins Hoy",     lblCheckinsHoy,     Tema.COLOR_ACENTO));
        fila.add(UIHelper.tarjetaKPI("💰", "Ingresos del Mes",  lblIngresosMes,     Tema.COLOR_EXITO));

        return fila;
    }

    // =========================================================
    // BIENVENIDA
    // =========================================================

    private JPanel crearBienvenida() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_PRIMARIO);
        p.setBorder(new EmptyBorder(18, 22, 18, 22));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        String saludo = obtenerSaludo();
        JLabel lbl = new JLabel(saludo + ", " + usuarioActual.getNombre() + "  👋");
        lbl.setFont(Tema.FUENTE_TITULO);
        lbl.setForeground(Color.WHITE);

        JLabel rol = new JLabel(usuarioActual.getRol() + "  ·  Hotel Sistema v2.0");
        rol.setFont(Tema.FUENTE_SMALL);
        rol.setForeground(new Color(180, 195, 255));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 3));
        textos.setOpaque(false);
        textos.add(lbl);
        textos.add(rol);
        p.add(textos, BorderLayout.WEST);

        return p;
    }

    private String obtenerSaludo() {
        int hora = java.time.LocalTime.now().getHour();
        if (hora < 12) return "Buenos días";
        if (hora < 18) return "Buenas tardes";
        return "Buenas noches";
    }

    // =========================================================
    // TABLAS DE AGENDA
    // =========================================================

    private JPanel crearTablaCheckins() {
        modeloCheckins = new DefaultTableModel(
            new String[]{"Cliente", "Hab.", "Fecha CI"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return crearPanelTabla("✅  Check-ins pendientes hoy", modeloCheckins,
            new int[]{200, 60, 90});
    }

    private JPanel crearTablaCheckouts() {
        modeloCheckouts = new DefaultTableModel(
            new String[]{"Cliente", "Hab.", "Fecha CO"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return crearPanelTabla("🚪  Check-outs pendientes hoy", modeloCheckouts,
            new int[]{200, 60, 90});
    }

    private JPanel crearPanelTabla(String titulo, DefaultTableModel modelo, int[] anchos) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.COLOR_BORDE, 1),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lbl = new JLabel(titulo);
        lbl.setFont(Tema.FUENTE_BOLD);
        lbl.setForeground(Tema.COLOR_PRIMARIO);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));

        JTable tabla = new JTable(modelo);
        UIHelper.estilizarTabla(tabla);
        tabla.setRowHeight(28);
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(null);

        p.add(lbl,    BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // =========================================================
    // GRÁFICAS JFREECHART
    // =========================================================

    private JPanel crearGraficas() {
        JPanel contenedor = new JPanel(new GridLayout(1, 2, 14, 0));
        contenedor.setOpaque(false);
        contenedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        // --- Gráfica de barras: reservaciones por mes ---
        datasetBarras = new DefaultCategoryDataset();
        JFreeChart barChart = ChartFactory.createBarChart(
            null, null, "Reservaciones",
            datasetBarras
        );
        barChart.setBackgroundPaint(Color.WHITE);
        barChart.removeLegend();

        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 224, 240));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Tema.COLOR_PRIMARIO);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.12);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(Tema.FUENTE_SMALL);
        domainAxis.setAxisLineVisible(false);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(Tema.FUENTE_SMALL);
        rangeAxis.setStandardTickUnits(org.jfree.chart.axis.NumberAxis.createIntegerTickUnits());

        ChartPanel panelBarra = new ChartPanel(barChart);
        panelBarra.setBackground(Color.WHITE);
        panelBarra.setPreferredSize(new Dimension(0, 280));

        contenedor.add(wrapGrafica("📊  Reservaciones — últimos 6 meses", panelBarra));

        // --- Gráfica de pie: habitaciones por estado ---
        datasetPie = new DefaultPieDataset();
        JFreeChart pieChart = ChartFactory.createPieChart(null, datasetPie, true, false, false);
        pieChart.setBackgroundPaint(Color.WHITE);

        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setBackgroundPaint(Color.WHITE);
        piePlot.setOutlineVisible(false);
        piePlot.setSectionPaint("DISPONIBLE",    Tema.COLOR_DISPONIBLE);
        piePlot.setSectionPaint("OCUPADA",       Tema.COLOR_OCUPADA);
        piePlot.setSectionPaint("RESERVADA",     Tema.COLOR_RESERVADA);
        piePlot.setSectionPaint("MANTENIMIENTO", Tema.COLOR_MANTENIMIENTO);
        piePlot.setLabelFont(Tema.FUENTE_SMALL);
        piePlot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        piePlot.setShadowPaint(null);

        ChartPanel panelPie = new ChartPanel(pieChart);
        panelPie.setBackground(Color.WHITE);

        contenedor.add(wrapGrafica("🏨  Habitaciones por estado", panelPie));

        return contenedor;
    }

    private JPanel wrapGrafica(String titulo, ChartPanel chart) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.COLOR_BORDE, 1),
            new EmptyBorder(12, 14, 12, 14)
        ));
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(Tema.FUENTE_BOLD);
        lbl.setForeground(Tema.COLOR_PRIMARIO);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        p.add(lbl,   BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);
        return p;
    }

    // =========================================================
    // CARGA ASÍNCRONA DE DATOS
    // =========================================================

    /**
     * Lanza un SwingWorker para cargar los datos sin bloquear la UI.
     * Mientras carga muestra "..." en los KPIs.
     */
    private void cargarDatos() {
        lblEstado.setText("Actualizando...");

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() {
                return consultarDatos();
            }

            @Override
            protected void done() {
                try {
                    DashboardData d = get();
                    actualizarUI(d);
                    lblEstado.setText("Actualizado a las " +
                        java.time.LocalTime.now()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                } catch (Exception ex) {
                    lblEstado.setText("Error al cargar datos");
                }
            }
        }.execute();
    }

    private DashboardData consultarDatos() {
        DashboardData d = new DashboardData();
        try (Connection conn = ConexionDB.getConexion()) {

            // KPI: habitaciones disponibles
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM habitaciones WHERE estado = 'DISPONIBLE'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.habDisponibles = rs.getInt(1);
            }

            // KPI: reservaciones activas (PENDIENTE + CONFIRMADA + CHECKIN)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reservaciones " +
                    "WHERE estado IN ('PENDIENTE','CONFIRMADA','CHECKIN')");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.reservasActivas = rs.getInt(1);
            }

            // KPI: check-ins pendientes hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reservaciones " +
                    "WHERE estado = 'CONFIRMADA' AND fecha_checkin = CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.checkinsHoy = rs.getInt(1);
            }

            // KPI: ingresos del mes (facturas PAGADA)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total), 0) FROM facturas " +
                    "WHERE estado = 'PAGADA' " +
                    "AND MONTH(fecha_emision) = MONTH(CURDATE()) " +
                    "AND YEAR(fecha_emision)  = YEAR(CURDATE())");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.ingresosMes = rs.getDouble(1);
            }

            // Agenda: check-ins de hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONCAT(c.nombre,' ',c.apellido) AS cliente, " +
                    "       h.numero AS hab, r.fecha_checkin " +
                    "FROM reservaciones r " +
                    "JOIN clientes    c ON r.id_cliente    = c.id " +
                    "JOIN habitaciones h ON r.id_habitacion = h.id " +
                    "WHERE r.estado = 'CONFIRMADA' AND r.fecha_checkin = CURDATE() " +
                    "ORDER BY c.apellido LIMIT 15");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    d.checkins.add(new String[]{
                        rs.getString("cliente"),
                        rs.getString("hab"),
                        rs.getDate("fecha_checkin").toLocalDate().format(FMT_FECHA)
                    });
                }
            }

            // Agenda: check-outs de hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONCAT(c.nombre,' ',c.apellido) AS cliente, " +
                    "       h.numero AS hab, r.fecha_checkout " +
                    "FROM reservaciones r " +
                    "JOIN clientes    c ON r.id_cliente    = c.id " +
                    "JOIN habitaciones h ON r.id_habitacion = h.id " +
                    "WHERE r.estado = 'CHECKIN' AND r.fecha_checkout = CURDATE() " +
                    "ORDER BY c.apellido LIMIT 15");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    d.checkouts.add(new String[]{
                        rs.getString("cliente"),
                        rs.getString("hab"),
                        rs.getDate("fecha_checkout").toLocalDate().format(FMT_FECHA)
                    });
                }
            }

            // Gráfica: reservaciones por mes (últimos 6 meses)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE_FORMAT(fecha_reserva, '%b %Y') AS etiqueta, COUNT(*) AS total " +
                    "FROM reservaciones " +
                    "WHERE fecha_reserva >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
                    "GROUP BY YEAR(fecha_reserva), MONTH(fecha_reserva), etiqueta " +
                    "ORDER BY YEAR(fecha_reserva), MONTH(fecha_reserva)");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    d.reservasPorMes.put(rs.getString("etiqueta"), rs.getInt("total"));
                }
            }

            // Gráfica: habitaciones por estado
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT estado, COUNT(*) AS total FROM habitaciones GROUP BY estado");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    d.habsPorEstado.put(rs.getString("estado"), rs.getInt("total"));
                }
            }

        } catch (Exception ex) {
            // Datos quedan en 0, la UI mostrará "-"
        }
        return d;
    }

    private void actualizarUI(DashboardData d) {
        lblHabDisponibles.setText(String.valueOf(d.habDisponibles));
        lblReservasActivas.setText(String.valueOf(d.reservasActivas));
        lblCheckinsHoy.setText(String.valueOf(d.checkinsHoy));

        // Ingresos: formato moneda guatemalteca
        String monto = d.ingresosMes == 0
            ? "Q 0.00"
            : String.format("Q %.2f", d.ingresosMes);
        lblIngresosMes.setText(monto);

        // Colorear check-ins hoy si hay pendientes
        lblCheckinsHoy.setForeground(
            d.checkinsHoy > 0 ? Tema.COLOR_ERROR : Tema.COLOR_EXITO);

        // Llenar tablas de agenda
        modeloCheckins.setRowCount(0);
        for (String[] fila : d.checkins) modeloCheckins.addRow(fila);
        if (d.checkins.isEmpty()) {
            modeloCheckins.addRow(new String[]{"Sin check-ins hoy", "", ""});
        }

        modeloCheckouts.setRowCount(0);
        for (String[] fila : d.checkouts) modeloCheckouts.addRow(fila);
        if (d.checkouts.isEmpty()) {
            modeloCheckouts.addRow(new String[]{"Sin check-outs hoy", "", ""});
        }

        // Gráfica de barras: reservaciones por mes
        datasetBarras.clear();
        for (Map.Entry<String, Integer> e : d.reservasPorMes.entrySet()) {
            datasetBarras.addValue(e.getValue(), "Reservaciones", e.getKey());
        }
        if (d.reservasPorMes.isEmpty()) {
            datasetBarras.addValue(0, "Reservaciones", "Sin datos");
        }

        // Gráfica de pie: habitaciones por estado
        datasetPie.clear();
        for (Map.Entry<String, Integer> e : d.habsPorEstado.entrySet()) {
            datasetPie.setValue(e.getKey(), e.getValue());
        }
        if (d.habsPorEstado.isEmpty()) {
            datasetPie.setValue("Sin datos", 1);
        }

        revalidate();
        repaint();
    }

    // =========================================================
    // DTO para los datos del dashboard
    // =========================================================

    private static class DashboardData {
        int    habDisponibles  = 0;
        int    reservasActivas = 0;
        int    checkinsHoy     = 0;
        double ingresosMes     = 0;
        java.util.List<String[]>    checkins      = new java.util.ArrayList<>();
        java.util.List<String[]>    checkouts     = new java.util.ArrayList<>();
        LinkedHashMap<String, Integer> reservasPorMes = new LinkedHashMap<>();
        Map<String, Integer>           habsPorEstado  = new LinkedHashMap<>();
    }
}
