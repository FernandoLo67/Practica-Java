package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.Cliente;
import com.hotel.util.BitacoraService;
import com.hotel.util.ExcelExporter;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Panel principal del módulo de Gestión de Clientes.
 *
 * Versión 3.0 — agrega:
 *   - KPI cards (total, activos, nuevos este mes)
 *   - Columnas "Activo" y "Fecha Registro"
 *   - Toggle "Solo activos / Todos"
 *   - Botón "📋 Historial" para ver reservaciones del cliente seleccionado
 *
 * @author Fernando
 * @version 3.0
 */
public class ClientesPanel extends JPanel {

    // =========================================================
    // COLUMNAS DE LA TABLA
    // =========================================================
    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Apellido", "Tipo Doc.", "Documento",
        "Teléfono", "Email", "Nacionalidad", "Activo", "Fecha Registro"
    };

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTable            tabla;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField        txtBuscar;
    private JLabel            lblEstado;

    private JButton btnNuevo;
    private JButton btnEditar;
    private JButton btnEliminar;
    private JButton btnActualizar;
    private JButton btnHistorial;
    private JButton btnToggle;

    // KPI labels
    private JLabel lblKpiTotal;
    private JLabel lblKpiActivos;
    private JLabel lblKpiNuevos;

    // =========================================================
    // ESTADO
    // =========================================================
    private boolean mostrarSoloActivos = false;
    private static final SimpleDateFormat FMT_FECHA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

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
    // UI — ENCABEZADO (título + buscador + botones + KPIs)
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(crearBarra(),  BorderLayout.NORTH);
        wrap.add(crearKPIs(),   BorderLayout.SOUTH);
        return wrap;
    }

    /** Barra superior: título, buscador, botones de acción. */
    private JPanel crearBarra() {
        JPanel p = UIHelper.panelEncabezado();
        p.add(UIHelper.titulo("👥  Gestión de Clientes"), BorderLayout.WEST);

        // Buscador
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        busqueda.setOpaque(false);
        JLabel ico = new JLabel("🔍");
        ico.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBuscar = UIHelper.campoTexto();
        txtBuscar.setPreferredSize(new Dimension(240, 32));
        txtBuscar.setToolTipText("Buscar por nombre, apellido, documento o email");
        busqueda.add(ico);
        busqueda.add(txtBuscar);

        // Toggle activos
        btnToggle = UIHelper.botonNeutro("👁  Solo activos");
        btnToggle.setToolTipText("Filtrar clientes activos / mostrar todos");
        btnToggle.addActionListener(e -> toggleFiltroActivos());

        // Botones de acción
        btnActualizar = UIHelper.botonNeutro("↺  Actualizar");
        btnNuevo      = UIHelper.botonExito("➕  Nuevo");
        btnEditar     = UIHelper.botonPrimario("✏  Editar");
        btnEliminar   = UIHelper.botonPeligro("🗑  Eliminar");
        btnHistorial  = UIHelper.botonNeutro("📋  Historial");

        btnEditar   .setEnabled(false);
        btnEliminar .setEnabled(false);
        btnHistorial.setEnabled(false);

        btnActualizar.addActionListener(e -> cargarClientes());
        btnNuevo     .addActionListener(e -> abrirFormNuevo());
        btnEditar    .addActionListener(e -> abrirFormEditar());
        btnEliminar  .addActionListener(e -> eliminarCliente());
        btnHistorial .addActionListener(e -> abrirHistorial());

        JButton btnExcel = UIHelper.botonNeutro("📊  Excel");
        btnExcel.addActionListener(e ->
            ExcelExporter.exportar(tabla, "Clientes", ventanaPadre));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        botones.setOpaque(false);
        botones.add(btnActualizar);
        botones.add(btnToggle);
        botones.add(btnExcel);
        botones.add(btnHistorial);
        botones.add(btnNuevo);
        botones.add(btnEditar);
        botones.add(btnEliminar);

        p.add(busqueda, BorderLayout.CENTER);
        p.add(botones,  BorderLayout.EAST);
        return p;
    }

    /** Fila de KPI cards debajo de la barra. */
    private JPanel crearKPIs() {
        JPanel fila = new JPanel(new GridLayout(1, 3, 10, 0));
        fila.setBackground(Tema.COLOR_FONDO);
        fila.setBorder(new EmptyBorder(8, 14, 4, 14));

        lblKpiTotal   = crearKpiLabel();
        lblKpiActivos = crearKpiLabel();
        lblKpiNuevos  = crearKpiLabel();

        fila.add(crearKpiCard("Total clientes",   lblKpiTotal,   new Color(26, 35, 126)));
        fila.add(crearKpiCard("Clientes activos",  lblKpiActivos, new Color(21, 128, 61)));
        fila.add(crearKpiCard("Nuevos este mes",   lblKpiNuevos,  new Color(146, 64, 14)));

        return fila;
    }

    private JLabel crearKpiLabel() {
        JLabel lbl = new JLabel("—", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    private JPanel crearKpiCard(String titulo, JLabel lblValor, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(color);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTit.setForeground(new Color(255, 255, 255, 200));

        card.add(lblValor, BorderLayout.CENTER);
        card.add(lblTit,   BorderLayout.SOUTH);
        return card;
    }

    // =========================================================
    // UI — TABLA CENTRAL
    // =========================================================

    private JPanel crearCentro() {
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        UIHelper.estilizarTabla(tabla);
        UIHelper.ocultarColumnaId(tabla);

        // Anchos: ID(oculto), Nombre, Apellido, TipoDoc, Documento, Tel, Email, Nac, Activo, FechaReg
        int[] anchos = {0, 120, 120, 85, 110, 110, 160, 100, 60, 140};
        for (int i = 1; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);

        // Filtro de texto en tiempo real (col 1,2,4,6)
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        // Doble clic → editar
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirFormEditar();
            }
        });

        // Selección → habilitar botones
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean sel = tabla.getSelectedRow() >= 0;
                btnEditar   .setEnabled(sel);
                btnEliminar .setEnabled(sel);
                btnHistorial.setEnabled(sel);
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.COLOR_BORDE));

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_FONDO);
        p.setBorder(new EmptyBorder(8, 14, 10, 14));
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

        for (Cliente c : clientes) {
            // Si el toggle de activos está activo, omitir inactivos
            if (mostrarSoloActivos && !c.isActivo()) continue;
            agregarFila(c);
        }

        // Actualizar KPI labels
        lblKpiTotal  .setText(String.valueOf(clienteDAO.contarTodos()));
        lblKpiActivos.setText(String.valueOf(clienteDAO.contarActivos()));
        lblKpiNuevos .setText(String.valueOf(clienteDAO.contarNuevosEsteMes()));

        long visibles = modeloTabla.getRowCount();
        lblEstado.setText("📋  " + visibles + " cliente(s)  " +
            (mostrarSoloActivos ? "— filtrando solo activos" : "— todos"));

        btnEditar   .setEnabled(false);
        btnEliminar .setEnabled(false);
        btnHistorial.setEnabled(false);
    }

    private void filtrar() {
        String txt = txtBuscar.getText().trim();
        if (txt.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txt, 1, 2, 4, 6));
        }
    }

    private void agregarFila(Cliente c) {
        String fechaStr = (c.getFechaRegistro() != null)
            ? FMT_FECHA.format(c.getFechaRegistro()) : "—";

        modeloTabla.addRow(new Object[]{
            c.getId(),
            c.getNombre(),
            c.getApellido(),
            c.getTipoDocumento(),
            c.getDocumento(),
            c.getTelefono()     != null ? c.getTelefono()     : "—",
            c.getEmail()        != null ? c.getEmail()        : "—",
            c.getNacionalidad() != null ? c.getNacionalidad() : "—",
            c.isActivo() ? "✓" : "✗",
            fechaStr
        });
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    private void toggleFiltroActivos() {
        mostrarSoloActivos = !mostrarSoloActivos;
        btnToggle.setText(mostrarSoloActivos ? "👁  Mostrar todos" : "👁  Solo activos");
        cargarClientes();
    }

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

    private void abrirHistorial() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int    id     = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        String nombre = modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 1)
                      + " " + modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 2);
        ClienteHistorialDialog dlg = new ClienteHistorialDialog(ventanaPadre, id, nombre);
        dlg.setVisible(true);
    }

    private void eliminarCliente() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int    modelo  = tabla.convertRowIndexToModel(fila);
        int    id      = (int) modeloTabla.getValueAt(modelo, 0);
        String nombre  = modeloTabla.getValueAt(modelo, 1) + " "
                       + modeloTabla.getValueAt(modelo, 2);

        int ok = JOptionPane.showConfirmDialog(this,
            "¿Eliminar a " + nombre + "?\nEsta acción no se puede deshacer.",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (ok == JOptionPane.YES_OPTION) {
            if (clienteDAO.eliminar(id)) {
                BitacoraService.log(Bitacora.ACCION_ELIMINAR, Bitacora.MODULO_CLIENTES,
                    "Cliente eliminado: " + nombre + " (ID: " + id + ")");
                cargarClientes();
                lblEstado.setText("✓  Cliente eliminado.");
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar.\n" +
                    "Es posible que tenga reservaciones asociadas.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
