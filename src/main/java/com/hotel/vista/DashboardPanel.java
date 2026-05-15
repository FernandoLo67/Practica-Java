package com.hotel.vista;

import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionDB;
import com.hotel.util.HotelConfig;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel de Dashboard — vista ejecutiva con datos en tiempo real.
 *
 * Versión 3.0 — mejoras:
 *   - 6 KPIs (2 filas de 3): + Checkouts hoy + Tasa de ocupación %
 *   - Banner de bienvenida con nombre real del hotel (HotelConfig)
 *   - Alertas de actividad pendiente hoy (check-ins y checkouts sin gestionar)
 *   - 3 tablas de agenda: check-ins, check-outs, top 5 clientes del mes
 *   - Gráfica de barras: ingresos mensuales en vez de solo conteo
 *   - Gráfica de pie: habitaciones por estado
 *
 * @author Fernando
 * @version 3.0
 */
public class DashboardPanel extends JPanel {

    private final Usuario usuarioActual;

    // KPI labels
    private JLabel lblHabDisponibles;
    private JLabel lblReservasActivas;
    private JLabel lblCheckinsHoy;
    private JLabel lblCheckoutsHoy;
    private JLabel lblIngresosMes;
    private JLabel lblOcupacion;
    private JLabel lblEstado;

    // Banner
    private JLabel lblBannerNombre;
    private JLabel lblBannerSub;

    // Alertas
    private JPanel panelAlertas;

    // Tablas de agenda
    private DefaultTableModel modeloCheckins;
    private DefaultTableModel modeloCheckouts;
    private DefaultTableModel modeloTopClientes;

    // Gráficas JFreeChart
    private DefaultCategoryDataset datasetBarras;
    private DefaultPieDataset      datasetPie;

    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FMT_MONEDA =
        NumberFormat.getCurrencyInstance(new Locale("es", "GT"));

    /** Auto-refresh cada 5 minutos */
    private static final int INTERVALO_REFRESH_MS = 5 * 60 * 1000;
    private Timer timerAutoRefresh;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DashboardPanel(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearCuerpo(),     BorderLayout.CENTER);

        cargarDatos();

        // Auto-refresh: recarga datos cada 5 minutos mientras el panel está visible
        timerAutoRefresh = new Timer(INTERVALO_REFRESH_MS, e -> cargarDatos());
        timerAutoRefresh.setRepeats(true);
        timerAutoRefresh.start();

        // Detener el timer cuando el panel se remueva de la jerarquía
        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override public void ancestorAdded(javax.swing.event.AncestorEvent e)   { timerAutoRefresh.start();  }
            @Override public void ancestorRemoved(javax.swing.event.AncestorEvent e) { timerAutoRefresh.stop();   }
            @Override public void ancestorMoved(javax.swing.event.AncestorEvent e)   {}
        });
    }

    // =========================================================
    // ENCABEZADO
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
    // CUERPO
    // =========================================================

    private JScrollPane crearCuerpo() {
        JPanel cuerpo = new JPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBackground(Tema.COLOR_FONDO);
        cuerpo.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Fila 1: KPIs principales (3 tarjetas)
        cuerpo.add(crearFilaKPIsPrincipales());
        cuerpo.add(Box.createVerticalStrut(10));

        // Fila 2: KPIs secundarios (3 tarjetas)
        cuerpo.add(crearFilaKPIsSecundarios());
        cuerpo.add(Box.createVerticalStrut(16));

        // Banner de bienvenida con nombre del hotel
        cuerpo.add(crearBannerBienvenida());
        cuerpo.add(Box.createVerticalStrut(16));

        // Panel de alertas (visible solo cuando hay pendientes)
        panelAlertas = new JPanel(new BorderLayout());
        panelAlertas.setOpaque(false);
        panelAlertas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        cuerpo.add(panelAlertas);

        // Agenda: 3 paneles en paralelo
        JPanel agenda = new JPanel(new GridLayout(1, 3, 14, 0));
        agenda.setOpaque(false);
        agenda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        agenda.add(crearTablaCheckins());
        agenda.add(crearTablaCheckouts());
        agenda.add(crearTablaTopClientes());
        cuerpo.add(agenda);
        cuerpo.add(Box.createVerticalStrut(18));

        // Gráficas
        cuerpo.add(crearGraficas());

        JScrollPane scroll = new JScrollPane(cuerpo);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Tema.COLOR_FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // =========================================================
    // KPIs — FILA 1 (principales)
    // =========================================================

    private JPanel crearFilaKPIsPrincipales() {
        JPanel fila = new JPanel(new GridLayout(1, 3, 14, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        lblHabDisponibles  = new JLabel("...");
        lblReservasActivas = new JLabel("...");
        lblIngresosMes     = new JLabel("...");

        fila.add(UIHelper.tarjetaKPI("🟢", "Hab. Disponibles",  lblHabDisponibles,  Tema.COLOR_EXITO));
        fila.add(UIHelper.tarjetaKPI("📅", "Reservas Activas",  lblReservasActivas, Tema.COLOR_INFO));
        fila.add(UIHelper.tarjetaKPI("💰", "Ingresos del Mes",  lblIngresosMes,     new Color(5, 150, 105)));

        return fila;
    }

    // =========================================================
    // KPIs — FILA 2 (secundarios)
    // =========================================================

    private JPanel crearFilaKPIsSecundarios() {
        JPanel fila = new JPanel(new GridLayout(1, 3, 14, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        lblCheckinsHoy  = new JLabel("...");
        lblCheckoutsHoy = new JLabel("...");
        lblOcupacion    = new JLabel("...");

        fila.add(tarjetaKPICompacta("✅", "Check-ins hoy",    lblCheckinsHoy,  new Color(251, 146, 60)));
        fila.add(tarjetaKPICompacta("🚪", "Check-outs hoy",   lblCheckoutsHoy, new Color(99, 102, 241)));
        fila.add(tarjetaKPICompacta("📊", "Tasa de ocupación", lblOcupacion,    new Color(14, 165, 233)));

        return fila;
    }

    /** Tarjeta KPI más compacta para la segunda fila. */
    private JPanel tarjetaKPICompacta(String icono, String etiqueta, JLabel lblValor, Color color) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 240), 1),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblIcono = new JLabel(icono + "  " + etiqueta);
        lblIcono.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblIcono.setForeground(new Color(90, 100, 130));

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValor.setForeground(color);

        p.add(lblIcono, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        return p;
    }

    // =========================================================
    // BANNER DE BIENVENIDA
    // =========================================================

    private JPanel crearBannerBienvenida() {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setBackground(Tema.COLOR_PRIMARIO);
        p.setBorder(new EmptyBorder(18, 24, 18, 24));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        // Izquierda: saludo + usuario
        String saludo = obtenerSaludo();
        JLabel lblSaludo = new JLabel(saludo + ", " + usuarioActual.getNombre() + "  👋");
        lblSaludo.setFont(Tema.FUENTE_TITULO);
        lblSaludo.setForeground(Color.WHITE);

        JLabel lblRol = new JLabel(usuarioActual.getRol()
            + "  ·  " + HotelConfig.getNombre());
        lblRol.setFont(Tema.FUENTE_SMALL);
        lblRol.setForeground(new Color(180, 195, 255));

        JPanel izq = new JPanel(new GridLayout(2, 1, 0, 3));
        izq.setOpaque(false);
        izq.add(lblSaludo);
        izq.add(lblRol);
        p.add(izq, BorderLayout.WEST);

        // Derecha: nombre del hotel + slogan
        lblBannerNombre = new JLabel("🏨  " + HotelConfig.getNombre());
        lblBannerNombre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBannerNombre.setForeground(Color.WHITE);
        lblBannerNombre.setHorizontalAlignment(SwingConstants.RIGHT);

        lblBannerSub = new JLabel(HotelConfig.getSlogan());
        lblBannerSub.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblBannerSub.setForeground(new Color(180, 195, 255));
        lblBannerSub.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel der = new JPanel(new GridLayout(2, 1, 0, 3));
        der.setOpaque(false);
        der.add(lblBannerNombre);
        der.add(lblBannerSub);
        p.add(der, BorderLayout.EAST);

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
            new String[]{"Cliente", "Hab.", "Fecha"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return crearPanelTabla("✅  Check-ins pendientes hoy",
            modeloCheckins, new int[]{180, 55, 85});
    }

    private JPanel crearTablaCheckouts() {
        modeloCheckouts = new DefaultTableModel(
            new String[]{"Cliente", "Hab.", "Fecha"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return crearPanelTabla("🚪  Check-outs pendientes hoy",
            modeloCheckouts, new int[]{180, 55, 85});
    }

    private JPanel crearTablaTopClientes() {
        modeloTopClientes = new DefaultTableModel(
            new String[]{"Cliente", "Reservas"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JPanel p = crearPanelTabla("⭐  Top 5 clientes del mes",
            modeloTopClientes, new int[]{230, 70});
        return p;
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

        // Centrar columna de números
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            if (i == anchos.length - 1) {
                tabla.getColumnModel().getColumn(i).setCellRenderer(centrado);
            }
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(null);

        p.add(lbl,    BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // =========================================================
    // ALERTAS
    // =========================================================

    private void actualizarAlertas(DashboardData d) {
        panelAlertas.removeAll();

        List<String> alertas = new ArrayList<>();
        if (d.checkinsHoy > 0)
            alertas.add("⚠  " + d.checkinsHoy + " check-in(s) pendiente(s) hoy");
        if (d.checkoutsHoy > 0)
            alertas.add("⚠  " + d.checkoutsHoy + " check-out(s) pendiente(s) hoy");

        if (!alertas.isEmpty()) {
            JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            strip.setBackground(new Color(254, 243, 199));
            strip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(245, 158, 11)),
                new EmptyBorder(0, 10, 0, 10)
            ));
            strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            for (String alerta : alertas) {
                JLabel lbl = new JLabel(alerta);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(new Color(120, 53, 15));
                strip.add(lbl);
            }
            panelAlertas.add(strip, BorderLayout.CENTER);
            panelAlertas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            panelAlertas.setBorder(new EmptyBorder(0, 0, 14, 0));
        } else {
            panelAlertas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        }

        panelAlertas.revalidate();
        panelAlertas.repaint();
    }

    // =========================================================
    // GRÁFICAS JFREECHART
    // =========================================================

    private JPanel crearGraficas() {
        JPanel contenedor = new JPanel(new GridLayout(1, 2, 14, 0));
        contenedor.setOpaque(false);
        contenedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        // --- Gráfica de barras: ingresos por mes ---
        datasetBarras = new DefaultCategoryDataset();
        JFreeChart barChart = ChartFactory.createBarChart(
            null, null, "Ingresos (Q)", datasetBarras);
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

        ChartPanel panelBarra = new ChartPanel(barChart);
        panelBarra.setBackground(Color.WHITE);
        panelBarra.setPreferredSize(new Dimension(0, 280));
        contenedor.add(wrapGrafica("💰  Ingresos mensuales — últimos 6 meses", panelBarra));

        // --- Gráfica de pie: habitaciones por estado ---
        datasetPie = new DefaultPieDataset();
        JFreeChart pieChart = ChartFactory.createPieChart(
            null, datasetPie, true, false, false);
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

    private void cargarDatos() {
        lblEstado.setText("Actualizando...");

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() { return consultarDatos(); }

            @Override
            protected void done() {
                try {
                    DashboardData d = get();
                    actualizarUI(d);
                    lblEstado.setText("Actualizado " +
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

            // KPI: habitaciones disponibles + total para tasa
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT estado, COUNT(*) AS cnt FROM habitaciones GROUP BY estado");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int cnt = rs.getInt("cnt");
                    d.totalHabs += cnt;
                    if ("DISPONIBLE".equals(rs.getString("estado"))) d.habDisponibles = cnt;
                    d.habsPorEstado.put(rs.getString("estado"), cnt);
                }
            }

            // KPI: reservaciones activas
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reservaciones " +
                    "WHERE estado IN ('PENDIENTE','CONFIRMADA','CHECKIN')");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.reservasActivas = rs.getInt(1);
            }

            // KPI: check-ins pendientes hoy (CONFIRMADA con fecha_checkin = hoy)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reservaciones " +
                    "WHERE estado = 'CONFIRMADA' AND fecha_checkin = CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.checkinsHoy = rs.getInt(1);
            }

            // KPI: check-outs pendientes hoy (CHECKIN con fecha_checkout = hoy)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reservaciones " +
                    "WHERE estado = 'CHECKIN' AND fecha_checkout = CURDATE()");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.checkoutsHoy = rs.getInt(1);
            }

            // KPI: ingresos del mes (facturas PAGADA)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total),0) FROM facturas " +
                    "WHERE estado = 'PAGADA' " +
                    "AND MONTH(fecha_emision) = MONTH(CURDATE()) " +
                    "AND YEAR(fecha_emision)  = YEAR(CURDATE())");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.ingresosMes = rs.getDouble(1);
            }

            // Agenda: check-ins de hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONCAT(c.nombre,' ',c.apellido) AS cliente, " +
                    "       h.numero, r.fecha_checkin " +
                    "FROM reservaciones r " +
                    "JOIN clientes    c ON r.id_cliente    = c.id " +
                    "JOIN habitaciones h ON r.id_habitacion = h.id " +
                    "WHERE r.estado = 'CONFIRMADA' AND r.fecha_checkin = CURDATE() " +
                    "ORDER BY c.apellido LIMIT 15");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) d.checkins.add(new String[]{
                    rs.getString("cliente"), rs.getString("numero"),
                    rs.getDate("fecha_checkin").toLocalDate().format(FMT_FECHA)
                });
            }

            // Agenda: check-outs de hoy
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONCAT(c.nombre,' ',c.apellido) AS cliente, " +
                    "       h.numero, r.fecha_checkout " +
                    "FROM reservaciones r " +
                    "JOIN clientes    c ON r.id_cliente    = c.id " +
                    "JOIN habitaciones h ON r.id_habitacion = h.id " +
                    "WHERE r.estado = 'CHECKIN' AND r.fecha_checkout = CURDATE() " +
                    "ORDER BY c.apellido LIMIT 15");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) d.checkouts.add(new String[]{
                    rs.getString("cliente"), rs.getString("numero"),
                    rs.getDate("fecha_checkout").toLocalDate().format(FMT_FECHA)
                });
            }

            // Top 5 clientes del mes (por número de reservaciones)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT CONCAT(c.nombre,' ',c.apellido) AS cliente, COUNT(*) AS total " +
                    "FROM reservaciones r " +
                    "JOIN clientes c ON r.id_cliente = c.id " +
                    "WHERE MONTH(r.fecha_reserva) = MONTH(CURDATE()) " +
                    "AND   YEAR(r.fecha_reserva)  = YEAR(CURDATE()) " +
                    "GROUP BY r.id_cliente, cliente " +
                    "ORDER BY total DESC LIMIT 5");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) d.topClientes.add(new Object[]{
                    rs.getString("cliente"), rs.getInt("total")
                });
            }

            // Gráfica de barras: ingresos por mes (últimos 6 meses)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE_FORMAT(fecha_emision, '%b %Y') AS etiqueta, " +
                    "       COALESCE(SUM(total), 0) AS total " +
                    "FROM facturas " +
                    "WHERE estado = 'PAGADA' " +
                    "AND fecha_emision >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
                    "GROUP BY YEAR(fecha_emision), MONTH(fecha_emision), etiqueta " +
                    "ORDER BY YEAR(fecha_emision), MONTH(fecha_emision)");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    d.ingresosPorMes.put(rs.getString("etiqueta"), rs.getDouble("total"));
            }

        } catch (Exception ex) {
            // Datos quedan en 0
        }
        return d;
    }

    private void actualizarUI(DashboardData d) {
        // KPIs principales
        lblHabDisponibles.setText(String.valueOf(d.habDisponibles));
        lblReservasActivas.setText(String.valueOf(d.reservasActivas));
        lblIngresosMes.setText(d.ingresosMes == 0 ? "Q 0.00"
            : String.format("Q %.2f", d.ingresosMes));

        // KPIs secundarios
        lblCheckinsHoy.setText(String.valueOf(d.checkinsHoy));
        lblCheckoutsHoy.setText(String.valueOf(d.checkoutsHoy));

        // Tasa de ocupación: (total - disponibles) / total × 100
        if (d.totalHabs > 0) {
            int ocupadas = d.totalHabs - d.habDisponibles;
            int tasa     = (int) Math.round((double) ocupadas / d.totalHabs * 100);
            lblOcupacion.setText(tasa + " %");
            lblOcupacion.setForeground(
                tasa >= 80 ? Tema.COLOR_EXITO :
                tasa >= 50 ? new Color(14, 165, 233) :
                             Tema.COLOR_ERROR);
        } else {
            lblOcupacion.setText("—");
        }

        // Colorear check-ins hoy
        lblCheckinsHoy.setForeground(
            d.checkinsHoy > 0 ? new Color(245, 158, 11) : Tema.COLOR_EXITO);
        lblCheckoutsHoy.setForeground(
            d.checkoutsHoy > 0 ? new Color(99, 102, 241) : Tema.COLOR_EXITO);

        // Banner: actualizar nombre (puede haber cambiado en DatosHotelPanel)
        lblBannerNombre.setText("🏨  " + HotelConfig.getNombre());
        lblBannerSub.setText(HotelConfig.getSlogan());

        // Alertas
        actualizarAlertas(d);

        // Tablas
        modeloCheckins.setRowCount(0);
        d.checkins.forEach(f -> modeloCheckins.addRow(f));
        if (d.checkins.isEmpty())
            modeloCheckins.addRow(new String[]{"Sin check-ins hoy", "", ""});

        modeloCheckouts.setRowCount(0);
        d.checkouts.forEach(f -> modeloCheckouts.addRow(f));
        if (d.checkouts.isEmpty())
            modeloCheckouts.addRow(new String[]{"Sin check-outs hoy", "", ""});

        modeloTopClientes.setRowCount(0);
        d.topClientes.forEach(f -> modeloTopClientes.addRow(f));
        if (d.topClientes.isEmpty())
            modeloTopClientes.addRow(new Object[]{"Sin datos este mes", ""});

        // Gráfica de barras: ingresos por mes
        datasetBarras.clear();
        if (d.ingresosPorMes.isEmpty()) {
            datasetBarras.addValue(0, "Ingresos", "Sin datos");
        } else {
            d.ingresosPorMes.forEach((k, v) ->
                datasetBarras.addValue(v, "Ingresos", k));
        }

        // Gráfica de pie: habitaciones por estado
        datasetPie.clear();
        if (d.habsPorEstado.isEmpty()) {
            datasetPie.setValue("Sin datos", 1);
        } else {
            d.habsPorEstado.forEach(datasetPie::setValue);
        }

        revalidate();
        repaint();
    }

    // =========================================================
    // DTO
    // =========================================================

    private static class DashboardData {
        int    habDisponibles  = 0;
        int    totalHabs       = 0;
        int    reservasActivas = 0;
        int    checkinsHoy     = 0;
        int    checkoutsHoy    = 0;
        double ingresosMes     = 0;

        List<String[]>   checkins     = new ArrayList<>();
        List<String[]>   checkouts    = new ArrayList<>();
        List<Object[]>   topClientes  = new ArrayList<>();

        LinkedHashMap<String, Double>  ingresosPorMes = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> habsPorEstado  = new LinkedHashMap<>();
    }
}
