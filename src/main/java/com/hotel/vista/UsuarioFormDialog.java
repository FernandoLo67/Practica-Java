package com.hotel.vista;

import com.hotel.dao.impl.UsuarioDAOImpl;
import com.hotel.exception.HotelException;
import com.hotel.modelo.Usuario;
import com.hotel.util.PasswordUtil;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diálogo modal para crear o editar un usuario del sistema.
 *
 * Modo NUEVO (usuario == null):
 *   - Muestra campo de contraseña obligatorio
 *   - Hashea la contraseña con BCrypt antes de guardar
 *
 * Modo EDITAR (usuario != null):
 *   - Oculta el campo de contraseña (se cambia desde el panel principal)
 *   - Solo permite modificar nombre, usuario, rol y estado
 *
 * @author Fernando
 * @version 1.0
 */
public class UsuarioFormDialog extends JDialog {

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTextField     txtNombre;
    private JTextField     txtUsuario;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirma;
    private JComboBox<String> cmbRol;
    private JCheckBox      chkActivo;
    private JLabel         lblMensaje;

    // =========================================================
    // ESTADO
    // =========================================================
    private final Usuario        usuarioEditar; // null = modo nuevo
    private final UsuarioDAOImpl usuarioDAO;
    private boolean              guardado = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * @param padre        Ventana padre (para posicionamiento y modalidad)
     * @param usuarioEditar Usuario a editar, o null para crear uno nuevo
     */
    public UsuarioFormDialog(Frame padre, Usuario usuarioEditar) {
        super(padre, usuarioEditar == null ? "Nuevo usuario" : "Editar usuario", true);
        this.usuarioEditar = usuarioEditar;
        this.usuarioDAO    = new UsuarioDAOImpl();

        setContentPane(crearContenido());
        pack();
        setMinimumSize(new Dimension(420, 0));
        setResizable(false);
        setLocationRelativeTo(padre);

        if (usuarioEditar != null) {
            cargarDatos();
        }
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA UI
    // =========================================================

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ── Encabezado ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Tema.COLOR_PRIMARIO);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        String icono = usuarioEditar == null ? "➕" : "✏";
        JLabel titulo = new JLabel(icono + "  " +
            (usuarioEditar == null ? "Nuevo usuario" : "Editar usuario"));
        titulo.setFont(Tema.FUENTE_TITULO);
        titulo.setForeground(Color.WHITE);
        header.add(titulo);
        root.add(header, BorderLayout.NORTH);

        // ── Formulario ──
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 12, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(5, 0, 5, 0);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx   = 0;

        // Nombre completo
        g.gridy = 0;
        form.add(UIHelper.etiquetaCampo("Nombre completo *"), g);
        g.gridy = 1;
        txtNombre = UIHelper.campoTexto();
        form.add(txtNombre, g);

        // Nombre de usuario
        g.gridy = 2;
        form.add(UIHelper.etiquetaCampo("Usuario (para login) *"), g);
        g.gridy = 3;
        txtUsuario = UIHelper.campoTexto();
        form.add(txtUsuario, g);

        // Contraseña (solo en modo NUEVO)
        if (usuarioEditar == null) {
            g.gridy = 4;
            form.add(UIHelper.etiquetaCampo("Contraseña * (mín. 6 caracteres)"), g);
            g.gridy = 5;
            txtPassword = UIHelper.campoPassword();
            form.add(txtPassword, g);

            g.gridy = 6;
            form.add(UIHelper.etiquetaCampo("Confirmar contraseña *"), g);
            g.gridy = 7;
            txtConfirma = UIHelper.campoPassword();
            form.add(txtConfirma, g);
        }

        // Rol
        int baseRow = (usuarioEditar == null) ? 8 : 4;
        g.gridy = baseRow;
        form.add(UIHelper.etiquetaCampo("Rol *"), g);
        g.gridy = baseRow + 1;
        cmbRol = new JComboBox<>(new String[]{"RECEPCIONISTA", "ADMIN"});
        cmbRol.setFont(Tema.FUENTE_CAMPO);
        cmbRol.setPreferredSize(new Dimension(0, 36));
        form.add(cmbRol, g);

        // Estado activo (solo en modo EDITAR)
        if (usuarioEditar != null) {
            g.gridy = baseRow + 2;
            chkActivo = new JCheckBox("Cuenta activa");
            chkActivo.setFont(Tema.FUENTE_NORMAL);
            chkActivo.setBackground(Color.WHITE);
            chkActivo.setSelected(true);
            form.add(chkActivo, g);
        }

        // Mensaje de error/éxito
        g.gridy = baseRow + 3;
        lblMensaje = new JLabel(" ");
        lblMensaje.setFont(Tema.FUENTE_SMALL);
        lblMensaje.setForeground(Tema.COLOR_ERROR);
        form.add(lblMensaje, g);

        root.add(form, BorderLayout.CENTER);

        // ── Botones ──
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        botones.setBackground(Color.WHITE);
        botones.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.COLOR_BORDE),
            new EmptyBorder(0, 16, 0, 16)
        ));

        JButton btnCancelar = UIHelper.botonSecundario("Cancelar");
        JButton btnGuardar  = UIHelper.botonExito(
            usuarioEditar == null ? "Crear usuario" : "Guardar cambios");

        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());

        botones.add(btnCancelar);
        botones.add(btnGuardar);
        root.add(botones, BorderLayout.SOUTH);

        // Enter = guardar
        getRootPane().setDefaultButton(btnGuardar);

        return root;
    }

    // =========================================================
    // LÓGICA
    // =========================================================

    /** Precarga los campos con los datos del usuario a editar */
    private void cargarDatos() {
        txtNombre.setText(usuarioEditar.getNombre());
        txtUsuario.setText(usuarioEditar.getUsuario());
        cmbRol.setSelectedItem(usuarioEditar.getRol());
        if (chkActivo != null) chkActivo.setSelected(usuarioEditar.isActivo());
    }

    private void guardar() {
        lblMensaje.setText(" ");
        lblMensaje.setForeground(Tema.COLOR_ERROR);

        // ── Validar campos ──
        String nombre  = txtNombre.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String rol     = (String) cmbRol.getSelectedItem();

        if (nombre.isEmpty()) {
            lblMensaje.setText("El nombre no puede estar vacío."); return;
        }
        if (usuario.isEmpty()) {
            lblMensaje.setText("El usuario no puede estar vacío."); return;
        }
        if (usuario.length() < 3) {
            lblMensaje.setText("El usuario debe tener al menos 3 caracteres."); return;
        }

        // ── Validaciones solo para NUEVO ──
        String passwordFinal = null;
        if (usuarioEditar == null) {
            String pass1 = new String(txtPassword.getPassword()).trim();
            String pass2 = new String(txtConfirma.getPassword()).trim();

            if (pass1.isEmpty()) {
                lblMensaje.setText("La contraseña no puede estar vacía."); return;
            }
            if (pass1.length() < 6) {
                lblMensaje.setText("La contraseña debe tener al menos 6 caracteres."); return;
            }
            if (!pass1.equals(pass2)) {
                lblMensaje.setText("Las contraseñas no coinciden."); return;
            }
            passwordFinal = PasswordUtil.hashear(pass1);
        }

        // ── Verificar usuario duplicado ──
        boolean esNuevo      = (usuarioEditar == null);
        boolean usuarioCambio = !esNuevo && !usuario.equals(usuarioEditar.getUsuario());

        if ((esNuevo || usuarioCambio) && usuarioDAO.existeUsuario(usuario)) {
            lblMensaje.setText("El usuario '" + usuario + "' ya está registrado."); return;
        }

        // ── Persistir ──
        try {
            if (esNuevo) {
                Usuario nuevo = new Usuario(nombre, usuario, passwordFinal, rol);
                if (!usuarioDAO.guardar(nuevo)) {
                    mostrarError("No se pudo crear el usuario."); return;
                }
            } else {
                usuarioEditar.setNombre(nombre);
                usuarioEditar.setUsuario(usuario);
                usuarioEditar.setRol(rol);
                if (chkActivo != null) usuarioEditar.setActivo(chkActivo.isSelected());
                if (!usuarioDAO.actualizar(usuarioEditar)) {
                    mostrarError("No se pudo actualizar el usuario."); return;
                }
            }

            guardado = true;
            dispose();

        } catch (HotelException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    // =========================================================
    // UTILIDADES
    // =========================================================

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** @return true si el usuario fue guardado correctamente */
    public boolean isGuardado() {
        return guardado;
    }
}
