package com.hotel.vista;

import com.hotel.dao.impl.UsuarioDAOImpl;
import com.hotel.modelo.Usuario;
import com.hotel.util.PasswordUtil;
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
 * Panel de Gestión de Usuarios del sistema.
 *
 * SOLO VISIBLE PARA ADMINISTRADORES.
 *
 * Funcionalidades:
 *   - Listar todos los usuarios con su rol y estado
 *   - Crear nuevos usuarios (con hash BCrypt automático)
 *   - Editar nombre, rol y estado de usuario
 *   - Cambiar contraseña con confirmación (genera nuevo hash BCrypt)
 *   - Activar / desactivar cuentas
 *   - Búsqueda en tiempo real por nombre, usuario o rol
 *
 * @author Fernando
 * @version 1.0
 */
public class UsuariosPanel extends JPanel {

    // Índices de columnas del modelo
    private static final int COL_ID      = 0;
    private static final int COL_NOMBRE  = 1;
    private static final int COL_USUARIO = 2;
    private static final int COL_ROL     = 3;
    private static final int COL_ESTADO  = 4;
    private static final int COL_CREADO  = 5;

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
    private JButton btnCambiarPassword;
    private JButton btnToggleActivo;
    private JButton btnActualizar;

    private final UsuarioDAOImpl usuarioDAO;
    private final Frame          ventanaPadre;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public UsuariosPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.usuarioDAO   = new UsuarioDAOImpl();

        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);

        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCentro(),      BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);

        cargarDatos();
        configurarListeners();
    }

    // =========================================================
    // UI — ENCABEZADO
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = UIHelper.panelEncabezado();
        p.add(UIHelper.titulo("👤  Gestión de Usuarios"), BorderLayout.WEST);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        busqueda.setOpaque(false);
        JLabel ico = new JLabel("🔍");
        ico.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBuscar = UIHelper.campoTexto();
        txtBuscar.setPreferredSize(new Dimension(220, 32));
        txtBuscar.setToolTipText("Buscar por nombre, usuario o rol…");
        busqueda.add(ico);
        busqueda.add(txtBuscar);
        p.add(busqueda, BorderLayout.EAST);

        return p;
    }

    // =========================================================
    // UI — CENTRO (barra de herramientas + tabla)
    // =========================================================

    private JPanel crearCentro() {
        // Modelo de tabla no editable
        String[] cols = {"ID", "Nombre completo", "Usuario", "Rol", "Estado", "Creado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        UIHelper.estilizarTabla(tabla);
        UIHelper.ocultarColumnaId(tabla);
        tabla.setDefaultRenderer(Object.class,
            UIHelper.rendererConEstado(tabla, COL_ESTADO));

        // Ancho de columnas
        tabla.getColumnModel().getColumn(COL_NOMBRE).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(COL_USUARIO).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(COL_ROL).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(COL_ESTADO).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(COL_CREADO).setPreferredWidth(135);

        // Ordenamiento
        sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.COLOR_BORDE));

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBackground(Tema.COLOR_FONDO);
        centro.setBorder(new EmptyBorder(10, 14, 10, 14));
        centro.add(crearBarraHerramientas(), BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);

        return centro;
    }

    private JPanel crearBarraHerramientas() {
        btnNuevo           = UIHelper.botonExito("➕  Nuevo usuario");
        btnEditar          = UIHelper.botonPrimario("✏  Editar");
        btnCambiarPassword = UIHelper.botonNeutro("🔑  Cambiar contraseña");
        btnToggleActivo    = UIHelper.botonSecundario("⏸  Desactivar");
        btnActualizar      = UIHelper.botonSecundario("↺  Actualizar");

        btnEditar.setEnabled(false);
        btnCambiarPassword.setEnabled(false);
        btnToggleActivo.setEnabled(false);

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        p.add(btnNuevo);
        p.add(btnEditar);
        p.add(btnCambiarPassword);
        p.add(btnToggleActivo);
        p.add(Box.createHorizontalStrut(10));
        p.add(btnActualizar);
        return p;
    }

    private JPanel crearBarraEstado() {
        lblEstado = new JLabel("Cargando…");
        return UIHelper.barraEstado(lblEstado,
            new JLabel("⚠  Solo administradores pueden gestionar usuarios"));
    }

    // =========================================================
    // DATOS
    // =========================================================

    private void cargarDatos() {
        modeloTabla.setRowCount(0);
        List<Usuario> lista = usuarioDAO.listarTodos();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Usuario u : lista) {
            modeloTabla.addRow(new Object[]{
                u.getId(),
                u.getNombre(),
                u.getUsuario(),
                u.getRol(),
                u.isActivo() ? "ACTIVO" : "INACTIVO",
                u.getFechaCreacion() != null ? sdf.format(u.getFechaCreacion()) : "—"
            });
        }

        lblEstado.setText("Total: " + lista.size() + " usuario(s)");
        actualizarBotones();
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private void configurarListeners() {
        btnNuevo.addActionListener(e -> abrirFormularioNuevo());
        btnEditar.addActionListener(e -> abrirFormularioEditar());
        btnCambiarPassword.addActionListener(e -> cambiarPassword());
        btnToggleActivo.addActionListener(e -> toggleActivo());
        btnActualizar.addActionListener(e -> cargarDatos());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarBotones();
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirFormularioEditar();
            }
        });

        // Filtro en tiempo real
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
    }

    private void filtrar() {
        String txt = txtBuscar.getText().trim();
        if (txt.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter(
                "(?i)" + txt, COL_NOMBRE, COL_USUARIO, COL_ROL));
        }
    }

    private void actualizarBotones() {
        boolean sel = tabla.getSelectedRow() >= 0;
        btnEditar.setEnabled(sel);
        btnCambiarPassword.setEnabled(sel);
        btnToggleActivo.setEnabled(sel);

        if (sel) {
            String estado = obtenerEstadoFilaSeleccionada();
            btnToggleActivo.setText("ACTIVO".equals(estado)
                ? "⏸  Desactivar" : "▶  Activar");
        }
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    private void abrirFormularioNuevo() {
        UsuarioFormDialog dlg = new UsuarioFormDialog(ventanaPadre, null);
        dlg.setVisible(true);
        if (dlg.isGuardado()) {
            cargarDatos();
            lblEstado.setText("✓ Usuario creado exitosamente.");
        }
    }

    private void abrirFormularioEditar() {
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) return;

        UsuarioFormDialog dlg = new UsuarioFormDialog(ventanaPadre, u);
        dlg.setVisible(true);
        if (dlg.isGuardado()) {
            cargarDatos();
            lblEstado.setText("✓ Usuario '" + u.getNombre() + "' actualizado.");
        }
    }

    private void cambiarPassword() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;

        int    id     = (int)    modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), COL_ID);
        String nombre = (String) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), COL_NOMBRE);

        JPasswordField txtNueva    = UIHelper.campoPassword();
        JPasswordField txtConfirma = UIHelper.campoPassword();
        txtNueva.setPreferredSize(new Dimension(260, 36));
        txtConfirma.setPreferredSize(new Dimension(260, 36));

        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 6));
        panel.setBorder(new EmptyBorder(8, 0, 4, 0));
        panel.add(UIHelper.etiquetaCampo("Nueva contraseña para: " + nombre));
        panel.add(txtNueva);
        panel.add(UIHelper.etiquetaCampo("Confirmar contraseña:"));
        panel.add(txtConfirma);

        int res = JOptionPane.showConfirmDialog(this, panel,
            "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String nueva    = new String(txtNueva.getPassword()).trim();
        String confirma = new String(txtConfirma.getPassword()).trim();

        if (nueva.isEmpty()) {
            mostrarError("La contraseña no puede estar vacía."); return;
        }
        if (nueva.length() < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres."); return;
        }
        if (!nueva.equals(confirma)) {
            mostrarError("Las contraseñas no coinciden."); return;
        }

        String hash = PasswordUtil.hashear(nueva);
        if (usuarioDAO.actualizarPassword(id, hash)) {
            lblEstado.setText("✓ Contraseña de '" + nombre + "' actualizada con BCrypt.");
        } else {
            mostrarError("Error al actualizar la contraseña.");
        }
    }

    private void toggleActivo() {
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) return;

        boolean estaActivo = u.isActivo();
        String accion = estaActivo ? "desactivar" : "activar";

        int ok = JOptionPane.showConfirmDialog(this,
            "¿Deseas " + accion + " la cuenta de '" + u.getNombre() + "'?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        u.setActivo(!estaActivo);
        if (usuarioDAO.actualizar(u)) {
            cargarDatos();
            lblEstado.setText("✓ Cuenta de '" + u.getNombre() + "' " + accion + "da.");
        } else {
            mostrarError("No se pudo actualizar el estado del usuario.");
        }
    }

    // =========================================================
    // UTILIDADES PRIVADAS
    // =========================================================

    private Usuario obtenerUsuarioSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return null;
        int id = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), COL_ID);
        Usuario u = usuarioDAO.buscarPorId(id);
        if (u == null) mostrarError("No se pudo cargar el usuario.");
        return u;
    }

    private String obtenerEstadoFilaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return "";
        return (String) modeloTabla.getValueAt(
            tabla.convertRowIndexToModel(fila), COL_ESTADO);
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
