package com.hotel.vista;

import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reservacion;
import com.hotel.util.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

/**
 * Panel de Calendario de Ocupación.
 *
 * Muestra una vista estilo Gantt:
 *   - Filas   = habitaciones del hotel
 *   - Columnas = días del mes seleccionado
 *   - Celdas  = coloreadas según el estado de la reservación activa ese día
 *
 * Navegación: botones ◀ / ▶ para cambiar de mes.
 * Doble clic en una celda muestra el detalle de la reservación.
 *
 * @author Fernando
 * @version 1.0
 */
public class CalendarioPanel extends JPanel {

    // ── Constantes de color por estado ───────────────────────────────────────
    private final Color C_CONFIRMADA  = Tema.COLOR_CONFIRMADA;
    private final Color C_CHECKIN     = Tema.COLOR_CHECKIN;
    private final Color C_PENDIENTE   = Tema.COLOR_PENDIENTE;
    private final Color C_CHECKOUT    = Tema.COLOR_CHECKOUT;
    private final Color C_CANCELADA   = Tema.COLOR_CANCELADA;

    // ── Estado ───────────────────────────────────────────────────────────────
    private YearMonth mesActual = YearMonth.now();

    /** ocupacion[habitacion.id][dia] = Reservacion (o null si libre) */
    private final Map<Integer, Map<Integer, Reservacion>> ocupacion = new HashMap<>();
    private List<Habitacion> habitaciones = new ArrayList<>();

    // ── DAO ──────────────────────────────────────────────────────────────────
    private final ReservacionDAOImpl resDAO = new ReservacionDAOImpl();
    private final HabitacionDAOImpl  habDAO = new HabitacionDAOImpl();

    // ── UI ───────────────────────────────────────────────────────────────────
    private JLabel           lblMes;
    private JTable           tabla;
    private DefaultTableModel modeloTabla;
    private JLabel           lblEstado;

    public CalendarioPanel() {
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);
        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearTabla(),      BorderLayout.CENTER);
        add(crearLeyenda(),    BorderLayout.SOUTH);
        cargar();
    }

    // =========================================================
    // UI
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_BLANCO);
        p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.COLOR_BORDE),
            new EmptyBorder(12, 20, 12, 20)
        ));

        // Título
        JLabel titulo = new JLabel("📅  Calendario de Ocupación");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(Tema.COLOR_PRIMARIO);
        p.add(titulo, BorderLayout.WEST);

        // Navegación de mes
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        nav.setOpaque(false);

        JButton btnPrev = botonNav("◀");
        lblMes = new JLabel("", SwingConstants.CENTER);
        lblMes.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblMes.setForeground(Tema.COLOR_PRIMARIO);
        lblMes.setPreferredSize(new Dimension(200, 28));
        JButton btnNext = botonNav("▶");
        JButton btnHoy  = botonNav("Hoy");
        btnHoy.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnPrev.addActionListener(e -> { mesActual = mesActual.minusMonths(1); cargar(); });
        btnNext.addActionListener(e -> { mesActual = mesActual.plusMonths(1);  cargar(); });
        btnHoy .addActionListener(e -> { mesActual = YearMonth.now();          cargar(); });

        nav.add(btnPrev);
        nav.add(lblMes);
        nav.add(btnNext);
        nav.add(Box.createHorizontalStrut(16));
        nav.add(btnHoy);
        p.add(nav, BorderLayout.CENTER);

        // Botón actualizar
        JButton btnRefresh = new JButton("↺");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btnRefresh.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
        btnRefresh.setBackground(Tema.COLOR_BLANCO);
        btnRefresh.setOpaque(true);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setToolTipText("Actualizar");
        btnRefresh.addActionListener(e -> cargar());
        p.add(btnRefresh, BorderLayout.EAST);

        return p;
    }

    private JButton botonNav(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(Tema.COLOR_PRIMARIO);
        b.setBackground(Tema.COLOR_FONDO);
        b.setOpaque(true);
        b.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(Tema.COLOR_BORDE, 1),
            new EmptyBorder(4, 10, 4, 10)
        ));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JScrollPane crearTabla() {
        modeloTabla = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tabla.setRowHeight(28);
        tabla.setShowGrid(true);
        tabla.setGridColor(Tema.COLOR_BORDE);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabla.getTableHeader().setBackground(Tema.COLOR_HEADER_TABLA);
        tabla.getTableHeader().setForeground(Tema.COLOR_PRIMARIO);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 34));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderer personalizado
        tabla.setDefaultRenderer(Object.class, new GanttRenderer());

        // Doble clic → detalle de reservación
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) mostrarDetalle();
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Tema.COLOR_BLANCO);
        // Fijar ancho de primera columna (nombre habitación)
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    private JPanel crearLeyenda() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.COLOR_BORDE),
            new EmptyBorder(2, 12, 2, 12)
        ));

        lblEstado = new JLabel(" ");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEstado.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);

        p.add(chipLeyenda(C_CONFIRMADA, "Confirmada"));
        p.add(chipLeyenda(C_CHECKIN,    "Check-In activo"));
        p.add(chipLeyenda(C_PENDIENTE,  "Pendiente"));
        p.add(chipLeyenda(C_CHECKOUT,   "Check-Out"));
        p.add(Box.createHorizontalStrut(20));
        p.add(lblEstado);
        return p;
    }

    private JLabel chipLeyenda(Color color, String texto) {
        JLabel l = new JLabel("  " + texto + "  ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(Color.WHITE);
        l.setBackground(color);
        l.setOpaque(true);
        l.setBorder(new EmptyBorder(2, 4, 2, 4));
        return l;
    }

    // =========================================================
    // DATOS
    // =========================================================

    private void cargar() {
        // Actualizar título
        String nombreMes = mesActual.getMonth()
            .getDisplayName(TextStyle.FULL, new Locale("es", "GT"));
        lblMes.setText(nombreMes.substring(0, 1).toUpperCase()
            + nombreMes.substring(1) + "  " + mesActual.getYear());

        int diasEnMes = mesActual.lengthOfMonth();

        // Fechas del mes
        LocalDate primerDia = mesActual.atDay(1);
        LocalDate ultimoDia = mesActual.atEndOfMonth();
        Date desde = Date.valueOf(primerDia);
        Date hasta  = Date.valueOf(ultimoDia);

        // Cargar habitaciones y reservaciones
        habitaciones = habDAO.listarTodas();
        List<Reservacion> reservaciones = resDAO.listarEnRango(desde, hasta);

        // Construir mapa de ocupación: habId → día → reservación
        ocupacion.clear();
        for (Habitacion h : habitaciones) {
            ocupacion.put(h.getId(), new HashMap<>());
        }
        for (Reservacion r : reservaciones) {
            int habId = r.getHabitacion().getId();
            if (!ocupacion.containsKey(habId)) continue;

            LocalDate ci = r.getFechaCheckin().toLocalDate();
            LocalDate co = r.getFechaCheckout().toLocalDate();

            // Marcar cada día desde checkin hasta checkout-1 (el día checkout no es noche)
            for (LocalDate d = ci; d.isBefore(co); d = d.plusDays(1)) {
                if (d.getYear()  == mesActual.getYear() &&
                    d.getMonthValue() == mesActual.getMonthValue()) {
                    ocupacion.get(habId).put(d.getDayOfMonth(), r);
                }
            }
        }

        // Construir columnas: [Habitación, Tipo, 1, 2, 3, ..., diasEnMes]
        String[] cols = new String[2 + diasEnMes];
        cols[0] = "Hab.";
        cols[1] = "Tipo";
        for (int d = 1; d <= diasEnMes; d++) cols[d + 1] = String.valueOf(d);

        modeloTabla.setColumnCount(0);
        modeloTabla.setRowCount(0);
        for (String col : cols) modeloTabla.addColumn(col);

        // Filas: una por habitación
        for (Habitacion h : habitaciones) {
            Object[] fila = new Object[2 + diasEnMes];
            fila[0] = h.getNumero();
            fila[1] = h.getTipo() != null ? h.getTipo().getNombre() : "";
            Map<Integer, Reservacion> dias = ocupacion.getOrDefault(h.getId(), new HashMap<>());
            for (int d = 1; d <= diasEnMes; d++) {
                fila[d + 1] = dias.get(d); // null si libre
            }
            modeloTabla.addRow(fila);
        }

        // Ajustar anchos de columna
        tabla.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabla.getColumnModel().getColumn(0).setMinWidth(55);
        tabla.getColumnModel().getColumn(0).setMaxWidth(55);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(1).setMinWidth(70);
        for (int d = 2; d < 2 + diasEnMes; d++) {
            tabla.getColumnModel().getColumn(d).setPreferredWidth(26);
            tabla.getColumnModel().getColumn(d).setMinWidth(22);
            tabla.getColumnModel().getColumn(d).setMaxWidth(40);
        }

        // Resaltar columna del día de hoy
        tabla.getTableHeader().repaint();

        // Contar ocupación del mes
        long totalCeldas = (long) habitaciones.size() * diasEnMes;
        long ocupadas = ocupacion.values().stream()
            .mapToLong(m -> m.values().stream().distinct().count())
            .sum();
        // Noches-habitación ocupadas en el mes
        long nochesOcupadas = ocupacion.values().stream()
            .mapToLong(Map::size).sum();
        int pct = totalCeldas > 0 ? (int)(nochesOcupadas * 100 / totalCeldas) : 0;

        lblEstado.setText(String.format(
            "🏨  %d habitaciones   |   %d noches-habitación ocupadas   |   Tasa del mes: %d%%",
            habitaciones.size(), nochesOcupadas, pct));
    }

    // =========================================================
    // DETALLE
    // =========================================================

    private void mostrarDetalle() {
        int fila = tabla.getSelectedRow();
        int col  = tabla.getSelectedColumn();
        if (fila < 0 || col < 2) return;

        Object val = modeloTabla.getValueAt(fila, col);
        if (!(val instanceof Reservacion)) {
            JOptionPane.showMessageDialog(this,
                "No hay reservación en esta celda.", "Celda libre",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Reservacion r = (Reservacion) val;
        String detalle = String.format(
            "Reservación #%d\n" +
            "Cliente:    %s\n" +
            "Documento:  %s\n" +
            "Habitación: %s  (Piso %d — %s)\n" +
            "Check-In:   %s\n" +
            "Check-Out:  %s\n" +
            "Noches:     %d\n" +
            "Estado:     %s\n" +
            "Total:      Q %.2f",
            r.getId(),
            r.getCliente().getNombreCompleto(),
            r.getCliente().getDocumento(),
            r.getHabitacion().getNumero(),
            r.getHabitacion().getPiso(),
            r.getHabitacion().getTipo().getNombre(),
            r.getFechaCheckin(),
            r.getFechaCheckout(),
            r.getNoches(),
            r.getEstado(),
            r.getTotalConImpuesto()
        );

        JTextArea area = new JTextArea(detalle);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(250, 252, 255));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Detalle de Reservación", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // RENDERER GANTT
    // =========================================================

    private class GanttRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {

            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(1, 1, 1, 1));
            setText("");
            setToolTipText(null);

            // Columnas de información (Hab. y Tipo)
            if (col == 0 || col == 1) {
                setText(v != null ? v.toString() : "");
                setHorizontalAlignment(col == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
                setFont(col == 0
                    ? new Font("Segoe UI", Font.BOLD, 11)
                    : new Font("Segoe UI", Font.PLAIN, 10));
                if (!sel) {
                    setBackground(row % 2 == 0 ? Tema.COLOR_FILA_PAR : Tema.COLOR_FILA_IMPAR);
                    setForeground(Tema.COLOR_TEXTO);
                }
                return this;
            }

            // Celda de día
            if (!sel) {
                // Resaltar el día de hoy
                LocalDate hoy = LocalDate.now();
                boolean esMesActual = mesActual.equals(YearMonth.now());
                int diaColumna = col - 1; // col 2 = día 1
                if (esMesActual && diaColumna == hoy.getDayOfMonth()) {
                    setBackground(new Color(255, 248, 225));
                } else {
                    setBackground(row % 2 == 0 ? Tema.COLOR_FILA_PAR : Tema.COLOR_FILA_IMPAR);
                }
            }

            if (v instanceof Reservacion) {
                Reservacion r = (Reservacion) v;
                Color c = colorEstado(r.getEstado());
                if (!sel) setBackground(c);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 9));
                // Mostrar inicial del apellido del cliente
                String cli = r.getCliente().getApellido();
                setText(cli != null && !cli.isEmpty() ? cli.substring(0, 1) : "");

                // Tooltip con detalle rápido
                setToolTipText(String.format(
                    "<html><b>%s</b><br>Hab. %s — %s<br>%s → %s<br>Estado: <b>%s</b></html>",
                    r.getCliente().getNombreCompleto(),
                    r.getHabitacion().getNumero(),
                    r.getHabitacion().getTipo().getNombre(),
                    r.getFechaCheckin(), r.getFechaCheckout(),
                    r.getEstado()
                ));
            } else {
                if (!sel) setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
            }

            return this;
        }

        private Color colorEstado(String estado) {
            if (estado == null) return C_PENDIENTE;
            switch (estado) {
                case "CONFIRMADA": return C_CONFIRMADA;
                case "CHECKIN":    return C_CHECKIN;
                case "PENDIENTE":  return C_PENDIENTE;
                case "CHECKOUT":   return C_CHECKOUT;
                case "CANCELADA":  return C_CANCELADA;
                default:           return C_PENDIENTE;
            }
        }
    }
}
