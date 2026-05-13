package com.hotel.vista;

import com.hotel.dao.impl.FacturaDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.Factura;
import com.hotel.util.BitacoraService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel de Facturación — lista todas las facturas generadas,
 * permite marcarlas como PAGADAS o ANULADAS.
 *
 * @author Fernando
 * @version 1.0
 */
public class FacturasPanel extends JPanel {

    private JTable            tabla;
    private DefaultTableModel modeloTabla;
    private JLabel            lblEstado;
    private JButton           btnPagar;
    private JButton           btnAnular;
    private JButton           btnDetalle;

    private final FacturaDAOImpl facturaDAO;
    private final Frame          ventanaPadre;

    // Colores delegados a Tema.java
    private static final Color COLOR_PRIMARIO   = com.hotel.util.Tema.COLOR_PRIMARIO;
    private static final Color COLOR_FONDO      = com.hotel.util.Tema.COLOR_FONDO;
    private static final Color COLOR_HEADER     = com.hotel.util.Tema.COLOR_HEADER_TABLA;
    private static final Color COLOR_PENDIENTE  = com.hotel.util.Tema.COLOR_PENDIENTE;
    private static final Color COLOR_PAGADA     = com.hotel.util.Tema.COLOR_EXITO;
    private static final Color COLOR_ANULADA    = com.hotel.util.Tema.COLOR_ERROR;
    private static final Color COLOR_FILA_PAR   = com.hotel.util.Tema.COLOR_FILA_PAR;
    private static final Color COLOR_FILA_IMPAR = com.hotel.util.Tema.COLOR_FILA_IMPAR;

    private static final String[] COLUMNAS = {
        "ID", "Cliente", "Habitación", "Fecha Emisión", "Subtotal (Q)", "IVA (Q)", "Total (Q)", "Método Pago", "Estado"
    };

    public FacturasPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.facturaDAO   = new FacturaDAOImpl();
        setLayout(new BorderLayout());
        setBackground(COLOR_FONDO);
        initComponents();
        cargarFacturas();
    }

    private void initComponents() {
        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCuerpo(),      BorderLayout.CENTER);
        add(crearBarra(),       BorderLayout.SOUTH);
    }

    private JPanel crearEncabezado() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel lbl = new JLabel("🧾  Facturación");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(COLOR_PRIMARIO);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);

        JButton btnRefresh = crearBoton("🔄 Actualizar", new Color(100, 116, 139), false);
        btnDetalle = crearBoton("📄 Ver Detalle", COLOR_PRIMARIO, true);
        btnPagar   = crearBoton("✅ Marcar Pagada", new Color(46, 125, 50), true);
        btnAnular  = crearBoton("✖ Anular", new Color(198, 40, 40), true);

        btnRefresh.addActionListener(e -> cargarFacturas());
        btnDetalle.addActionListener(e -> verDetalle());
        btnPagar  .addActionListener(e -> cambiarEstado(Factura.ESTADO_PAGADA));
        btnAnular .addActionListener(e -> cambiarEstado(Factura.ESTADO_ANULADA));

        botones.add(btnRefresh);
        botones.add(btnDetalle);
        botones.add(btnPagar);
        botones.add(btnAnular);

        p.add(lbl,     BorderLayout.WEST);
        p.add(botones, BorderLayout.EAST);
        return p;
    }

    private JPanel crearCuerpo() {
        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setBackground(COLOR_FONDO);
        cuerpo.add(crearTarjetas(), BorderLayout.NORTH);
        cuerpo.add(crearTabla(),    BorderLayout.CENTER);
        return cuerpo;
    }

    private JPanel crearTarjetas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 14));
        panel.setBackground(COLOR_FONDO);

        JLabel lblPend  = new JLabel("0");
        JLabel lblPag   = new JLabel("0");
        JLabel lblTotal = new JLabel("Q 0.00");

        actualizarTarjetasConLabels(lblPend, lblPag, lblTotal);

        panel.add(tarjeta("⏳", "Pendientes",    lblPend,  COLOR_PENDIENTE));
        panel.add(tarjeta("✅", "Pagadas",        lblPag,   COLOR_PAGADA));
        panel.add(tarjeta("💰", "Ingresos Totales", lblTotal, new Color(63, 81, 181)));

        return panel;
    }

    private void actualizarTarjetasConLabels(JLabel p, JLabel pg, JLabel tot) {
        p  .setText(String.valueOf(facturaDAO.contarPorEstado(Factura.ESTADO_PENDIENTE)));
        pg .setText(String.valueOf(facturaDAO.contarPorEstado(Factura.ESTADO_PAGADA)));
        tot.setText(String.format("Q %.2f", facturaDAO.sumarTotalPorEstado(Factura.ESTADO_PAGADA)));
    }

    private JPanel tarjeta(String icono, String titulo, JLabel numero, Color color) {
        JPanel t = new JPanel(new GridBagLayout());
        t.setBackground(Color.WHITE);
        t.setPreferredSize(new Dimension(180, 80));
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(icono + "  " + titulo);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(100, 110, 140));
        t.add(lbl, g);
        g.gridy = 1;
        numero.setFont(new Font("Segoe UI", Font.BOLD, 22));
        numero.setForeground(color);
        t.add(numero, g);
        return t;
    }

    private JScrollPane crearTabla() {
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(230, 233, 245));
        tabla.setSelectionBackground(new Color(197, 210, 255));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(COLOR_HEADER);
        tabla.getTableHeader().setForeground(COLOR_PRIMARIO);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tabla.setRowSorter(new TableRowSorter<>(modeloTabla));

        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 150, 80, 130, 100, 90, 100, 110, 90};
        for (int i = 0; i < anchos.length; i++)
            if (anchos[i] > 0)
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    setBackground(row % 2 == 0 ? COLOR_FILA_PAR : COLOR_FILA_IMPAR);
                    int cm = tabla.convertColumnIndexToModel(col);
                    if (cm == 8 && v != null) {
                        switch (v.toString()) {
                            case "PENDIENTE": setForeground(COLOR_PENDIENTE); break;
                            case "PAGADA":    setForeground(COLOR_PAGADA);    break;
                            case "ANULADA":   setForeground(COLOR_ANULADA);   break;
                            default:          setForeground(Color.BLACK);
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(Color.BLACK);
                        setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    }
                }
                return this;
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) verDetalle();
            }
        });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            boolean hay = tabla.getSelectedRow() >= 0;
            btnPagar.setEnabled(hay);
            btnAnular.setEnabled(hay);
            btnDetalle.setEnabled(hay);
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel crearBarra() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)),
            new EmptyBorder(6, 16, 6, 16)
        ));
        lblEstado = new JLabel(" ");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(90, 95, 120));
        p.add(lblEstado, BorderLayout.WEST);
        return p;
    }

    public void cargarFacturas() {
        modeloTabla.setRowCount(0);
        List<Factura> lista = facturaDAO.listarTodas();
        for (Factura f : lista) {
            modeloTabla.addRow(new Object[]{
                f.getId(),
                f.getReservacion().getCliente().getNombreCompleto(),
                "Hab. " + f.getReservacion().getHabitacion().getNumero(),
                f.getFechaEmision() != null ? f.getFechaEmision().toString().substring(0, 16) : "-",
                String.format("Q %.2f", f.getSubtotal()),
                String.format("Q %.2f", f.getImpuesto()),
                String.format("Q %.2f", f.getTotal()),
                f.getMetodoPago(),
                f.getEstado()
            });
        }
        lblEstado.setText("🧾  " + lista.size() + " factura(s) encontradas");
        btnPagar.setEnabled(false);
        btnAnular.setEnabled(false);
        btnDetalle.setEnabled(false);
    }

    private void verDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int filaM = tabla.convertRowIndexToModel(fila);
        int id = (int) modeloTabla.getValueAt(filaM, 0);
        Factura f = facturaDAO.buscarPorId(id);
        if (f == null) return;

        String detalle = String.format(
            "══════════════════════════════════\n" +
            "          FACTURA #%d\n" +
            "══════════════════════════════════\n" +
            "Cliente:       %s\n" +
            "Documento:     %s\n" +
            "Habitación:    %s (Piso %d)\n" +
            "Tipo:          %s\n" +
            "Check-In:      %s\n" +
            "Check-Out:     %s\n" +
            "Noches:        %d\n" +
            "──────────────────────────────────\n" +
            "Subtotal:      Q %.2f\n" +
            "IVA (18%%):     Q %.2f\n" +
            "TOTAL:         Q %.2f\n" +
            "──────────────────────────────────\n" +
            "Método de pago: %s\n" +
            "Estado:         %s\n" +
            "Fecha emisión:  %s\n" +
            "══════════════════════════════════",
            f.getId(),
            f.getReservacion().getCliente().getNombreCompleto(),
            f.getReservacion().getCliente().getDocumento(),
            f.getReservacion().getHabitacion().getNumero(),
            f.getReservacion().getHabitacion().getPiso(),
            f.getReservacion().getHabitacion().getTipo().getNombre(),
            f.getReservacion().getFechaCheckin(),
            f.getReservacion().getFechaCheckout(),
            f.getReservacion().getNoches(),
            f.getSubtotal(), f.getImpuesto(), f.getTotal(),
            f.getMetodoPago(), f.getEstado(),
            f.getFechaEmision() != null ? f.getFechaEmision().toString().substring(0, 16) : "-"
        );

        JTextArea area = new JTextArea(detalle);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(250, 252, 255));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Detalle de Factura #" + f.getId(), JOptionPane.PLAIN_MESSAGE);
    }

    private void cambiarEstado(String nuevoEstado) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int filaM = tabla.convertRowIndexToModel(fila);
        int id    = (int)    modeloTabla.getValueAt(filaM, 0);
        String est = (String) modeloTabla.getValueAt(filaM, 8);

        if (est.equals(nuevoEstado)) {
            JOptionPane.showMessageDialog(this, "La factura ya está en estado " + nuevoEstado + ".");
            return;
        }

        String accion = nuevoEstado.equals(Factura.ESTADO_PAGADA) ? "marcar como PAGADA" : "ANULAR";
        int op = JOptionPane.showConfirmDialog(this,
            "¿Deseas " + accion + " la Factura #" + id + "?",
            "Confirmar", JOptionPane.YES_NO_OPTION);

        if (op == JOptionPane.YES_OPTION) {
            if (facturaDAO.actualizarEstado(id, nuevoEstado)) {
                BitacoraService.log(Bitacora.ACCION_CAMBIAR_ESTADO,
                    Bitacora.MODULO_FACTURAS,
                    "Factura #" + id + " → " + nuevoEstado +
                    " (antes: " + est + ")");
                cargarFacturas();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton crearBoton(String texto, Color color, boolean deshabilitado) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setEnabled(!deshabilitado);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(color.darker());
            }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(color); }
        });
        return b;
    }
}
