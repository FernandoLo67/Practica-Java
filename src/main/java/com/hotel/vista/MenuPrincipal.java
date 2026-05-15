package com.hotel.vista;

import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionDB;
import com.hotel.util.HotelConfig;
import com.hotel.util.Tema;

import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.prefs.Preferences;

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
    // Colores del tema — reasignados en applyTheme() según el modo
    private static Color COLOR_SIDEBAR;
    private static Color COLOR_SIDEBAR_HOVER;
    private static Color COLOR_SIDEBAR_BORDE;
    private static Color COLOR_FONDO;
    private static Color COLOR_HEADER;

    /** Módulo que se muestra actualmente (para restaurarlo tras cambio de tema). */
    private String moduloActivo = "dashboard";

    // =========================================================
    // PREFERENCIAS / ESTADO
    // =========================================================
    private static final Preferences PREFS = Preferences.userNodeForPackage(MenuPrincipal.class);
    private JLabel lblCampana;
    private Timer  timerCampana;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MenuPrincipal(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        // Restaurar preferencia de modo oscuro antes de construir la UI
        boolean oscuroGuardado = PREFS.getBoolean("tema.oscuro", false);
        Tema.setModoOscuro(oscuroGuardado);
        aplicarTema();      // set color fields BEFORE building UI
        initComponents();
        configurarVentana();
        restaurarTamanoYPosicion();
        iniciarCampana();
    }

    /** Aplica los colores del sidebar/header según el modo oscuro/claro actual. */
    private static void aplicarTema() {
        if (Tema.isModoOscuro()) {
            COLOR_SIDEBAR       = new Color(13,  17,  41);
            COLOR_SIDEBAR_HOVER = new Color(26,  31,  66);
            COLOR_SIDEBAR_BORDE = new Color(255, 180, 30);
            COLOR_FONDO         = Tema.COLOR_FONDO;          // reads from Tema (already set)
            COLOR_HEADER        = new Color(22,  25,  43);
        } else {
            COLOR_SIDEBAR       = new Color(26, 35, 126);
            COLOR_SIDEBAR_HOVER = new Color(40, 53, 147);
            COLOR_SIDEBAR_BORDE = new Color(255, 160, 0);
            COLOR_FONDO         = new Color(243, 246, 253);
            COLOR_HEADER        = Color.WHITE;
        }
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

        // Confirmar antes de cerrar y guardar tamaño
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                guardarTamanoYPosicion();
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

        // Atajos globales (registrados después de construir el frame)
        SwingUtilities.invokeLater(() -> {
            // Ctrl+F → búsqueda global
            getRootPane().registerKeyboardAction(
                e -> abrirBusquedaGlobal(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
            // F5 → actualizar panel actual (navega de nuevo al mismo módulo)
            getRootPane().registerKeyboardAction(
                e -> {
                    Component c = panelContenido.getComponentCount() > 0
                        ? panelContenido.getComponent(0) : null;
                    if (c instanceof DashboardPanel) navegarA("dashboard");
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        });
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

        JButton btnBuscar = new JButton("🔍  Buscar...");
        btnBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBuscar.setForeground(new Color(60, 70, 130));
        btnBuscar.setBackground(new Color(235, 239, 255));
        btnBuscar.setOpaque(true);
        btnBuscar.setBorderPainted(true);
        btnBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 200, 235), 1),
            new EmptyBorder(6, 16, 6, 16)
        ));
        btnBuscar.setFocusPainted(false);
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.setToolTipText("Búsqueda global  (Ctrl+F)");
        btnBuscar.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btnBuscar.setBackground(new Color(220, 228, 255));
                btnBuscar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(26, 35, 126), 1),
                    new EmptyBorder(6, 16, 6, 16)
                ));
            }
            @Override public void mouseExited(MouseEvent e) {
                btnBuscar.setBackground(new Color(235, 239, 255));
                btnBuscar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(190, 200, 235), 1),
                    new EmptyBorder(6, 16, 6, 16)
                ));
            }
        });
        btnBuscar.addActionListener(e -> abrirBusquedaGlobal());

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

        // Campana de notificaciones
        lblCampana = new JLabel("🔔");
        lblCampana.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblCampana.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCampana.setToolTipText("Sin check-ins pendientes hoy");
        lblCampana.setBorder(new EmptyBorder(0, 4, 0, 8));
        lblCampana.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navegarA("checkin"); }
        });

        // Botón de modo oscuro/claro
        JButton btnTema = new JButton(Tema.isModoOscuro() ? "☀" : "🌙");
        btnTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btnTema.setForeground(new Color(60, 70, 130));
        btnTema.setBackground(new Color(235, 239, 255));
        btnTema.setOpaque(true);
        btnTema.setBorderPainted(true);
        btnTema.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 200, 235), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTema.setToolTipText(Tema.isModoOscuro() ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
        btnTema.addActionListener(e -> toggleModoOscuro());

        panelDerecho.add(btnBuscar);
        panelDerecho.add(btnTema);
        panelDerecho.add(lblCampana);
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
        sidebar.add(crearBotonMenu("🏷", "Tipos de Habitación", "tipos"));
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

            sidebar.add(crearBotonMenu("🏨", "Datos del Hotel",  "hotel"));
            sidebar.add(crearBotonMenu("🔐", "Usuarios",        "usuarios"));
            sidebar.add(crearBotonMenu("🔍", "Bitácora",        "bitacora"));
        }

        // Espacio flexible que empuja el texto de versión hacia abajo
        sidebar.add(Box.createVerticalGlue());

        // Separador "MI CUENTA" — visible para todos los roles
        JSeparator sepCuenta = new JSeparator();
        sepCuenta.setForeground(new Color(50, 65, 150));
        sepCuenta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sepCuenta);

        JLabel lblCuenta = new JLabel("  MI CUENTA");
        lblCuenta.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCuenta.setForeground(new Color(140, 160, 220));
        lblCuenta.setBorder(new EmptyBorder(8, 18, 4, 10));
        lblCuenta.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblCuenta);

        sidebar.add(crearBotonMenu("🔑", "Cambiar contraseña", "cambiar-password"));

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
        if (!modulo.equals("cambiar-password")) moduloActivo = modulo;
        panelContenido.removeAll();

        switch (modulo) {
            case "dashboard":
                panelContenido.add(new DashboardPanel(usuarioActual), BorderLayout.CENTER);
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
            case "tipos":
                // Módulo: Tipos de Habitación (tarifas base y categorías)
                TiposHabitacionPanel tiposPanel = new TiposHabitacionPanel(
                    (Frame) SwingUtilities.getWindowAncestor(this));
                panelContenido.add(tiposPanel, BorderLayout.CENTER);
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
            case "hotel":
                // Datos del Hotel: solo ADMIN
                if (usuarioActual.esAdmin()) {
                    panelContenido.add(new DatosHotelPanel(), BorderLayout.CENTER);
                } else {
                    mostrarAccesoDenegado();
                }
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
            case "bitacora":
                // Bitácora: solo ADMIN
                if (usuarioActual.esAdmin()) {
                    BitacoraPanel bitacoraPanel = new BitacoraPanel(
                        (Frame) SwingUtilities.getWindowAncestor(this));
                    panelContenido.add(bitacoraPanel, BorderLayout.CENTER);
                } else {
                    mostrarAccesoDenegado();
                }
                break;
            case "cambiar-password":
                // Abre diálogo modal sin alterar el panel de contenido actual
                CambiarPasswordDialog dlg = new CambiarPasswordDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this), usuarioActual);
                dlg.setVisible(true);
                return;   // No revalidar ni repintar el contenido — no cambiamos de panel
        }

        panelContenido.revalidate();
        panelContenido.repaint();
    }

    /**
     * Muestra el Dashboard con datos en tiempo real.
     * Delegado a DashboardPanel.
     */
    private void mostrarDashboard() {
        panelContenido.add(new DashboardPanel(usuarioActual), BorderLayout.CENTER);
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
     * Abre el diálogo de búsqueda global.
     * Al seleccionar un resultado, navega al módulo correspondiente.
     */
    private void abrirBusquedaGlobal() {
        BusquedaGlobalDialog dlg = new BusquedaGlobalDialog(this, this::navegarA);
        dlg.setVisible(true);
    }

    /**
     * Alterna entre modo claro y oscuro, reconstruye toda la UI del frame
     * y navega de vuelta al módulo que estaba activo.
     */
    private void toggleModoOscuro() {
        Tema.toggleModo();
        aplicarTema();

        // Reconstruir la interfaz completamente
        getContentPane().removeAll();
        setLayout(new BorderLayout(0, 0));
        add(crearHeader(),    BorderLayout.NORTH);
        add(crearSidebar(),   BorderLayout.WEST);
        add(crearContenido(), BorderLayout.CENTER);

        // Re-registrar atajos globales
        SwingUtilities.invokeLater(() -> {
            getRootPane().registerKeyboardAction(
                e -> abrirBusquedaGlobal(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
            getRootPane().registerKeyboardAction(
                e -> {
                    Component c = panelContenido.getComponentCount() > 0
                        ? panelContenido.getComponent(0) : null;
                    if (c instanceof DashboardPanel) navegarA("dashboard");
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        });

        // Volver al módulo activo con los nuevos colores
        navegarA(moduloActivo);
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    // =========================================================
    // RECORDAR TAMAÑO Y POSICIÓN
    // =========================================================

    private void restaurarTamanoYPosicion() {
        int w = PREFS.getInt("ventana.ancho",  1100);
        int h = PREFS.getInt("ventana.alto",   680);
        int x = PREFS.getInt("ventana.x",      -1);
        int y = PREFS.getInt("ventana.y",      -1);
        setSize(Math.max(900, w), Math.max(580, h));
        if (x >= 0 && y >= 0) setLocation(x, y);
        else setLocationRelativeTo(null);

        boolean maximizado = PREFS.getBoolean("ventana.maximizado", false);
        if (maximizado) setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void guardarTamanoYPosicion() {
        PREFS.putBoolean("tema.oscuro", Tema.isModoOscuro());
        boolean maximizado = (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        PREFS.putBoolean("ventana.maximizado", maximizado);
        if (!maximizado) {
            PREFS.putInt("ventana.ancho", getWidth());
            PREFS.putInt("ventana.alto",  getHeight());
            PREFS.putInt("ventana.x",     getX());
            PREFS.putInt("ventana.y",     getY());
        }
    }

    // =========================================================
    // CAMPANA DE NOTIFICACIONES
    // =========================================================

    /**
     * Inicia un Timer que verifica cada 60 seg cuántos check-ins
     * hay pendientes hoy y actualiza el icono de la campana.
     */
    private void iniciarCampana() {
        actualizarCampana(); // primera vez inmediata
        timerCampana = new Timer(60_000, e -> actualizarCampana());
        timerCampana.start();
    }

    private void actualizarCampana() {
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                try (Connection conn = ConexionDB.getConexion();
                     PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM reservaciones " +
                         "WHERE estado = 'CONFIRMADA' AND fecha_checkin = CURDATE()");
                     ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                } catch (Exception ex) { return 0; }
            }
            @Override protected void done() {
                try {
                    int pendientes = get();
                    if (pendientes > 0) {
                        lblCampana.setText("🔔 " + pendientes);
                        lblCampana.setForeground(new Color(220, 100, 0));
                        lblCampana.setToolTipText(pendientes + " check-in(s) pendiente(s) hoy — clic para ir");
                    } else {
                        lblCampana.setText("🔔");
                        lblCampana.setForeground(new Color(90, 100, 130));
                        lblCampana.setToolTipText("Sin check-ins pendientes hoy");
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

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
            guardarTamanoYPosicion();
            if (timerCampana != null) timerCampana.stop();
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
