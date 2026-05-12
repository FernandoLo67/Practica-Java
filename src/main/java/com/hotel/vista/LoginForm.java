package com.hotel.vista;

import com.hotel.dao.impl.UsuarioDAOImpl;
import com.hotel.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Formulario de inicio de sesión del sistema de hotel.
 *
 * Esta es la primera pantalla que ve el usuario al abrir la aplicación.
 * Valida las credenciales contra la base de datos y, si son correctas,
 * abre el menú principal.
 *
 * COMPONENTES:
 *   - txtUsuario      : campo para el nombre de usuario
 *   - txtPassword     : campo para la contraseña (oculta)
 *   - btnLogin        : botón para iniciar sesión
 *   - chkMostrarPass  : checkbox para mostrar/ocultar la contraseña
 *   - lblMensaje      : etiqueta para mostrar errores o éxito
 *
 * @author Fernando
 * @version 1.0
 */
public class LoginForm extends JFrame {

    // =========================================================
    // COMPONENTES DE LA INTERFAZ
    // =========================================================
    private JTextField    txtUsuario;
    private JPasswordField txtPassword;
    private JButton       btnLogin;
    private JLabel        lblMensaje;
    private JCheckBox     chkMostrarPass;

    // DAO para verificar las credenciales en la base de datos
    private final UsuarioDAOImpl usuarioDAO;

    // =========================================================
    // COLORES DEL TEMA (paleta azul oscuro + dorado)
    // =========================================================
    private static final Color COLOR_PRIMARIO  = new Color(26, 35, 126);  // Azul índigo
    private static final Color COLOR_ACENTO    = new Color(255, 160, 0);  // Dorado
    private static final Color COLOR_EXITO     = new Color(46, 125, 50);  // Verde
    private static final Color COLOR_ERROR     = new Color(198, 40, 40);  // Rojo
    private static final Color COLOR_TEXTO     = new Color(33, 33, 33);   // Negro suave

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public LoginForm() {
        this.usuarioDAO = new UsuarioDAOImpl();
        initComponents();
        configurarVentana();
    }

    // =========================================================
    // CONFIGURACIÓN DE LA VENTANA
    // =========================================================

    private void configurarVentana() {
        setTitle("Hotel Sistema - Iniciar Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(430, 520);
        setLocationRelativeTo(null);  // Centra la ventana en la pantalla
        setResizable(false);
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA INTERFAZ
    // =========================================================

    private void initComponents() {
        // Panel raíz con fondo azul
        JPanel panelRaiz = new JPanel(new BorderLayout());
        panelRaiz.setBackground(COLOR_PRIMARIO);

        panelRaiz.add(crearPanelLogo(),       BorderLayout.NORTH);
        panelRaiz.add(crearPanelFormulario(), BorderLayout.CENTER);

        setContentPane(panelRaiz);
    }

    /**
     * Crea el panel superior con el logo e identificación del sistema.
     */
    private JPanel crearPanelLogo() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_PRIMARIO);
        panel.setBorder(new EmptyBorder(35, 20, 20, 20));

        // Emoji de hotel como ícono
        JLabel lblIcono = new JLabel("🏨", SwingConstants.CENTER);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitulo = new JLabel("HOTEL SISTEMA");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Sistema de Administración");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitulo.setForeground(new Color(180, 195, 255));
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(lblIcono);
        panel.add(Box.createVerticalStrut(6));
        panel.add(lblTitulo);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblSubtitulo);
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    /**
     * Crea el panel del formulario (tarjeta blanca con campos y botón).
     */
    private JPanel crearPanelFormulario() {
        // Contenedor externo con padding
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(COLOR_PRIMARIO);
        contenedor.setBorder(new EmptyBorder(0, 28, 35, 28));

        // Tarjeta blanca
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(28, 28, 28, 28)
        ));

        // --- Título de la tarjeta ---
        JLabel lblIngresar = new JLabel("Iniciar Sesión");
        lblIngresar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblIngresar.setForeground(COLOR_PRIMARIO);
        lblIngresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        tarjeta.add(lblIngresar);
        tarjeta.add(Box.createVerticalStrut(22));

        // --- Campo: Usuario ---
        tarjeta.add(crearEtiquetaCampo("Usuario"));
        tarjeta.add(Box.createVerticalStrut(5));
        txtUsuario = new JTextField();
        estilizarCampo(txtUsuario);
        tarjeta.add(txtUsuario);
        tarjeta.add(Box.createVerticalStrut(16));

        // --- Campo: Contraseña ---
        tarjeta.add(crearEtiquetaCampo("Contraseña"));
        tarjeta.add(Box.createVerticalStrut(5));
        txtPassword = new JPasswordField();
        estilizarCampo(txtPassword);
        tarjeta.add(txtPassword);
        tarjeta.add(Box.createVerticalStrut(8));

        // --- Checkbox mostrar contraseña ---
        chkMostrarPass = new JCheckBox("Mostrar contraseña");
        chkMostrarPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkMostrarPass.setForeground(new Color(100, 100, 120));
        chkMostrarPass.setBackground(Color.WHITE);
        chkMostrarPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkMostrarPass.addActionListener(e -> toggleMostrarPassword());
        tarjeta.add(chkMostrarPass);
        tarjeta.add(Box.createVerticalStrut(18));

        // --- Etiqueta de mensaje (error / éxito) ---
        lblMensaje = new JLabel(" ");
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMensaje.setForeground(COLOR_ERROR);
        lblMensaje.setAlignmentX(Component.CENTER_ALIGNMENT);
        tarjeta.add(lblMensaje);
        tarjeta.add(Box.createVerticalStrut(10));

        // --- Botón Login ---
        btnLogin = new JButton("INGRESAR");
        estilizarBoton(btnLogin);
        btnLogin.addActionListener(e -> realizarLogin());
        tarjeta.add(btnLogin);

        // --- Atajo: Enter en contraseña dispara el login ---
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) realizarLogin();
            }
        });
        txtUsuario.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) txtPassword.requestFocus();
            }
        });

        contenedor.add(tarjeta, BorderLayout.CENTER);
        return contenedor;
    }

    // =========================================================
    // MÉTODOS DE ESTILO
    // =========================================================

    private JLabel crearEtiquetaCampo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(COLOR_TEXTO);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void estilizarBoton(JButton boton) {
        boton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        boton.setForeground(Color.WHITE);
        boton.setBackground(COLOR_PRIMARIO);
        boton.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Efecto visual al pasar el mouse
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(COLOR_ACENTO);
                boton.setForeground(COLOR_PRIMARIO);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(COLOR_PRIMARIO);
                boton.setForeground(Color.WHITE);
            }
        });
    }

    // =========================================================
    // LÓGICA DE LA PANTALLA
    // =========================================================

    /**
     * Alterna entre mostrar y ocultar la contraseña.
     */
    private void toggleMostrarPassword() {
        if (chkMostrarPass.isSelected()) {
            txtPassword.setEchoChar((char) 0);      // Muestra los caracteres
        } else {
            txtPassword.setEchoChar('•');       // Oculta con puntos •
        }
    }

    /**
     * Valida los campos y verifica las credenciales en la base de datos.
     * Si son correctas, abre el MenuPrincipal y cierra el Login.
     */
    private void realizarLogin() {
        String usuario  = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        // --- VALIDACIONES DE FORMULARIO ---
        if (usuario.isEmpty()) {
            mostrarError("⚠  Ingresa tu nombre de usuario");
            txtUsuario.requestFocus();
            return;
        }
        if (usuario.length() < 3) {
            mostrarError("⚠  El usuario debe tener al menos 3 caracteres");
            txtUsuario.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            mostrarError("⚠  Ingresa tu contraseña");
            txtPassword.requestFocus();
            return;
        }

        // --- VERIFICAR CREDENCIALES EN LA BASE DE DATOS ---
        try {
            // Deshabilitar el botón mientras se verifica (evita doble clic)
            btnLogin.setEnabled(false);
            btnLogin.setText("Verificando...");

            Usuario usuarioObj = usuarioDAO.autenticar(usuario, password);

            if (usuarioObj != null) {
                // Login exitoso
                mostrarExito("✓  Bienvenido/a, " + usuarioObj.getNombre() + "!");

                // Esperar 1 segundo y abrir el menú principal
                Timer timer = new Timer(900, e -> {
                    new MenuPrincipal(usuarioObj).setVisible(true);
                    dispose(); // Cierra la ventana de login
                });
                timer.setRepeats(false);
                timer.start();

            } else {
                // Credenciales incorrectas
                mostrarError("✗  Usuario o contraseña incorrectos");
                txtPassword.setText("");
                txtPassword.requestFocus();

                // Rehabilitar el botón
                btnLogin.setEnabled(true);
                btnLogin.setText("INGRESAR");
            }

        } catch (Exception ex) {
            // Error de conexión a la base de datos
            mostrarError("✗  Error de conexión a la base de datos");
            btnLogin.setEnabled(true);
            btnLogin.setText("INGRESAR");
            ex.printStackTrace();

            // Mostrar detalle del error al usuario
            JOptionPane.showMessageDialog(
                this,
                "No se pudo conectar a la base de datos.\n\n" +
                "Verifica que:\n" +
                "  • MySQL esté corriendo\n" +
                "  • La base de datos 'hotel_sistema' exista\n" +
                "  • La contraseña en ConexionDB.java sea correcta\n\n" +
                "Error técnico: " + ex.getMessage(),
                "Error de Conexión",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setForeground(COLOR_ERROR);
        lblMensaje.setText(mensaje);
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.setForeground(COLOR_EXITO);
        lblMensaje.setText(mensaje);
    }
}
