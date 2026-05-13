package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.modelo.Cliente;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel principal del módulo de Gestión de Clientes.
 *
 * Refactorizado para usar Tema y UIHelper centralizados.
 *
 * @author Fernando
 * @version 2.0
 */
public class ClientesPanel extends JPanel {

    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Apellido", "Tipo Doc.", "Documento", "Teléfono", "Email", "Nacionalidad"
    };

    private JTable            tabla;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField        txtBuscar;
    private JLabel            lblEstado;
    private JButton           btnNuevo;
    private JButton           btnEditar;
    private JButton           btnEliminar;
    private JButton           btnActualizar;

    private final ClienteDAOImpl clienteDAO;
    private final Frame          ventanaPadre;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ClientesPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.clienteDAO   = new ClienteDAOImpl();
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);
        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCentro(),      BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);
        cargarClientes();
    }

    // =========================================================
    // UI
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = UIHelper.panelEncabezado();
        p.add(UIHelper.titulo("👥  Gestión de Clientes"), BorderLayout.WEST);

        // Buscador
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        busqueda.setOpaque(false);
        JLabel ico = new JLabel("🔍");
        ico.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBuscar = UIHelper.campoTexto();
        txtBuscar.setPreferredSize(new Dimension(280, 32));
        txtBuscar.setToolTipText("Buscar por nombre, apellido, documento o email");
        busqueda.add(ico);
        busqueda.add(txtBuscar);

        // Botones
        btnActualizar = UIHelper.botonNeutro("↺  Actualizar");
        btnNuevo      = UIHelper.botonExito("➕  Nuevo");
        btnEditar     = UIHelper.botonPrimario("✏  Editar");
        btnEliminar   = UIHelper.botonPeligro("🗑  Eliminar");
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);

        btnActualizar.addActionListener(e -> cargarClientes());
        btnNuevo.addActionListener(e -> abrirFormNuevo());
        btnEditar.addActionListener(e -> abrirFormEditar());
        btnEliminar.addActionListener(e -> eliminarCliente());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        botones.add(btnActualizar);
        botones.add(btnNuevo);
        botones.add(btnEditar);
        botones.add(btnEliminar);

        p.add(busqueda, BorderLayout.CENTER);
        p.add(botones,  BorderLayout.EAST);
        return p;
    }

    private JPanel crearCentro() {
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        UIHelper.estilizarTabla(tabla);
        UIHelper.ocultarColumnaId(tabla);

        int[] anchos = {0, 130, 130, 90, 110, 110, 170, 110};
        for (int i = 1; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);

        // Filtro en tiempo real con TableRowSorter (sin re-consultar BD)
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirFormEditar();
            }
        });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean sel = tabla.getSelectedRow() >= 0;
                btnEditar.setEnabled(sel);
                btnEliminar.setEnabled(sel);
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.COLOR_BORDE));

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_FONDO);
        p.setBorder(new javax.swing.border.EmptyBorder(10, 14, 10, 14));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearBarraEstado() {
        lblEstado = new JLabel("Cargando...");
        return UIHelper.barraEstado(lblEstado,
            new JLabel("Doble clic para editar  |  Clic en encabezado para ordenar"));
    }

    // =========================================================
    // DATOS
    // =========================================================

    public void cargarClientes() {
        modeloTabla.setRowCount(0);
        txtBuscar.setText("");

        List<Cliente> clientes = clienteDAO.listarTodos();
        for (Cliente c : clientes) agregarFila(c);

        lblEstado.setText("📋  " + clientes.size() + " cliente(s) encontrado(s)");
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    private void filtrar() {
        String txt = txtBuscar.getText().trim();
        if (txt.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Busca en columnas: Nombre(1), Apellido(2), Documento(4), Email(6)
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txt, 1, 2, 4, 6));
        }
    }

    private void agregarFila(Cliente c) {
        modeloTabla.addRow(new Object[]{
            c.getId(),
            c.getNombre(),
            c.getApellido(),
            c.getTipoDocumento(),
            c.getDocumento(),
            c.getTelefono()    != null ? c.getTelefono()    : "—",
            c.getEmail()       != null ? c.getEmail()       : "—",
            c.getNacionalidad() != null ? c.getNacionalidad() : "—"
        });
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    private void abrirFormNuevo() {
        ClienteFormDialog dlg = new ClienteFormDialog(ventanaPadre, null);
        dlg.setVisible(true);
        if (dlg.isGuardadoExitoso()) cargarClientes();
    }

    private void abrirFormEditar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int id = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        Cliente c = clienteDAO.buscarPorId(id);
        if (c == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el cliente.", "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        ClienteFormDialog dlg = new ClienteFormDialog(ventanaPadre, c);
        dlg.setVisible(true);
        if (dlg.isGuardadoExitoso()) cargarClientes();
    }

    private void eliminarCliente() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int    modelo  = tabla.convertRowIndexToModel(fila);
        int    id      = (int) modeloTabla.getValueAt(modelo, 0);
        String nombre  = modeloTabla.getValueAt(modelo, 1) + " " +
                         modeloTabla.getValueAt(modelo, 2);

        int ok = JOptionPane.showConfirmDialog(this,
            "¿Eliminar a " + nombre + "?\nEsta acción no se puede deshacer.",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (ok == JOptionPane.YES_OPTION) {
            if (clienteDAO.eliminar(id)) {
                cargarClientes();
                lblEstado.setText("✓ Cliente eliminado.");
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar.\n" +
                    "Es posible que tenga reservaciones asociadas.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
