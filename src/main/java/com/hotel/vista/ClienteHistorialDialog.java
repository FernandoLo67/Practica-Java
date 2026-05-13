package com.hotel.vista;

import com.hotel.util.ConexionDB;
import com.hotel.util.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diálogo que muestra el historial completo de reservaciones y facturas
 * de un cliente específico.
 *
 * Consulta directamente las tablas reservaciones, habitaciones,
 * tipo_habitacion y facturas para construir la vista.
 *
 * @author Fernando
 * @version 1.0
 */
public class ClienteHistorialDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(ClienteHistorialDialog.class);

    // =========================================================
    // COLUMNAS
    // =========================================================
    private static final String[] COLS_RESERVACIONES = {
        "ID Res.", "Habitación", "Tipo", "Check-In", "Check-Out",
        "Noches", "Estado Res.", "Factura", "Total", "Estado Pago"
    };

    // =========================================================
    // CONSULTA
    // =========================================================
    private static final String SQL =
        "SELECT r.id                                          AS id_res, " +
        "       h.numero                                      AS habitacion, " +
        "       t.nombre                                      AS tipo, " +
        "       r.fecha_checkin, " +
        "       r.fecha_checkout, " +
        "       DATEDIFF(r.fecha_checkout, r.fecha_checkin)  AS noches, " +
        "       r.estado                                      AS estado_res, " +
        "       f.id                                          AS id_factura, " +
        "       f.total, " +
        "       f.estado                                      AS estado_pago " +
        "FROM reservaciones r " +
        "INNER JOIN habitaciones   h ON r.id_habitacion = h.id " +
        "INNER JOIN tipo_habitacion t ON h.id_tipo       = t.id " +
        "LEFT  JOIN facturas        f ON f.id_reservacion = r.id " +
        "WHERE r.id_cliente = ? " +
        "ORDER BY r.fecha_checkin DESC";

    // =========================================================
    // COLORES DE ESTADO
    // =========================================================
    private static final Color C_VERDE    = new Color(21,  128,  61);
    private static final Color C_AZUL     = new Color(29,  78, 216);
    private static final Color C_AMBAR    = new Color(146,  64,  14);
    private static final Color C_ROJO     = new Color(185,  28,  28);
    private static final Color C_GRIS     = new Color(100, 100, 110);

    // =========================================================
    // ESTADO
    // =========================================================
    private final int    clienteId;
    private final String clienteNombre;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ClienteHistorialDialog(Frame padre, int clienteId, String clienteNombre) {
        super(padre, true);
        this.clienteId     = clienteId;
        this.clienteNombre = clienteNombre;
        setTitle("Historial — " + clienteNombre);
        setSize(920, 480);
        setLocationRelativeTo(padre);
        setResizable(true);
        initComponents();
        cargarHistorial();
    }

    // =========================================================
    // UI
    // =========================================================

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Tema.COLOR_FONDO);
        add(crearHeader(),  BorderLayout.NORTH);
        add(crearTabla(),   BorderLayout.CENTER);
        add(crearFooter(),  BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_PRIMARIO);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lbl = new JLabel("📋  Historial de reservaciones — " + clienteNombre);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST);

        JLabel sub = new JLabel("ID cliente: " + clienteId);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(200, 210, 240));
        p.add(sub, BorderLayout.EAST);
        return p;
    }

    private JScrollPane crearTabla() {
        DefaultTableModel modelo = new DefaultTableModel(COLS_RESERVACIONES, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable tabla = new JTable(modelo);
        tabla.setName("historial");
        tabla.putClientProperty("modelo", modelo);  // guardar referencia para cargarHistorial
        UIHelper_mini(tabla);

        // Anchos de columna
        int[] anchos = {60, 80, 100, 90, 90, 60, 110, 75, 90, 100};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Renderer de color para estados
        DefaultTableCellRenderer estadoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                if (!sel) {
                    String v = val != null ? val.toString() : "";
                    switch (v) {
                        case "CONFIRMADA": case "PAGADA":   setForeground(C_VERDE);  break;
                        case "CHECKIN":                     setForeground(C_AZUL);   break;
                        case "PENDIENTE":                   setForeground(C_AMBAR);  break;
                        case "CANCELADA": case "ANULADA":  setForeground(C_ROJO);   break;
                        case "CHECKOUT":                    setForeground(C_GRIS);   break;
                        default:                            setForeground(C_GRIS);   break;
                    }
                } else {
                    setForeground(Color.WHITE);
                }
                return this;
            }
        };
        tabla.getColumnModel().getColumn(6).setCellRenderer(estadoRenderer); // estado reservacion
        tabla.getColumnModel().getColumn(9).setCellRenderer(estadoRenderer); // estado pago

        // Centrar columnas numéricas
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c : new int[]{0, 1, 5, 7}) {
            tabla.getColumnModel().getColumn(c).setCellRenderer(centrado);
        }

        // Guardar modelo para cargarHistorial
        this.tablaHistorial = tabla;
        this.modeloHistorial = modelo;

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.COLOR_BORDE));
        return scroll;
    }

    private JPanel crearFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)));

        lblResumen = new JLabel("Cargando...");
        lblResumen.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblResumen.setForeground(new Color(80, 80, 100));

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCerrar.addActionListener(e -> dispose());

        p.add(lblResumen);
        p.add(Box.createHorizontalStrut(20));
        p.add(btnCerrar);
        return p;
    }

    // =========================================================
    // DATOS
    // =========================================================

    private JTable             tablaHistorial;
    private DefaultTableModel  modeloHistorial;
    private JLabel             lblResumen;

    private void cargarHistorial() {
        modeloHistorial.setRowCount(0);
        int count = 0;
        double totalAcumulado = 0.0;

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int    idRes       = rs.getInt("id_res");
                    String hab         = rs.getString("habitacion");
                    String tipo        = rs.getString("tipo");
                    String checkin     = rs.getString("fecha_checkin");
                    String checkout    = rs.getString("fecha_checkout");
                    int    noches      = rs.getInt("noches");
                    String estadoRes   = rs.getString("estado_res");
                    Object idFact      = rs.getObject("id_factura");
                    Object totalObj    = rs.getObject("total");
                    String estadoPago  = rs.getString("estado_pago");

                    String totalStr = (totalObj != null)
                        ? String.format("Q %.2f", ((Number) totalObj).doubleValue()) : "—";
                    if (totalObj != null)
                        totalAcumulado += ((Number) totalObj).doubleValue();

                    modeloHistorial.addRow(new Object[]{
                        idRes,
                        hab,
                        tipo,
                        checkin,
                        checkout,
                        noches,
                        estadoRes,
                        (idFact != null ? idFact : "—"),
                        totalStr,
                        (estadoPago != null ? estadoPago : "—")
                    });
                    count++;
                }
            }
        } catch (SQLException e) {
            log.error("Error cargando historial del cliente {}", clienteId, e);
            JOptionPane.showMessageDialog(this,
                "Error al cargar el historial: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        String resumen = count == 0
            ? "Sin reservaciones registradas."
            : count + " reservación(es)  |  Total facturado: Q " + String.format("%.2f", totalAcumulado);
        lblResumen.setText(resumen);
    }

    // =========================================================
    // MINI ESTILIZADOR de tabla (inline para no depender de UIHelper)
    // =========================================================

    private static void UIHelper_mini(JTable t) {
        t.setRowHeight(28);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(Tema.COLOR_PRIMARIO);
        t.getTableHeader().setForeground(Color.WHITE);
        t.setSelectionBackground(new Color(197, 210, 250));
        t.setSelectionForeground(Color.BLACK);
        t.setGridColor(new Color(220, 225, 240));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
    }
}
