package com.hotel.vista;

import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.util.ConexionDB;
import com.hotel.util.HotelConfig;
import com.hotel.util.Tema;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.dao.impl.FacturaDAOImpl;
import com.hotel.modelo.*;
import com.hotel.util.BitacoraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Panel de Check-In y Check-Out.
 *
 * CHECK-IN:  Reservaciones CONFIRMADAS → pasan a CHECKIN, habitación a OCUPADA.
 * CHECK-OUT: Reservaciones en CHECKIN  → pasan a CHECKOUT, habitación a DISPONIBLE,
 *            se genera la factura automáticamente.
 *
 * CAMBIOS v1.2:
 *   - [C-01] IVA tomado de HotelConfig — texto "IVA (X%)" dinámico.
 *   - [C-02] Checkout ejecuta las 3 operaciones SQL dentro de una única
 *            transacción con commit/rollback automático ante fallo.
 *
 * @author Fernando
 * @version 1.2
 */
public class CheckInOutPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(CheckInOutPanel.class);

    private JTable            tablaCheckin;
    private JTable            tablaCheckout;
    private DefaultTableModel modeloCheckin;
    private DefaultTableModel modeloCheckout;
    private JLabel            lblEstado;

    private final ReservacionDAOImpl reservacionDAO;
    private final HabitacionDAOImpl  habitacionDAO;
    private final FacturaDAOImpl     facturaDAO;
    private final Frame              ventanaPadre;
    private final Usuario            usuarioActual;

    // Colores delegados a Tema.java
    private final Color COLOR_PRIMARIO   = com.hotel.util.Tema.COLOR_PRIMARIO;
    private final Color COLOR_FONDO      = com.hotel.util.Tema.COLOR_FONDO;
    private final Color COLOR_CHECKIN    = com.hotel.util.Tema.COLOR_EXITO;
    private final Color COLOR_CHECKOUT   = com.hotel.util.Tema.COLOR_ERROR;
    private final Color COLOR_HEADER     = com.hotel.util.Tema.COLOR_HEADER_TABLA;
    private final Color COLOR_FILA_PAR   = com.hotel.util.Tema.COLOR_FILA_PAR;
    private final Color COLOR_FILA_IMPAR = com.hotel.util.Tema.COLOR_FILA_IMPAR;

    private static final String[] COLS = {
        "ID", "Cliente", "Habitación", "Tipo", "Check-In", "Check-Out", "Noches", "Total (Q)"
    };

    // =========================================================
    // SQL para la transacción atómica de checkout (C-02)
    // =========================================================
    private static final String SQL_CHECKOUT_RESERVACION =
        "UPDATE reservaciones SET estado = 'CHECKOUT' WHERE id = ?";
    private static final String SQL_CHECKOUT_HABITACION =
        "UPDATE habitaciones SET estado = 'DISPONIBLE' WHERE id = ?";
    private static final String SQL_CHECKOUT_FACTURA =
        "INSERT INTO facturas (id_reservacion, subtotal, impuesto, total, estado, metodo_pago) " +
        "VALUES (?, ?, ?, ?, 'PAGADA', ?)";

    public CheckInOutPanel(Frame ventanaPadre, Usuario usuarioActual) {
        this.ventanaPadre  = ventanaPadre;
        this.usuarioActual = usuarioActual;
        this.reservacionDAO = new ReservacionDAOImpl();
        this.habitacionDAO  = new HabitacionDAOImpl();
        this.facturaDAO     = new FacturaDAOImpl();
        setLayout(new BorderLayout());
        setBackground(COLOR_FONDO);
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearCuerpo(),     BorderLayout.CENTER);
        add(crearBarra(),      BorderLayout.SOUTH);
    }

    private JPanel crearEncabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_BLANCO);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));
        JLabel lbl = new JLabel("✅  Check-In / Check-Out");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(COLOR_PRIMARIO);

        JButton btnRefresh = crearBoton("🔄 Actualizar", new Color(100, 116, 139));
        btnRefresh.addActionListener(e -> cargarDatos());

        p.add(lbl,        BorderLayout.WEST);
        p.add(btnRefresh, BorderLayout.EAST);
        return p;
    }

    private JSplitPane crearCuerpo() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            crearSeccion("🟢  Pendientes de CHECK-IN",
                "Reservaciones confirmadas — haz clic en Registrar Check-In",
                COLOR_CHECKIN, true),
            crearSeccion("🔴  Pendientes de CHECK-OUT",
                "Huéspedes en el hotel — haz clic en Registrar Check-Out",
                COLOR_CHECKOUT, false)
        );
        split.setDividerLocation(0.5);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setBackground(COLOR_FONDO);
        return split;
    }

    private JPanel crearSeccion(String titulo, String subtitulo, Color color, boolean esCheckin) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Header de sección
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(color);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(220, 240, 220));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblSub);
        header.add(textos, BorderLayout.CENTER);

        // Tabla
        DefaultTableModel modelo = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(30);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(230, 233, 245));
        tabla.setSelectionBackground(new Color(197, 210, 255));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(COLOR_HEADER);
        tabla.getTableHeader().setForeground(COLOR_PRIMARIO);
        tabla.setRowSorter(new TableRowSorter<>(modelo));

        // Ocultar columna ID
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 140, 80, 90, 85, 85, 60, 85};
        for (int i = 0; i < anchos.length; i++)
            if (anchos[i] > 0)
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!sel) setBackground(row % 2 == 0 ? COLOR_FILA_PAR : COLOR_FILA_IMPAR);
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Botón acción
        String textoBtn = esCheckin ? "✅  Registrar Check-In" : "🏁  Registrar Check-Out";
        JButton btnAccion = crearBoton(textoBtn, color);
        btnAccion.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAccion.setEnabled(false);
        btnAccion.setPreferredSize(new Dimension(0, 46));

        tabla.getSelectionModel().addListSelectionListener(e ->
            btnAccion.setEnabled(tabla.getSelectedRow() >= 0)
        );

        if (esCheckin) {
            modeloCheckin = modelo;
            tablaCheckin  = tabla;
            btnAccion.addActionListener(e -> hacerCheckin());
        } else {
            modeloCheckout = modelo;
            tablaCheckout  = tabla;
            btnAccion.addActionListener(e -> hacerCheckout());
        }

        panel.add(header,    BorderLayout.NORTH);
        panel.add(scroll,    BorderLayout.CENTER);
        panel.add(btnAccion, BorderLayout.SOUTH);

        return panel;
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

    // =========================================================
    // DATOS
    // =========================================================

    public void cargarDatos() {
        cargarTabla(modeloCheckin,  "CONFIRMADA");
        cargarTabla(modeloCheckout, "CHECKIN");
        lblEstado.setText("Datos actualizados correctamente");
    }

    private void cargarTabla(DefaultTableModel modelo, String estado) {
        modelo.setRowCount(0);
        List<Reservacion> lista = reservacionDAO.listarTodas();
        for (Reservacion r : lista) {
            if (!r.getEstado().equals(estado)) continue;
            modelo.addRow(new Object[]{
                r.getId(),
                r.getCliente().getNombreCompleto(),
                "Hab. " + r.getHabitacion().getNumero(),
                r.getHabitacion().getTipo().getNombre(),
                r.getFechaCheckin().toString(),
                r.getFechaCheckout().toString(),
                r.getNoches() + " noche(s)",
                String.format("Q %.2f", r.getTotalSinImpuesto())
            });
        }
    }

    // =========================================================
    // CHECK-IN
    // =========================================================

    private void hacerCheckin() {
        int fila = tablaCheckin.getSelectedRow();
        if (fila < 0) return;

        int id = (int) modeloCheckin.getValueAt(
            tablaCheckin.convertRowIndexToModel(fila), 0);
        Reservacion r = reservacionDAO.buscarPorId(id);
        if (r == null) return;

        String cliente = r.getCliente().getNombreCompleto();
        String hab     = r.getHabitacion().getNumero();

        int op = JOptionPane.showConfirmDialog(this,
            "¿Confirmar Check-In de:\n\n" +
            "  Cliente:     " + cliente + "\n" +
            "  Habitación:  " + hab + "\n" +
            "  Check-Out:   " + r.getFechaCheckout() + "\n",
            "Confirmar Check-In", JOptionPane.YES_NO_OPTION);

        if (op == JOptionPane.YES_OPTION) {
            reservacionDAO.cambiarEstado(id, Reservacion.ESTADO_CHECKIN);
            habitacionDAO.cambiarEstado(r.getHabitacion().getId(), "OCUPADA");
            BitacoraService.log(usuarioActual, Bitacora.ACCION_CHECKIN,
                Bitacora.MODULO_CHECKINOUT,
                "Check-In: " + cliente + " — Hab. " + hab +
                " — Checkout: " + r.getFechaCheckout());
            JOptionPane.showMessageDialog(this,
                "✅  Check-In registrado para " + cliente +
                ".\nHabitación " + hab + " marcada como OCUPADA.",
                "Check-In Exitoso", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        }
    }

    // =========================================================
    // CHECK-OUT  [C-02] — transacción atómica
    // =========================================================

    private void hacerCheckout() {
        int fila = tablaCheckout.getSelectedRow();
        if (fila < 0) return;

        int id = (int) modeloCheckout.getValueAt(
            tablaCheckout.convertRowIndexToModel(fila), 0);
        Reservacion r = reservacionDAO.buscarPorId(id);
        if (r == null) return;

        // [C-01] Total con IVA configurado (no hardcoded)
        double subtotal = r.getTotalSinImpuesto();
        double impuesto = r.getImpuesto();
        double total    = r.getTotalConImpuesto();
        int    pctIva   = (int) Math.round(HotelConfig.getIva() * 100);

        // Selección del método de pago
        String[] metodos = {"EFECTIVO", "TARJETA", "TRANSFERENCIA"};
        String metodo = (String) JOptionPane.showInputDialog(this,
            "Cliente:     " + r.getCliente().getNombreCompleto() + "\n" +
            "Habitación:  " + r.getHabitacion().getNumero() + "\n" +
            "Noches:      " + r.getNoches() + "\n" +
            "Subtotal:    Q " + String.format("%.2f", subtotal) + "\n" +
            "IVA (" + pctIva + "%): Q " + String.format("%.2f", impuesto) + "\n" +
            "TOTAL:       Q " + String.format("%.2f", total) + "\n\n" +
            "Selecciona el método de pago:",
            "Registrar Check-Out", JOptionPane.PLAIN_MESSAGE,
            null, metodos, metodos[0]);

        if (metodo == null) return; // usuario canceló

        // [C-02] Ejecutar las 3 operaciones en una sola transacción
        Factura facturaGenerada = ejecutarCheckoutAtomico(r, metodo, subtotal, impuesto, total);

        if (facturaGenerada == null) {
            // El método interno ya mostró el error
            return;
        }

        // Auditoría (fuera de la transacción — no crítica para atomicidad)
        BitacoraService.log(usuarioActual, Bitacora.ACCION_CHECKOUT,
            Bitacora.MODULO_CHECKINOUT,
            "Check-Out: " + r.getCliente().getNombreCompleto() +
            " — Hab. " + r.getHabitacion().getNumero() +
            " — Total: Q" + String.format("%.2f", facturaGenerada.getTotal()) +
            " — Pago: " + metodo);

        // [A-06] Texto IVA dinámico
        JOptionPane.showMessageDialog(this,
            "🏁  Check-Out registrado.\n\n" +
            "  Cliente:        " + r.getCliente().getNombreCompleto() + "\n" +
            "  Habitación:     " + r.getHabitacion().getNumero() + " → DISPONIBLE\n" +
            "  Subtotal:       Q " + String.format("%.2f", facturaGenerada.getSubtotal()) + "\n" +
            "  IVA (" + pctIva + "%):     Q " + String.format("%.2f", facturaGenerada.getImpuesto()) + "\n" +
            "  TOTAL:          Q " + String.format("%.2f", facturaGenerada.getTotal()) + "\n" +
            "  Método de pago: " + metodo + "\n\n" +
            "  Factura #" + facturaGenerada.getId() + " generada correctamente.",
            "Check-Out Exitoso", JOptionPane.INFORMATION_MESSAGE);

        cargarDatos();
    }

    /**
     * Ejecuta el checkout en una transacción SQL única.
     *
     * Las tres operaciones (cambiar reservación → liberar habitación → crear factura)
     * se ejecutan sobre la misma conexión con autoCommit=false.
     * Si cualquiera falla, se hace rollback automático y se devuelve null.
     *
     * @return Factura generada con ID asignado, o null si falló
     */
    private Factura ejecutarCheckoutAtomico(
            Reservacion r, String metodo,
            double subtotal, double impuesto, double total) {

        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false); // iniciar transacción

            try {
                // 1. Reservación → CHECKOUT
                try (PreparedStatement ps = conn.prepareStatement(SQL_CHECKOUT_RESERVACION)) {
                    ps.setInt(1, r.getId());
                    ps.executeUpdate();
                }

                // 2. Habitación → DISPONIBLE
                try (PreparedStatement ps = conn.prepareStatement(SQL_CHECKOUT_HABITACION)) {
                    ps.setInt(1, r.getHabitacion().getId());
                    ps.executeUpdate();
                }

                // 3. Crear factura y recuperar ID generado
                Factura factura = new Factura(r, metodo);
                try (PreparedStatement ps = conn.prepareStatement(
                        SQL_CHECKOUT_FACTURA, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt   (1, r.getId());
                    ps.setDouble(2, subtotal);
                    ps.setDouble(3, impuesto);
                    ps.setDouble(4, total);
                    ps.setString(5, metodo);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) factura.setId(keys.getInt(1));
                    }
                }

                conn.commit(); // todo OK — confirmar
                log.info("Checkout atómico completado: reservación #{}, factura #{}",
                    r.getId(), factura.getId());
                return factura;

            } catch (SQLException ex) {
                // Revertir TODO si cualquier paso falló
                try { conn.rollback(); } catch (SQLException rb) {
                    log.error("Error al hacer rollback en checkout", rb);
                }
                log.error("Fallo en checkout atómico — reservación #{}", r.getId(), ex);
                JOptionPane.showMessageDialog(this,
                    "⚠  No se pudo completar el Check-Out.\n\n" +
                    "Ningún dato fue modificado (rollback aplicado).\n" +
                    "Error: " + ex.getMessage(),
                    "Error en Check-Out", JOptionPane.ERROR_MESSAGE);
                return null;
            }

        } catch (SQLException connEx) {
            log.error("Sin conexión disponible para checkout — reservación #{}", r.getId(), connEx);
            JOptionPane.showMessageDialog(this,
                "⚠  No hay conexión disponible con la base de datos.\n" +
                "Verifica que MySQL esté corriendo.",
                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private JButton crearBoton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(color.darker());
            }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(color); }
        });
        return b;
    }
}
