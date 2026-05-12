package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.modelo.Cliente;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel principal del módulo de Gestión de Clientes.
 *
 * FUNCIONALIDADES:
 *   - Tabla (JTable) con todos los clientes
 *   - Búsqueda en tiempo real por nombre, apellido, documento o email
 *   - Botón NUEVO    → abre ClienteFormDialog para registrar
 *   - Botón EDITAR   → abre ClienteFormDialog con datos precargados
 *   - Botón ELIMINAR → pide confirmación y elimina de la BD
 *   - Botón ACTUALIZAR → recarga los datos desde la BD
 *   - Barra de estado con el total de clientes
 *
 * CONCEPTO: JTable con DefaultTableModel
 *   - DefaultTableModel maneja los datos de la tabla
 *   - TableRowSorter permite ordenar columnas con un clic
 *   - Para obtener la fila seleccionada se usa: tabla.getSelectedRow()
 *
 * @author Fernando
 * @version 1.0
 */
public class ClientesPanel extends JPanel {

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTable              tabla;
    private DefaultTableModel   modeloTabla;
    private JTextField          txtBuscar;
    private JLabel              lblEstado;
    private JButton             btnNuevo;
    private JButton             btnEditar;
    private JButton             btnEliminar;
    private JButton             btnActualizar;

    // DAO para todas las operaciones con la BD
    private final ClienteDAOImpl clienteDAO;

    // Referencia a la ventana principal (necesaria para abrir diálogos modales)
    private final Frame ventanaPadre;

    // =========================================================
    // COLORES
    // =========================================================
    private static final Color COLOR_PRIMARIO  = new Color(26, 35, 126);
    private static final Color COLOR_FONDO     = new Color(243, 246, 253);
    private static final Color COLOR_HEADER    = new Color(232, 236, 255);
    private static final Color COLOR_FILA_PAR  = Color.WHITE;
    private static final Color COLOR_FILA_IMPAR = new Color(248, 250, 255);

    // Nombres de las columnas de la tabla
    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Apellido", "Tipo Doc.", "Documento", "Teléfono", "Email", "Nacionalidad"
    };

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ClientesPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.clienteDAO   = new ClienteDAOImpl();
        setLayout(new BorderLayout(0, 0));
        setBackground(COLOR_FONDO);
        initComponents();
        cargarClientes(); // Carga los datos al abrir el panel
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA INTERFAZ
    // =========================================================

    private void initComponents() {
        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearTabla(),       BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);
    }

    /**
     * Encabezado: título + buscador + botones de acción.
     */
    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        // --- Lado izquierdo: título ---
        JLabel lblTitulo = new JLabel("👥  Gestión de Clientes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(COLOR_PRIMARIO);

        // --- Centro: buscador ---
        JPanel panelBuscar = new JPanel(new BorderLayout(8, 0));
        panelBuscar.setOpaque(false);
        panelBuscar.setMaximumSize(new Dimension(320, 38));

        JLabel lblLupa = new JLabel("🔍");
        lblLupa.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscar.setPreferredSize(new Dimension(280, 36));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 205, 225), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        txtBuscar.setToolTipText("Buscar por nombre, apellido, documento o email");

        // Búsqueda en tiempo real: se ejecuta cada vez que el usuario escribe
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrarTabla(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrarTabla(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarTabla(); }
        });

        panelBuscar.add(lblLupa,    BorderLayout.WEST);
        panelBuscar.add(txtBuscar,  BorderLayout.CENTER);

        // --- Lado derecho: botones ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotones.setOpaque(false);

        btnActualizar = crearBoton("🔄 Actualizar", new Color(100, 116, 139), false);
        btnNuevo      = crearBoton("➕ Nuevo",      new Color(46, 125, 50),   false);
        btnEditar     = crearBoton("✏️ Editar",     COLOR_PRIMARIO,            true);
        btnEliminar   = crearBoton("🗑️ Eliminar",  new Color(198, 40, 40),    true);

        btnActualizar.addActionListener(e -> cargarClientes());
        btnNuevo.addActionListener(e -> abrirFormNuevo());
        btnEditar.addActionListener(e -> abrirFormEditar());
        btnEliminar.addActionListener(e -> eliminarCliente());

        panelBotones.add(btnActualizar);
        panelBotones.add(btnNuevo);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);

        panel.add(lblTitulo,    BorderLayout.WEST);
        panel.add(panelBuscar,  BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }

    /**
     * Crea el panel con la JTable y su scroll.
     */
    private JScrollPane crearTabla() {
        // Modelo de tabla: define columnas y hace las celdas NO editables
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // El usuario NO puede editar directamente en la tabla
            }
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

        // Encabezado de la tabla
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(COLOR_HEADER);
        tabla.getTableHeader().setForeground(COLOR_PRIMARIO);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Permitir ordenar columnas haciendo clic en el encabezado
        tabla.setRowSorter(new TableRowSorter<>(modeloTabla));

        // Ocultar la columna ID (posición 0) — se usa internamente pero no se muestra
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        // Anchos de columnas visibles
        int[] anchos = {0, 130, 130, 90, 110, 110, 170, 110};
        for (int i = 0; i < anchos.length; i++) {
            if (anchos[i] > 0) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Colores alternados en filas (par/impar)
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? COLOR_FILA_PAR : COLOR_FILA_IMPAR);
                }
                return this;
            }
        });

        // Doble clic en una fila → abrir formulario de edición
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirFormEditar();
            }
        });

        // Habilitar/deshabilitar botones según si hay fila seleccionada
        tabla.getSelectionModel().addListSelectionListener(e -> {
            boolean haySeleccion = tabla.getSelectedRow() >= 0;
            btnEditar.setEnabled(haySeleccion);
            btnEliminar.setEnabled(haySeleccion);
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    /**
     * Barra inferior con conteo de registros.
     */
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

        Color colorHover = color.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(colorHover);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    // =========================================================
    // OPERACIONES DE DATOS
    // =========================================================

    /**
     * Carga todos los clientes desde la BD y los muestra en la tabla.
     * Limpia primero la tabla y luego agrega cada cliente como una fila.
     */
    public void cargarClientes() {
        modeloTabla.setRowCount(0); // Limpiar tabla
        txtBuscar.setText("");

        List<Cliente> clientes = clienteDAO.listarTodos();

        for (Cliente c : clientes) {
            modeloTabla.addRow(new Object[]{
                c.getId(),
                c.getNombre(),
                c.getApellido(),
                c.getTipoDocumento(),
                c.getDocumento(),
                c.getTelefono() != null ? c.getTelefono() : "-",
                c.getEmail()    != null ? c.getEmail()    : "-",
                c.getNacionalidad() != null ? c.getNacionalidad() : "-"
            });
        }

        actualizarEstado(clientes.size());
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    /**
     * Filtra la tabla en tiempo real según lo que escribe el usuario.
     * Busca en la BD con el texto ingresado.
     */
    private void filtrarTabla() {
        String texto = txtBuscar.getText().trim();
        modeloTabla.setRowCount(0);

        List<Cliente> clientes = texto.isEmpty()
            ? clienteDAO.listarTodos()
            : clienteDAO.buscar(texto);

        for (Cliente c : clientes) {
            modeloTabla.addRow(new Object[]{
                c.getId(),
                c.getNombre(),
                c.getApellido(),
                c.getTipoDocumento(),
                c.getDocumento(),
                c.getTelefono() != null ? c.getTelefono() : "-",
                c.getEmail()    != null ? c.getEmail()    : "-",
                c.getNacionalidad() != null ? c.getNacionalidad() : "-"
            });
        }

        actualizarEstado(clientes.size());
    }

    /**
     * Abre el formulario para registrar un nuevo cliente.
     */
    private void abrirFormNuevo() {
        ClienteFormDialog dialog = new ClienteFormDialog(ventanaPadre, null);
        dialog.setVisible(true);
        if (dialog.isGuardadoExitoso()) {
            cargarClientes(); // Recargar tabla si se guardó
        }
    }

    /**
     * Abre el formulario con los datos del cliente seleccionado para editarlo.
     */
    private void abrirFormEditar() {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona un cliente de la tabla para editarlo.",
                "Sin selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Obtener ID de la fila seleccionada (columna 0, oculta)
        int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        int idCliente  = (int) modeloTabla.getValueAt(filaModelo, 0);

        // Buscar el objeto completo en la BD
        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el cliente.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ClienteFormDialog dialog = new ClienteFormDialog(ventanaPadre, cliente);
        dialog.setVisible(true);
        if (dialog.isGuardadoExitoso()) {
            cargarClientes();
        }
    }

    /**
     * Pide confirmación y elimina el cliente seleccionado.
     */
    private void eliminarCliente() {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) return;

        int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        int    idCliente  = (int) modeloTabla.getValueAt(filaModelo, 0);
        String nombre     = modeloTabla.getValueAt(filaModelo, 1) + " " +
                            modeloTabla.getValueAt(filaModelo, 2);

        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que deseas eliminar a:\n\n" +
            "  " + nombre + "\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            boolean eliminado = clienteDAO.eliminar(idCliente);
            if (eliminado) {
                JOptionPane.showMessageDialog(this,
                    "Cliente eliminado correctamente.", "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
                cargarClientes();
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar el cliente.\n" +
                    "Es posible que tenga reservaciones asociadas.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Actualiza el texto de la barra de estado con el total de registros.
     */
    private void actualizarEstado(int cantidad) {
        String texto = cantidad == 1
            ? "1 cliente encontrado"
            : cantidad + " clientes encontrados";
        lblEstado.setText("📋  " + texto);
    }
}
