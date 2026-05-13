package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.dao.impl.FacturaDAOImpl;
import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.util.ConexionDB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    private static final Color COLOR_PRIMARIO = new Color(26, 35, 126);
    private static final Color COLOR_FONDO    = new Color(243, 246, 253);

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
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));
        JLabel lbl = new JLabel("📊  Reportes y Estadísticas");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(COLOR_PRIMARIO);

        JButton btnRefresh = new JButton("🔄 Actualizar");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setBackground(new Color(100, 116, 139));
        btnRefresh.setOpaque(true);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setBorder(new EmptyBorder(8, 14, 8, 14));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> {
            removeAll();
            initComponents();
            revalidate();
            repaint();
        });

        p.add(lbl,        BorderLayout.WEST);
        p.add(btnRefresh, BorderLayout.EAST);
        return p;
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

        int totalClientes     = clienteDAO.contarTodos();
        int disponibles       = habitacionDAO.contarPorEstado("DISPONIBLE");
        int ocupadas          = habitacionDAO.contarPorEstado("OCUPADA");
        int resActivas        = reservacionDAO.contarPorEstado("PENDIENTE")
                              + reservacionDAO.contarPorEstado("CONFIRMADA")
                              + reservacionDAO.contarPorEstado("CHECKIN");
        double ingresosTotales = facturaDAO.sumarTotalPorEstado("PAGADA");

        panel.add(kpi("👥", "Clientes",          String.valueOf(totalClientes),     new Color(63, 81, 181)));
        panel.add(kpi("🟢", "Hab. Disponibles",  String.valueOf(disponibles),       new Color(46, 125, 50)));
        panel.add(kpi("🔴", "Hab. Ocupadas",     String.valueOf(ocupadas),          new Color(198, 40, 40)));
        panel.add(kpi("💰", "Ingresos (Pagados)", String.format("Q %.0f", ingresosTotales), new Color(230, 81, 0)));

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
