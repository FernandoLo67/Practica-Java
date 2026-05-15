package com.hotel.vista;

import com.hotel.dao.impl.UsuarioDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.Usuario;
import com.hotel.util.BitacoraService;
import com.hotel.util.PasswordUtil;
import com.hotel.util.Validaciones;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Diálogo modal para que el usuario actual cambie su propia contraseña.
 *
 * Flujo:
 *   1. Ingresa contraseña actual → se verifica con BCrypt
 *   2. Ingresa nueva contraseña → se valida fortaleza mínima
 *   3. Confirma la nueva contraseña → deben coincidir
 *   4. Se hashea con BCrypt y se persiste en la BD
 *   5. Se registra la acción en la bitácora
 *
 * @author Fernando
 * @version 1.0
 */
public class CambiarPasswordDialog extends JDialog {

    // =========================================================
    // COLORES
    // =========================================================
    private static final Color COLOR_PRIMARIO = new Color(26, 35, 126);
    private static final Color COLOR_FONDO    = new Color(248, 250, 255);
    private static final Color COLOR_ERROR    = new Color(185, 28, 28);
    private static final Color COLOR_OK       = new Color(21, 128, 61);

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JPasswordField pfActual;
    private JPasswordField pfNueva;
    private JPasswordField pfConfirmar;
    private JLabel         lblFortaleza;
    private JProgressBar   barraFortaleza;
    private JLabel         lblMensaje;

    // =========================================================
    // ESTADO
    // =========================================================
    private final Usuario          usuario;
    private final UsuarioDAOImpl   dao;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public CambiarPasswordDialog(Frame padre, Usuario usuario) {
        super(padre, "Cambiar contraseña", true);
        this.usuario = usuario;
        this.dao     = new UsuarioDAOImpl();
        initComponents();
        setSize(420, 440);
        setLocationRelativeTo(padre);
        setResizable(false);
    }

    // =========================================================
    // UI
    // =========================================================

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO);
        add(crearHeader(),     BorderLayout.NORTH);
        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotones(),    BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(COLOR_PRIMARIO);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titulo = new JLabel("🔑  Cambiar contraseña");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Usuario: " + usuario.getUsuario()
                + "  •  " + usuario.getRol());
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(180, 195, 255));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(sub);
        p.add(textos, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_FONDO);
        p.setBorder(new EmptyBorder(20, 24, 8, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 4, 5, 4);
        g.weightx = 1;

        // Contraseña actual
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        p.add(etiqueta("Contraseña actual *"), g);
        g.gridx = 0; g.gridy = 1; g.weightx = 1;
        pfActual = campo();
        p.add(pfActual, g);

        // Separador visual
        g.gridx = 0; g.gridy = 2;
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(220, 224, 245));
        p.add(sep, g);

        // Nueva contraseña
        g.gridx = 0; g.gridy = 3; g.weightx = 0;
        p.add(etiqueta("Nueva contraseña *"), g);
        g.gridx = 0; g.gridy = 4; g.weightx = 1;
        pfNueva = campo();
        pfNueva.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { actualizarFortaleza(); }
        });
        p.add(pfNueva, g);

        // Barra de fortaleza
        g.gridx = 0; g.gridy = 5;
        barraFortaleza = new JProgressBar(0, 4);
        barraFortaleza.setPreferredSize(new Dimension(0, 6));
        barraFortaleza.setBorderPainted(false);
        barraFortaleza.setValue(0);
        p.add(barraFortaleza, g);

        g.gridx = 0; g.gridy = 6;
        lblFortaleza = new JLabel(" ");
        lblFortaleza.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFortaleza.setForeground(Color.GRAY);
        p.add(lblFortaleza, g);

        // Confirmar contraseña
        g.gridx = 0; g.gridy = 7; g.weightx = 0;
        p.add(etiqueta("Confirmar nueva contraseña *"), g);
        g.gridx = 0; g.gridy = 8; g.weightx = 1;
        pfConfirmar = campo();
        p.add(pfConfirmar, g);

        // Mensaje de error/éxito
        g.gridx = 0; g.gridy = 9;
        lblMensaje = new JLabel(" ");
        lblMensaje.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMensaje.setForeground(COLOR_ERROR);
        p.add(lblMensaje, g);

        // Requisitos mínimos
        g.gridx = 0; g.gridy = 10;
        JLabel req = new JLabel("  Mínimo 6 caracteres. Recomendado: letras + números.");
        req.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        req.setForeground(Color.GRAY);
        p.add(req, g);

        return p;
    }

    private JPanel crearBotones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancelar.addActionListener(e -> dispose());

        JButton btnGuardar = new JButton("🔒  Guardar contraseña");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(COLOR_PRIMARIO);
        btnGuardar.setOpaque(true);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardar());

        // Enter en el último campo también guarda
        pfConfirmar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) guardar();
            }
        });

        p.add(btnCancelar);
        p.add(btnGuardar);
        return p;
    }

    // =========================================================
    // LÓGICA
    // =========================================================

    private void guardar() {
        String actual    = new String(pfActual.getPassword());
        String nueva     = new String(pfNueva.getPassword());
        String confirmar = new String(pfConfirmar.getPassword());

        // 1. Verificar que no estén vacíos
        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            error("Todos los campos son obligatorios.");
            return;
        }

        // 2. Verificar contraseña actual con BCrypt
        if (!PasswordUtil.verificar(actual, usuario.getPassword())) {
            error("La contraseña actual es incorrecta.");
            pfActual.requestFocus();
            pfActual.selectAll();
            return;
        }

        // 3. Validar nueva contraseña
        String errNueva = Validaciones.validarPassword(nueva);
        if (errNueva != null) {
            error(errNueva);
            pfNueva.requestFocus();
            return;
        }

        // 4. Confirmar que coinciden
        if (!nueva.equals(confirmar)) {
            error("Las contraseñas nuevas no coinciden.");
            pfConfirmar.requestFocus();
            pfConfirmar.selectAll();
            return;
        }

        // 5. No permitir que sea igual a la actual
        if (PasswordUtil.verificar(nueva, usuario.getPassword())) {
            error("La nueva contraseña debe ser diferente a la actual.");
            pfNueva.requestFocus();
            return;
        }

        // 6. Hashear y persistir
        String nuevoHash = PasswordUtil.hashear(nueva);
        boolean ok = dao.actualizarPassword(usuario.getId(), nuevoHash);

        if (ok) {
            // Actualizar el objeto en sesión para evitar re-login
            usuario.setPassword(nuevoHash);

            // Bitácora
            BitacoraService.log(Bitacora.ACCION_EDITAR, Bitacora.MODULO_USUARIOS,
                "Contraseña cambiada por el usuario: " + usuario.getUsuario());

            JOptionPane.showMessageDialog(this,
                "✅  Contraseña actualizada correctamente.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            error("Error al guardar. Intenta de nuevo.");
        }
    }

    /**
     * Actualiza la barra de fortaleza según la contraseña ingresada.
     * Criterios: longitud ≥6, ≥8, tiene número, tiene mayúscula/símbolo.
     */
    private void actualizarFortaleza() {
        String pwd = new String(pfNueva.getPassword());
        int score = 0;
        if (pwd.length() >= 6)                          score++;
        if (pwd.length() >= 8)                          score++;
        if (pwd.matches(".*\\d.*"))                     score++;
        if (pwd.matches(".*[A-Z].*"))                   score++;
        if (pwd.matches(".*[^a-zA-Z0-9].*"))            score++;
        // cap a 4
        score = Math.min(score, 4);

        barraFortaleza.setValue(score);
        switch (score) {
            case 0: case 1:
                barraFortaleza.setForeground(COLOR_ERROR);
                lblFortaleza.setText("  Muy débil");
                lblFortaleza.setForeground(COLOR_ERROR);
                break;
            case 2:
                barraFortaleza.setForeground(new Color(217, 119, 6));
                lblFortaleza.setText("  Débil");
                lblFortaleza.setForeground(new Color(217, 119, 6));
                break;
            case 3:
                barraFortaleza.setForeground(new Color(101, 163, 13));
                lblFortaleza.setText("  Aceptable");
                lblFortaleza.setForeground(new Color(101, 163, 13));
                break;
            case 4:
                barraFortaleza.setForeground(COLOR_OK);
                lblFortaleza.setText("  Fuerte ✓");
                lblFortaleza.setForeground(COLOR_OK);
                break;
        }
        if (pwd.isEmpty()) {
            barraFortaleza.setValue(0);
            lblFortaleza.setText(" ");
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(50, 55, 80));
        return l;
    }

    private JPasswordField campo() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return f;
    }

    private void error(String msg) {
        lblMensaje.setForeground(COLOR_ERROR);
        lblMensaje.setText("⚠  " + msg);
    }
}
