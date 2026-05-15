package com.hotel.vista;

import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.modelo.Habitacion;
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
 * Panel principal del módulo de Gestión de Habitaciones.
 *
 * FUNCIONALIDADES:
 *   - Tabla con todas las habitaciones y su estado
 *   - Tarjetas resumen (Disponibles / Ocupadas / Mantenimiento)
 *   - Búsqueda en tiempo real
 *   - Filtro rápido por estado
 *   - Doble clic para editar estado y descripción
 *
 * @author Fernando
 * @version 1.0
 */
public class HabitacionesPanel extends JPanel {

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTable             tabla;
    private DefaultTableModel  modeloTabla;
    private JTextField         txtBuscar;
    private JLabel             lblEstado;
    private JButton            btnEditar;
    private JButton            btnActualizar;
    private JComboBox<String>  cmbFiltro;

    // Tarjetas de conteo por estado
    private JLabel lblTotalDisponibles;
    private JLabel lblTotalOcupadas;
    private JLabel lblTotalMantenimiento;
    private JLabel lblTotalHabitaciones;

    // Tarjetas de precio
    private JLabel lblPrecioMin;
    private JLabel lblPrecioMax;
    private JLabel lblPrecioProm;

    private final HabitacionDAOImpl habitacionDAO;
    private final Frame             ventanaPadre;

    // =========================================================
    // COLORES
    // =========================================================
    // Colores delegados a Tema.java (punto único de verdad)
    private final Color COLOR_PRIMARIO      = com.hotel.util.Tema.COLOR_PRIMARIO;
    private final Color COLOR_FONDO         = com.hotel.util.Tema.COLOR_FONDO;
    private final Color COLOR_HEADER        = com.hotel.util.Tema.COLOR_HEADER_TABLA;
    private final Color COLOR_DISPONIBLE    = com.hotel.util.Tema.COLOR_DISPONIBLE;
    private final Color COLOR_OCUPADA       = com.hotel.util.Tema.COLOR_OCUPADA;
    private final Color COLOR_RESERVADA     = com.hotel.util.Tema.COLOR_RESERVADA;
    private final Color COLOR_MANTENIMIENTO = com.hotel.util.Tema.COLOR_MANTENIMIENTO;
    private final Color COLOR_FILA_PAR      = com.hotel.util.Tema.COLOR_FILA_PAR;
    private final Color COLOR_FILA_IMPAR    = com.hotel.util.Tema.COLOR_FILA_IMPAR;

    private static final String[] COLUMNAS = {
        "ID", "N°", "Piso", "Tipo", "Estado", "Precio/Noche", "Capacidad", "Descripción"
    };

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public HabitacionesPanel(Frame ventanaPadre) {
        this.ventanaPadre  = ventanaPadre;
        this.habitacionDAO = new HabitacionDAOImpl();
        setLayout(new BorderLayout(0, 0));
        setBackground(COLOR_FONDO);
        initComponents();
        cargarHabitaciones();
        registrarAtajos();
    }

    /** F5 = Actualizar, Ctrl+E = Editar habitación seleccionada */
    private void registrarAtajos() {
        javax.swing.InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "actualizar");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E,
            java.awt.event.InputEvent.CTRL_DOWN_MASK), "editar");

        am.put("actualizar", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cargarHabitaciones(); }
        });
        am.put("editar", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnEditar.isEnabled()) abrirEditar();
            }
        });
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA INTERFAZ
    // =========================================================

    private void initComponents() {
        add(crearEncabezado(),   BorderLayout.NORTH);
        add(crearCuerpo(),       BorderLayout.CENTER);
        add(crearBarraEstado(),  BorderLayout.SOUTH);
    }

    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblTitulo = new JLabel("🛏  Gestión de Habitaciones");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(COLOR_PRIMARIO);

        // Buscador + filtro
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelFiltros.setOpaque(false);

        txtBuscar = new JTextField(18);
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscar.setPreferredSize(new Dimension(220, 34));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 205, 225), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        txtBuscar.setToolTipText("Buscar por número, tipo, estado...");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        cmbFiltro = new JComboBox<>(new String[]{
            "Todos", "DISPONIBLE", "OCUPADA", "RESERVADA", "MANTENIMIENTO"
        });
        cmbFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbFiltro.setPreferredSize(new Dimension(150, 34));
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
        btnEditar     = crearBoton("✏️ Editar",     COLOR_PRIMARIO,            true);

        btnActualizar.addActionListener(e -> cargarHabitaciones());
        btnEditar.addActionListener(e -> abrirEditar());

        JButton btnExcel = crearBoton("📊 Excel", new Color(46, 125, 50), false);
        btnExcel.addActionListener(e ->
            ExcelExporter.exportar(tabla, "Habitaciones", ventanaPadre));

        panelBotones.add(btnActualizar);
        panelBotones.add(btnExcel);
        panelBotones.add(btnEditar);

        panel.add(lblTitulo,    BorderLayout.WEST);
        panel.add(panelFiltros, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }

    /** Panel central: tarjetas resumen arriba + tabla abajo */
    private JPanel crearCuerpo() {
        JPanel cuerpo = new JPanel(new BorderLayout(0, 0));
        cuerpo.setBackground(COLOR_FONDO);

        JPanel kpis = new JPanel(new BorderLayout());
        kpis.setOpaque(false);
        kpis.add(crearTarjetasResumen(), BorderLayout.NORTH);
        kpis.add(crearTarjetasPrecio(),  BorderLayout.SOUTH);

        cuerpo.add(kpis,         BorderLayout.NORTH);
        cuerpo.add(crearTabla(), BorderLayout.CENTER);
        return cuerpo;
    }

    /** Cuatro tarjetas con contadores por estado */
    private JPanel crearTarjetasResumen() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 14));
        panel.setBackground(COLOR_FONDO);

        lblTotalHabitaciones  = new JLabel("0");
        lblTotalDisponibles   = new JLabel("0");
        lblTotalOcupadas      = new JLabel("0");
        lblTotalMantenimiento = new JLabel("0");

        panel.add(crearTarjeta("🏨", "Total",        lblTotalHabitaciones,  new Color(63, 81, 181)));
        panel.add(crearTarjeta("✅", "Disponibles",  lblTotalDisponibles,   COLOR_DISPONIBLE));
        panel.add(crearTarjeta("🔴", "Ocupadas",     lblTotalOcupadas,      COLOR_OCUPADA));
        panel.add(crearTarjeta("🔧", "Mantenimiento",lblTotalMantenimiento, COLOR_MANTENIMIENTO));

        return panel;
    }

    /** Tres tarjetas con estadísticas de precio */
    private JPanel crearTarjetasPrecio() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        lblPrecioMin  = new JLabel("Q —");
        lblPrecioProm = new JLabel("Q —");
        lblPrecioMax  = new JLabel("Q —");

        panel.add(crearTarjeta("⬇", "Precio mínimo",  lblPrecioMin,  new Color(21, 128, 61)));
        panel.add(crearTarjeta("〜", "Precio promedio", lblPrecioProm, new Color(107, 33, 168)));
        panel.add(crearTarjeta("⬆", "Precio máximo",  lblPrecioMax,  new Color(146, 64, 14)));

        return panel;
    }

    private JPanel crearTarjeta(String icono, String titulo, JLabel lblNumero, Color color) {
        JPanel t = new JPanel(new GridBagLayout());
        t.setBackground(Color.WHITE);
        t.setPreferredSize(new Dimension(155, 80));
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;

        JLabel lblIconoTitulo = new JLabel(icono + "  " + titulo);
        lblIconoTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblIconoTitulo.setForeground(new Color(100, 110, 140));
        t.add(lblIconoTitulo, g);

        g.gridy = 1;
        lblNumero.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblNumero.setForeground(color);
        t.add(lblNumero, g);

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

        // Ocultar columna ID
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        // Anchos de columnas
        int[] anchos = {0, 60, 55, 100, 120, 110, 85, 200};
        for (int i = 0; i < anchos.length; i++) {
            if (anchos[i] > 0)
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Renderer con colores por estado y filas alternadas
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? COLOR_FILA_PAR : COLOR_FILA_IMPAR);
                    // Colorear la celda de estado
                    int colModelo = tabla.convertColumnIndexToModel(col);
                    if (colModelo == 4 && value != null) {
                        switch (value.toString()) {
                            case "DISPONIBLE":    setForeground(COLOR_DISPONIBLE);    break;
                            case "OCUPADA":       setForeground(COLOR_OCUPADA);       break;
                            case "RESERVADA":     setForeground(COLOR_RESERVADA);     break;
                            case "MANTENIMIENTO": setForeground(COLOR_MANTENIMIENTO); break;
                            default:              setForeground(Color.BLACK);
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

        // Doble clic para editar
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirEditar();
            }
        });

        // Habilitar botón editar al seleccionar fila
        tabla.getSelectionModel().addListSelectionListener(e -> {
            btnEditar.setEnabled(tabla.getSelectedRow() >= 0);
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
    // HELPERS DE ESTILO
    // =========================================================

    private JButton crearBoton(String texto, Color color, boolean deshabilitadoInicial) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setEnabled(!deshabilitadoInicial);
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
    // OPERACIONES DE DATOS
    // =========================================================

    public void cargarHabitaciones() {
        modeloTabla.setRowCount(0);
        txtBuscar.setText("");
        cmbFiltro.setSelectedIndex(0);

        List<Habitacion> lista = habitacionDAO.listarTodas();
        poblarTabla(lista);
        actualizarTarjetas();
        btnEditar.setEnabled(false);
    }

    private void filtrar() {
        String texto  = txtBuscar.getText().trim();
        String filtro = (String) cmbFiltro.getSelectedItem();

        List<Habitacion> lista = texto.isEmpty()
            ? habitacionDAO.listarTodas()
            : habitacionDAO.buscar(texto);

        // Filtro adicional por estado
        if (filtro != null && !filtro.equals("Todos")) {
            lista.removeIf(h -> !h.getEstado().equals(filtro));
        }

        modeloTabla.setRowCount(0);
        poblarTabla(lista);
    }

    private void poblarTabla(List<Habitacion> lista) {
        for (Habitacion h : lista) {
            modeloTabla.addRow(new Object[]{
                h.getId(),
                h.getNumero(),
                "Piso " + h.getPiso(),
                h.getTipo().getNombre(),
                h.getEstado(),
                "Q " + String.format("%.2f", h.getPrecioNoche()),
                h.getTipo().getCapacidad() + " persona(s)",
                h.getDescripcion() != null ? h.getDescripcion() : ""
            });
        }
        String texto = lista.size() == 1 ? "1 habitación" : lista.size() + " habitaciones";
        lblEstado.setText("🏨  " + texto);
    }

    private void actualizarTarjetas() {
        lblTotalHabitaciones .setText(String.valueOf(habitacionDAO.contarTodas()));
        lblTotalDisponibles  .setText(String.valueOf(habitacionDAO.contarPorEstado("DISPONIBLE")));
        lblTotalOcupadas     .setText(String.valueOf(habitacionDAO.contarPorEstado("OCUPADA")));
        lblTotalMantenimiento.setText(String.valueOf(habitacionDAO.contarPorEstado("MANTENIMIENTO")));

        lblPrecioMin .setText(String.format("Q %.0f", habitacionDAO.getPrecioMin()));
        lblPrecioProm.setText(String.format("Q %.0f", habitacionDAO.getPrecioProm()));
        lblPrecioMax .setText(String.format("Q %.0f", habitacionDAO.getPrecioMax()));
    }

    private void abrirEditar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona una habitación de la tabla.",
                "Sin selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(fila);
        int id = (int) modeloTabla.getValueAt(filaModelo, 0);

        Habitacion hab = habitacionDAO.buscarPorId(id);
        if (hab == null) {
            JOptionPane.showMessageDialog(this, "No se encontró la habitación.", "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        HabitacionFormDialog dialog = new HabitacionFormDialog(ventanaPadre, hab);
        dialog.setVisible(true);
        if (dialog.isGuardadoExitoso()) {
            cargarHabitaciones();
        }
    }
}
