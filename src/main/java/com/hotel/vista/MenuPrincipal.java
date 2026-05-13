package com.hotel.vista;

import com.hotel.modelo.Usuario;

import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Ventana principal del sistema de hotel.
 *
 * Contiene:
 *   - Header superior con nombre del usuario y botón de cierre de sesión
 *   - Sidebar (menú lateral) con acceso a todos los módulos
 *   - Área de contenido central donde se cargan los módulos
 *
 * Módulos disponibles (se irán implementando módulo por módulo):
 *   ✓ Dashboard (bienvenida)
 *   → Clientes
 *   → Habitaciones
 *   → Reservaciones
 *   → Check-In / Check-Out
 *   → Facturación
 *   → Reportes
 *
 * @author Fernando
 * @version 1.0
 */
public class MenuPrincipal extends JFrame {

    // =========================================================
    // ATRIBUTOS
    // =========================================================

    /** Usuario que inició sesión (se usa para mostrar su nombre y controlar permisos) */
    private final Usuario usuarioActual;

    /** Panel central donde se cargan los diferentes módulos */
    private JPanel panelContenido;

    // =========================================================
    // COLORES DEL TEMA
    // =========================================================
    private static final Color COLOR_SIDEBAR       = new Color(26, 35, 126);
    private static final Color COLOR_SIDEBAR_HOVER = new Color(40, 53, 147);
    private static final Color COLOR_SIDEBAR_BORDE = new Color(255, 160, 0);
    private static final Color COLOR_FONDO         = new Color(243, 246, 253);
    private static final Color COLOR_HEADER        = Color.WHITE;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MenuPrincipal(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        initComponents();
        configurarVentana();
    }

    // =========================================================
    // CONFIGURACIÓN DE LA VENTANA
    // =========================================================

    private void configurarVentana() {
        setTitle("Hotel Sistema  |  " + usuarioActual.getNombre()
                 + "  [" + usuarioActual.getRol() + "]");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 580));

        // Confirmar antes de cerrar
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmarSalida();
            }
        });
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA INTERFAZ
    // =========================================================

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        add(crearHeader(),   BorderLayout.NORTH);
        add(crearSidebar(),  BorderLayout.WEST);
        add(crearContenido(), BorderLayout.CENTER);

        // Mostrar pantalla de bienvenida al iniciar
        mostrarDashboard();
    }

    /**
     * Crea la barra superior con logo, nombre de usuario y botón cerrar sesión.
     */
    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER);
        header.setPreferredSize(new Dimension(0, 56));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(0, 20, 0, 20)
        ));

        // Lado izquierdo: Logo
        JLabel lblLogo = new JLabel("🏨  Hotel Sistema");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblLogo.setForeground(COLOR_SIDEBAR);

        // Lado derecho: info usuario + botón cerrar
        JPanel panelDerecho = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panelDerecho.setOpaque(false);

        JLabel lblUsuarioInfo = new JLabel("👤  " + usuarioActual.getNombre());
        lblUsuarioInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUsuarioInfo.setForeground(new Color(90, 95, 120));

        // Badge de rol coloreado
        JLabel lblRol = new JLabel("  " + usuarioActual.getRol() + "  ");
        lblRol.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblRol.setForeground(Color.WHITE);
        lblRol.setOpaque(true);
        lblRol.setBackground(usuarioActual.esAdmin()
            ? new Color(26, 35, 126)    // azul para ADMIN
            : new Color(46, 125, 50));  // verde para RECEPCIONISTA
        lblRol.setBorder(new EmptyBorder(3, 8, 3, 8));

        JButton btnCerrar = new JButton("⏻  Cerrar Sesión");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setBackground(new Color(198, 40, 40));
        btnCerrar.setOpaque(true);
        btnCerrar.setBorderPainted(false);
        btnCerrar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 20, 20), 1),
            new EmptyBorder(7, 18, 7, 18)
        ));
        btnCerrar.setFocusPainted(false);
        btnCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrar.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btnCerrar.setBackground(new Color(229, 57, 53));
                btnCerrar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(183, 28, 28), 2),
                    new EmptyBorder(6, 17, 6, 17)
                ));
            }
            @Override public void mouseExited(MouseEvent e) {
                btnCerrar.setBackground(new Color(198, 40, 40));
                btnCerrar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(150, 20, 20), 1),
                    new EmptyBorder(7, 18, 7, 18)
                ));
            }
        });
        btnCerrar.addActionListener(e -> cerrarSesion());

        panelDerecho.add(lblUsuarioInfo);
        panelDerecho.add(lblRol);
        panelDerecho.add(btnCerrar);

        header.add(lblLogo,      BorderLayout.WEST);
        header.add(panelDerecho, BorderLayout.EAST);

        return header;
    }

    /**
     * Crea el menú lateral izquierdo con todos los módulos.
     */
    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setBorder(new EmptyBorder(8, 0, 8, 0));

        // Encabezado del menú
        JLabel lblMenu = new JLabel("  NAVEGACIÓN");
        lblMenu.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblMenu.setForeground(new Color(140, 160, 220));
        lblMenu.setBorder(new EmptyBorder(10, 18, 8, 10));
        lblMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblMenu);

        // Separador
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(50, 65, 150));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(6));

        // --- Botones de módulos ---
        sidebar.add(crearBotonMenu("🏠", "Dashboard",          "dashboard"));
        sidebar.add(crearBotonMenu("👥", "Clientes",            "clientes"));
        sidebar.add(crearBotonMenu("🛏", "Habitaciones",        "habitaciones"));
        sidebar.add(crearBotonMenu("📅", "Reservaciones",       "reservaciones"));
        sidebar.add(crearBotonMenu("✅", "Check-In / Check-Out","checkin"));
        sidebar.add(crearBotonMenu("🧾", "Facturación",         "facturacion"));
        sidebar.add(crearBotonMenu("📊", "Reportes",            "reportes"));

        // Módulo de Usuarios: SOLO para administradores
        if (usuarioActual.esAdmin()) {
            sidebar.add(Box.createVerticalStrut(6));
            JSeparator sepAdmin = new JSeparator();
            sepAdmin.setForeground(new Color(50, 65, 150));
            sepAdmin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            sidebar.add(sepAdmin);

            JLabel lblAdmin = new JLabel("  ADMINISTRACIÓN");
            lblAdmin.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblAdmin.setForeground(new Color(140, 160, 220));
            lblAdmin.setBorder(new EmptyBorder(8, 18, 4, 10));
            lblAdmin.setAlignmentX(Component.LEFT_ALIGNMENT);
            sidebar.add(lblAdmin);

            sidebar.add(crearBotonMenu("🔐", "Usuarios",        "usuarios"));
        }

        // Espacio flexible que empuja el texto de versión hacia abajo
        sidebar.add(Box.createVerticalGlue());

        // Versión en la parte inferior
        JLabel lblVersion = new JLabel("  Hotel Sistema  v1.0");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVersion.setForeground(new Color(100, 120, 190));
        lblVersion.setBorder(new EmptyBorder(8, 18, 5, 10));
        lblVersion.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblVersion);

        return sidebar;
    }

    /**
     * Crea un botón del menú lateral con ícono, texto y evento de navegación.
     *
     * @param icono   Emoji o símbolo del módulo
     * @param texto   Nombre del módulo
     * @param modulo  Identificador interno del módulo
     * @return JButton estilizado
     */
    private JButton crearBotonMenu(String icono, String texto, String modulo) {
        JButton btn = new JButton(icono + "  " + texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(new Color(220, 225, 255));
        btn.setBackground(COLOR_SIDEBAR);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(11, 18, 11, 18));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Efectos hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_SIDEBAR_HOVER);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, COLOR_SIDEBAR_BORDE),
                    new EmptyBorder(11, 14, 11, 18)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_SIDEBAR);
                btn.setForeground(new Color(220, 225, 255));
                btn.setBorder(new EmptyBorder(11, 18, 11, 18));
            }
        });

        btn.addActionListener(e -> navegarA(modulo));
        return btn;
    }

    /**
     * Crea el panel central de contenido (donde se cargan los módulos).
     */
    private JPanel crearContenido() {
        panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBackground(COLOR_FONDO);
        return panelContenido;
    }

    // =========================================================
    // NAVEGACIÓN ENTRE MÓDULOS
    // =========================================================

    /**
     * Navega al módulo indicado cargando su panel en el área de contenido.
     *
     * @param modulo Identificador del módulo a mostrar
     */
    private void navegarA(String modulo) {
        panelContenido.removeAll();

        switch (modulo) {
            case "dashboard":
                mostrarDashboard();
                break;
            case "clientes":
                // Módulo 2: Panel de Gestión de Clientes
                ClientesPanel clientesPanel = new ClientesPanel((Frame) SwingUtilities.getWindowAncestor(this));
                panelContenido.add(clientesPanel, BorderLayout.CENTER);
                break;
            case "habitaciones":
                // Módulo 3: Panel de Gestión de Habitaciones
                HabitacionesPanel habitacionesPanel = new HabitacionesPanel((Frame) SwingUtilities.getWindowAncestor(this));
                panelContenido.add(habitacionesPanel, BorderLayout.CENTER);
                break;
            case "reservaciones":
                // Módulo 4: Panel de Gestión de Reservaciones
                ReservacionesPanel reservacionesPanel = new ReservacionesPanel(
                    (Frame) SwingUtilities.getWindowAncestor(this), usuarioActual);
                panelContenido.add(reservacionesPanel, BorderLayout.CENTER);
                break;
            case "checkin":
                // Módulo 5: Check-In / Check-Out
                CheckInOutPanel checkInOutPanel = new CheckInOutPanel(
                    (Frame) SwingUtilities.getWindowAncestor(this), usuarioActual);
                panelContenido.add(checkInOutPanel, BorderLayout.CENTER);
                break;
            case "facturacion":
                // Módulo 6: Facturación
                FacturasPanel facturasPanel = new FacturasPanel(
                    (Frame) SwingUtilities.getWindowAncestor(this));
                panelContenido.add(facturasPanel, BorderLayout.CENTER);
                break;
            case "reportes":
                // Módulo 7: Reportes
                ReportesPanel reportesPanel = new ReportesPanel();
                panelContenido.add(reportesPanel, BorderLayout.CENTER);
                break;
            case "usuarios":
                // Módulo de administración: solo ADMIN
                if (usuarioActual.esAdmin()) {
                    UsuariosPanel usuariosPanel = new UsuariosPanel(
                        (Frame) SwingUtilities.getWindowAncestor(this));
                    panelContenido.add(usuariosPanel, BorderLayout.CENTER);
                } else {
                    mostrarAccesoDenegado();
                }
                break;
        }

        panelContenido.revalidate();
        panelContenido.repaint();
    }

    /**
     * Muestra el Dashboard de bienvenida con tarjetas de acceso rápido.
     */
    private void mostrarDashboard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.CENTER;
        gbc.insets    = new Insets(8, 0, 8, 0);

        // Ícono principal
        JLabel lblIcono = new JLabel("🏨");
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 55));
        panel.add(lblIcono, gbc);

        // Mensaje de bienvenida
        JLabel lblBienvenida = new JLabel(
            "Bienvenido/a, " + usuarioActual.getNombre() + "!"
        );
        lblBienvenida.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblBienvenida.setForeground(COLOR_SIDEBAR);
        panel.add(lblBienvenida, gbc);

        JLabel lblSubtitulo = new JLabel(
            "Selecciona un módulo del menú lateral para comenzar."
        );
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(new Color(100, 110, 140));
        panel.add(lblSubtitulo, gbc);

        // Tarjetas de acceso rápido
        JPanel panelTarjetas = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        panelTarjetas.setOpaque(false);
        panelTarjetas.add(crearTarjetaAcceso("👥", "Clientes",      "clientes"));
        panelTarjetas.add(crearTarjetaAcceso("🛏", "Habitaciones",  "habitaciones"));
        panelTarjetas.add(crearTarjetaAcceso("📅", "Reservar",      "reservaciones"));
        panelTarjetas.add(crearTarjetaAcceso("📊", "Reportes",      "reportes"));
        if (usuarioActual.esAdmin()) {
            panelTarjetas.add(crearTarjetaAcceso("🔐", "Usuarios",  "usuarios"));
        }

        gbc.insets = new Insets(22, 0, 0, 0);
        panel.add(panelTarjetas, gbc);

        panelContenido.add(panel, BorderLayout.CENTER);
    }

    /**
     * Crea una tarjeta clickeable para acceso rápido a un módulo.
     */
    private JPanel crearTarjetaAcceso(String icono, String nombre, String modulo) {
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(20, 28, 20, 28)
        ));
        tarjeta.setPreferredSize(new Dimension(145, 110));
        tarjeta.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNombre = new JLabel(nombre);
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNombre.setForeground(new Color(50, 55, 80));
        lblNombre.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblVer = new JLabel("Abrir →");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVer.setForeground(COLOR_SIDEBAR);
        lblVer.setAlignmentX(Component.CENTER_ALIGNMENT);

        tarjeta.add(lblIcono);
        tarjeta.add(Box.createVerticalStrut(6));
        tarjeta.add(lblNombre);
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(lblVer);

        // Al hacer click en la tarjeta, navegar al módulo
        tarjeta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { navegarA(modulo); }
            @Override
            public void mouseEntered(MouseEvent e) {
                tarjeta.setBackground(new Color(240, 244, 255));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                tarjeta.setBackground(Color.WHITE);
            }
        });

        return tarjeta;
    }

    /**
     * Muestra un panel de "Acceso denegado" cuando un usuario sin permisos
     * intenta acceder a un módulo restringido.
     */
    private void mostrarAccesoDenegado() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.CENTER;
        gbc.insets    = new Insets(8, 0, 8, 0);

        JLabel lblIcono = new JLabel("🚫");
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        panel.add(lblIcono, gbc);

        JLabel lblTitulo = new JLabel("Acceso restringido");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(new Color(198, 40, 40));
        panel.add(lblTitulo, gbc);

        JLabel lblDesc = new JLabel(
            "Este módulo solo está disponible para administradores.");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDesc.setForeground(new Color(100, 110, 140));
        panel.add(lblDesc, gbc);

        panelContenido.add(panel, BorderLayout.CENTER);
    }

    /**
     * Muestra un panel "Próximamente" para módulos en desarrollo.
     *
     * @param titulo      Nombre del módulo
     * @param descripcion Descripción breve
     */
    private void mostrarProximamente(String titulo, String descripcion) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.CENTER;
        gbc.insets    = new Insets(8, 0, 8, 0);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(COLOR_SIDEBAR);
        panel.add(lblTitulo, gbc);

        JLabel lblDesc = new JLabel(descripcion);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDesc.setForeground(new Color(100, 110, 140));
        panel.add(lblDesc, gbc);

        JLabel lblProx = new JLabel("🔧  Módulo en desarrollo - Próximo paso");
        lblProx.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblProx.setForeground(new Color(180, 160, 0));
        gbc.insets = new Insets(20, 0, 0, 0);
        panel.add(lblProx, gbc);

        panelContenido.add(panel, BorderLayout.CENTER);
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    /**
     * Cierra la sesión actual y regresa al Login después de confirmar.
     */
    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Deseas cerrar sesión?",
            "Cerrar Sesión",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (opcion == JOptionPane.YES_OPTION) {
            new LoginForm().setVisible(true);
            dispose();
        }
    }

    /**
     * Pide confirmación antes de cerrar la aplicación.
     */
    private void confirmarSalida() {
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Deseas salir del sistema?",
            "Salir",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}
