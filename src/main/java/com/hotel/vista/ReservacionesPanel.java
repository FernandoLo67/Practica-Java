package com.hotel.vista;

import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.modelo.Reservacion;
import com.hotel.modelo.Usuario;
import com.hotel.util.ExcelExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel principal del módulo de Reservaciones.
 *
 * FUNCIONALIDADES:
 *   - Tabla con todas las reservaciones
 *   - Tarjetas resumen por estado
 *   - Búsqueda en tiempo real
 *   - Filtro por estado
 *   - Crear, editar y cancelar reservaciones
 *
 * @author Fernando
 * @version 1.0
 */
public class ReservacionesPanel extends JPanel {

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTable            tabla;
    private DefaultTableModel modeloTabla;
    private JTextField        txtBuscar;
    private JLabel            lblEstado;
    private JButton           btnNuevo;
    private JButton           btnEditar;
    private JButton           btnCancelar;
    private JButton           btnActualizar;
    private JComboBox<String> cmbFiltro;

    private JLabel lblPendientes;
    private JLabel lblConfirmadas;
    private JLabel lblCheckin;
    private JLabel lblTotal;

    private final ReservacionDAOImpl reservacionDAO;
    private final Frame              ventanaPadre;
    private final Usuario            usuarioActual;

    // =========================================================
    // COLORES
    // =========================================================
    // Colores delegados a Tema.java
    private final Color COLOR_PRIMARIO   = com.hotel.util.Tema.COLOR_PRIMARIO;
    private final Color COLOR_FONDO      = com.hotel.util.Tema.COLOR_FONDO;
    private final Color COLOR_HEADER     = com.hotel.util.Tema.COLOR_HEADER_TABLA;
    private final Color COLOR_PENDIENTE  = com.hotel.util.Tema.COLOR_PENDIENTE;
    private final Color COLOR_CONFIRMADA = com.hotel.util.Tema.COLOR_CONFIRMADA;
    private final Color COLOR_CHECKIN    = com.hotel.util.Tema.COLOR_CHECKIN;
    private final Color COLOR_CHECKOUT   = com.hotel.util.Tema.COLOR_CHECKOUT;
    private final Color COLOR_CANCELADA  = com.hotel.util.Tema.COLOR_CANCELADA;
    private final Color COLOR_FILA_PAR   = com.hotel.util.Tema.COLOR_FILA_PAR;
    private final Color COLOR_FILA_IMPAR = com.hotel.util.Tema.COLOR_FILA_IMPAR;

    private static final String[] COLUMNAS = {
        "ID", "Cliente", "Documento", "Habitación", "Tipo", "Check-In", "Check-Out", "Noches", "Total (Q)", "Estado"
    };

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReservacionesPanel(Frame ventanaPadre, Usuario usuarioActual) {
        this.ventanaPadre  = ventanaPadre;
        this.usuarioActual = usuarioActual;
        this.reservacionDAO = new ReservacionDAOImpl();
        setLayout(new BorderLayout(0, 0));
        setBackground(COLOR_FONDO);
        initComponents();
        cargarReservaciones();
        registrarAtajos();
    }

    /** Ctrl+N = Nueva, F5 = Actualizar, Ctrl+E = Editar */
    private void registrarAtajos() {
        javax.swing.InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N,
            java.awt.event.InputEvent.CTRL_DOWN_MASK), "nueva");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "actualizar");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E,
            java.awt.event.InputEvent.CTRL_DOWN_MASK), "editar");

        am.put("nueva",      new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { abrirFormNuevo(); }
        });
        am.put("actualizar", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cargarReservaciones(); }
        });
        am.put("editar",     new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnEditar.isEnabled()) abrirFormEditar();
            }
        });
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA INTERFAZ
    // =========================================================

    private void initComponents() {
        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCuerpo(),      BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);
    }

    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblTitulo = new JLabel("📅  Gestión de Reservaciones");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(COLOR_PRIMARIO);

        // Filtros
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelFiltros.setOpaque(false);

        txtBuscar = new JTextField(16);
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscar.setPreferredSize(new Dimension(200, 34));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 205, 225), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        txtBuscar.setToolTipText("Buscar por cliente, habitación o estado...");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        cmbFiltro = new JComboBox<>(new String[]{
            "Todos", "PENDIENTE", "CONFIRMADA", "CHECKIN", "CHECKOUT", "CANCELADA"
        });
        cmbFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbFiltro.setPreferredSize(new Dimension(145, 34));
        cmbFiltro.setBackground(Color.WHITE);
        cmbFiltro.addActionListener(e -> filtrar());

        panelFiltros.add(new JLabel("🔍"));
        panelFiltros.add(txtBuscar);
        panelFiltros.add(new JLabel("Estado:"));
        panelFiltros.add(cmbFiltro);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotones.setOpaque(false);

        btnActualizar = crearBoton("🔄 Actualizar", new Color(100, 116, 139), false);
        btnNuevo      = crearBoton("➕ Nueva",       new Color(46, 125, 50),   false);
        btnEditar     = crearBoton("✏️ Editar",      COLOR_PRIMARIO,            true);
        btnCancelar   = crearBoton("✖ Cancelar",    new Color(198, 40, 40),    true);

        btnActualizar.addActionListener(e -> cargarReservaciones());
        btnNuevo.addActionListener(e -> abrirFormNuevo());
        btnEditar.addActionListener(e -> abrirFormEditar());
        btnCancelar.addActionListener(e -> cancelarReservacion());

        JButton btnExcel = crearBoton("📊 Excel", new Color(46, 125, 50), false);
        btnExcel.addActionListener(e ->
            ExcelExporter.exportar(tabla, "Reservaciones", ventanaPadre));

        panelBotones.add(btnActualizar);
        panelBotones.add(btnExcel);
        panelBotones.add(btnNuevo);
        panelBotones.add(btnEditar);
        panelBotones.add(btnCancelar);

        panel.add(lblTitulo,    BorderLayout.WEST);
        panel.add(panelFiltros, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
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

        lblTotal       = new JLabel("0");
        lblPendientes  = new JLabel("0");
        lblConfirmadas = new JLabel("0");
        lblCheckin     = new JLabel("0");

        panel.add(tarjeta("📋", "Total",       lblTotal,       new Color(63, 81, 181)));
        panel.add(tarjeta("⏳", "Pendientes",  lblPendientes,  COLOR_PENDIENTE));
        panel.add(tarjeta("✅", "Confirmadas", lblConfirmadas, COLOR_CONFIRMADA));
        panel.add(tarjeta("🏠", "En Hotel",    lblCheckin,     COLOR_CHECKIN));

        return panel;
    }

    private JPanel tarjeta(String icono, String titulo, JLabel numero, Color color) {
        JPanel t = new JPanel(new GridBagLayout());
        t.setBackground(Color.WHITE);
        t.setPreferredSize(new Dimension(155, 80));
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
        numero.setFont(new Font("Segoe UI", Font.BOLD, 26));
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

        // Ocultar ID
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        // Anchos
        int[] anchos = {0, 150, 90, 80, 90, 90, 90, 60, 90, 100};
        for (int i = 0; i < anchos.length; i++) {
            if (anchos[i] > 0)
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Renderer con color por estado
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? COLOR_FILA_PAR : COLOR_FILA_IMPAR);
                    int cm = tabla.convertColumnIndexToModel(col);
                    if (cm == 9 && value != null) {
                        switch (value.toString()) {
                            case "PENDIENTE":  setForeground(COLOR_PENDIENTE);  break;
                            case "CONFIRMADA": setForeground(COLOR_CONFIRMADA); break;
                            case "CHECKIN":    setForeground(COLOR_CHECKIN);    break;
                            case "CHECKOUT":   setForeground(COLOR_CHECKOUT);   break;
                            case "CANCELADA":  setForeground(COLOR_CANCELADA);  break;
                            default:           setForeground(Color.BLACK);
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
                if (e.getClickCount() == 2) abrirFormEditar();
            }
        });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            boolean hay = tabla.getSelectedRow() >= 0;
            btnEditar.setEnabled(hay);
            btnCancelar.setEnabled(hay);
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel crearBarraEstado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 242, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)),
            new EmptyBorder(6, 16, 6, 16)
        ));
        lblEstado = new JLabel("Cargando...");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(90, 95, 120));

        JLabel lblAyuda = new JLabel("Doble clic para editar  |  Clic en encabezado para ordenar");
        lblAyuda.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAyuda.setForeground(new Color(150, 155, 175));

        panel.add(lblEstado, BorderLayout.WEST);
        panel.add(lblAyuda,  BorderLayout.EAST);
        return panel;
    }

    // =========================================================
    // HELPERS ESTILO
    // =========================================================

    private JButton crearBoton(String texto, Color color, boolean deshabilitado) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setEnabled(!deshabilitado);
        Color hover = color.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hover);
            }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(color); }
        });
        return btn;
    }

    // =========================================================
    // DATOS
    // =========================================================

    public void cargarReservaciones() {
        modeloTabla.setRowCount(0);
        txtBuscar.setText("");
        cmbFiltro.setSelectedIndex(0);

        List<Reservacion> lista = reservacionDAO.listarTodas();
        poblarTabla(lista);
        actualizarTarjetas();
        btnEditar.setEnabled(false);
        btnCancelar.setEnabled(false);
    }

    private void filtrar() {
        String texto  = txtBuscar.getText().trim();
        String filtro = (String) cmbFiltro.getSelectedItem();

        List<Reservacion> lista = texto.isEmpty()
            ? reservacionDAO.listarTodas()
            : reservacionDAO.buscar(texto);

        if (filtro != null && !filtro.equals("Todos"))
            lista.removeIf(r -> !r.getEstado().equals(filtro));

        modeloTabla.setRowCount(0);
        poblarTabla(lista);
    }

    private void poblarTabla(List<Reservacion> lista) {
        for (Reservacion r : lista) {
            modeloTabla.addRow(new Object[]{
                r.getId(),
                r.getCliente().getNombreCompleto(),
                r.getCliente().getDocumento(),
                "Hab. " + r.getHabitacion().getNumero(),
                r.getHabitacion().getTipo().getNombre(),
                r.getFechaCheckin().toString(),
                r.getFechaCheckout().toString(),
                r.getNoches() + " noche(s)",
                String.format("Q %.2f", r.getTotalSinImpuesto()),
                r.getEstado()
            });
        }
        String txt = lista.size() == 1 ? "1 reservación" : lista.size() + " reservaciones";
        lblEstado.setText("📋  " + txt);
    }

    private void actualizarTarjetas() {
        int total = reservacionDAO.contarPorEstado("PENDIENTE")
                  + reservacionDAO.contarPorEstado("CONFIRMADA")
                  + reservacionDAO.contarPorEstado("CHECKIN")
                  + reservacionDAO.contarPorEstado("CHECKOUT")
                  + reservacionDAO.contarPorEstado("CANCELADA");
        lblTotal      .setText(String.valueOf(total));
        lblPendientes .setText(String.valueOf(reservacionDAO.contarPorEstado("PENDIENTE")));
        lblConfirmadas.setText(String.valueOf(reservacionDAO.contarPorEstado("CONFIRMADA")));
        lblCheckin    .setText(String.valueOf(reservacionDAO.contarPorEstado("CHECKIN")));
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    private void abrirFormNuevo() {
        ReservacionFormDialog d = new ReservacionFormDialog(ventanaPadre, null, usuarioActual);
        d.setVisible(true);
        if (d.isGuardadoExitoso()) cargarReservaciones();
    }

    private void abrirFormEditar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una reservación.", "Sin selección",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        Reservacion r = reservacionDAO.buscarPorId(id);
        if (r == null) return;

        ReservacionFormDialog d = new ReservacionFormDialog(ventanaPadre, r, usuarioActual);
        d.setVisible(true);
        if (d.isGuardadoExitoso()) cargarReservaciones();
    }

    private void cancelarReservacion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;

        int filaModelo = tabla.convertRowIndexToModel(fila);
        int id     = (int)    modeloTabla.getValueAt(filaModelo, 0);
        String est = (String) modeloTabla.getValueAt(filaModelo, 9);

        if ("CANCELADA".equals(est) || "CHECKOUT".equals(est)) {
            JOptionPane.showMessageDialog(this,
                "Esta reservación ya no puede cancelarse.", "Aviso",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cliente = (String) modeloTabla.getValueAt(filaModelo, 1);
        int op = JOptionPane.showConfirmDialog(this,
            "¿Cancelar la reservación de:\n\n  " + cliente + "?\n\nEsta acción no se puede deshacer.",
            "Confirmar Cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (op == JOptionPane.YES_OPTION) {
            if (reservacionDAO.cambiarEstado(id, Reservacion.ESTADO_CANCELADA)) {
                JOptionPane.showMessageDialog(this, "Reservación cancelada.", "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
                cargarReservaciones();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo cancelar.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
