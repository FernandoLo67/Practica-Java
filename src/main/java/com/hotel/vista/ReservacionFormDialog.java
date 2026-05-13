package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.modelo.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Diálogo modal para crear o editar una reservación.
 *
 * Permite seleccionar cliente, habitación disponible,
 * fechas de check-in y check-out, y observaciones.
 *
 * @author Fernando
 * @version 1.0
 */
public class ReservacionFormDialog extends JDialog {

    private final Reservacion      reservacion;
    private final Usuario          usuarioActual;
    private final ReservacionDAOImpl reservacionDAO;
    private final ClienteDAOImpl     clienteDAO;
    private final HabitacionDAOImpl  habitacionDAO;
    private boolean guardadoExitoso = false;

    // Componentes
    private JComboBox<Cliente>       cmbCliente;
    private JComboBox<Habitacion>    cmbHabitacion;
    private JFormattedTextField      txtCheckin;
    private JFormattedTextField      txtCheckout;
    private JComboBox<String>      cmbEstado;
    private JTextArea              txtObservaciones;
    private JLabel                 lblPrecioInfo;
    private JLabel                 lblMensaje;

    private static final Color COLOR_PRIMARIO = new Color(26, 35, 126);
    private static final Color COLOR_ERROR    = new Color(198, 40, 40);
    private static final Color COLOR_EXITO    = new Color(46, 125, 50);
    private static final Color COLOR_TEXTO    = new Color(33, 33, 33);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReservacionFormDialog(Frame padre, Reservacion reservacion, Usuario usuarioActual) {
        super(padre, reservacion == null ? "Nueva Reservación" : "Editar Reservación", true);
        this.reservacion    = reservacion == null ? new Reservacion() : reservacion;
        this.usuarioActual  = usuarioActual;
        this.reservacionDAO = new ReservacionDAOImpl();
        this.clienteDAO     = new ClienteDAOImpl();
        this.habitacionDAO  = new HabitacionDAOImpl();
        initComponents();
        configurarDialogo();
        cargarCombos();
        if (reservacion != null) cargarDatos();
    }

    private void configurarDialogo() {
        setSize(500, 560);
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private void initComponents() {
        JPanel principal = new JPanel(new BorderLayout());
        principal.setBackground(Color.WHITE);

        // --- Header ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_PRIMARIO);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        boolean esNueva = reservacion.getId() == 0;
        JLabel lblTitulo = new JLabel(esNueva ? "📅  Nueva Reservación" : "📅  Editar Reservación");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(esNueva
            ? "Completa los datos para registrar la reservación"
            : "Modifica los datos de la reservación #" + reservacion.getId());
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(180, 195, 255));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 3));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblSub);
        header.add(textos, BorderLayout.CENTER);

        // --- Formulario ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 24, 10, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        // Cliente
        g.insets = new Insets(0, 0, 4, 0);
        form.add(etiqueta("Cliente *"), g);
        g.gridy++; g.insets = new Insets(0, 0, 10, 0);
        cmbCliente = new JComboBox<>();
        cmbCliente.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbCliente.setPreferredSize(new Dimension(0, 36));
        cmbCliente.setBackground(Color.WHITE);
        form.add(cmbCliente, g);

        // Habitación
        g.gridy++; g.insets = new Insets(0, 0, 4, 0);
        form.add(etiqueta("Habitación *"), g);
        g.gridy++; g.insets = new Insets(0, 0, 4, 0);
        cmbHabitacion = new JComboBox<>();
        cmbHabitacion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbHabitacion.setPreferredSize(new Dimension(0, 36));
        cmbHabitacion.setBackground(Color.WHITE);
        cmbHabitacion.addActionListener(e -> actualizarInfoPrecio());
        form.add(cmbHabitacion, g);

        // Info precio
        g.gridy++; g.insets = new Insets(2, 0, 10, 0);
        lblPrecioInfo = new JLabel(" ");
        lblPrecioInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPrecioInfo.setForeground(new Color(80, 100, 180));
        form.add(lblPrecioInfo, g);

        // Fechas en dos columnas
        g.gridy++; g.insets = new Insets(0, 0, 4, 0);
        JPanel panelFechasLabel = new JPanel(new GridLayout(1, 2, 10, 0));
        panelFechasLabel.setOpaque(false);
        panelFechasLabel.add(etiqueta("Check-In * (dd/mm/aaaa)"));
        panelFechasLabel.add(etiqueta("Check-Out * (dd/mm/aaaa)"));
        form.add(panelFechasLabel, g);

        g.gridy++; g.insets = new Insets(0, 0, 10, 0);
        JPanel panelFechas = new JPanel(new GridLayout(1, 2, 10, 0));
        panelFechas.setOpaque(false);

        txtCheckin  = campoFecha();
        txtCheckout = campoFecha();
        txtCheckin.addActionListener(e -> actualizarInfoPrecio());
        txtCheckout.addActionListener(e -> actualizarInfoPrecio());
        txtCheckin.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { actualizarInfoPrecio(); }
        });
        txtCheckout.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { actualizarInfoPrecio(); }
        });

        panelFechas.add(txtCheckin);
        panelFechas.add(txtCheckout);
        form.add(panelFechas, g);

        // Estado (solo al editar)
        if (reservacion.getId() != 0) {
            g.gridy++; g.insets = new Insets(0, 0, 4, 0);
            form.add(etiqueta("Estado"), g);
            g.gridy++; g.insets = new Insets(0, 0, 10, 0);
            cmbEstado = new JComboBox<>(new String[]{
                "PENDIENTE", "CONFIRMADA", "CHECKIN", "CHECKOUT", "CANCELADA"
            });
            cmbEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cmbEstado.setPreferredSize(new Dimension(0, 36));
            cmbEstado.setBackground(Color.WHITE);
            form.add(cmbEstado, g);
        }

        // Observaciones
        g.gridy++; g.insets = new Insets(0, 0, 4, 0);
        form.add(etiqueta("Observaciones"), g);
        g.gridy++; g.weighty = 1.0; g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 0);
        txtObservaciones = new JTextArea(3, 20);
        txtObservaciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        txtObservaciones.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        form.add(new JScrollPane(txtObservaciones), g);

        // Mensaje
        g.gridy++; g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 0, 0, 0);
        lblMensaje = new JLabel(" ", SwingConstants.CENTER);
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMensaje.setForeground(COLOR_ERROR);
        form.add(lblMensaje, g);

        // --- Botones ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panelBotones.setBackground(new Color(245, 247, 255));
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)));

        JButton btnCancelar = boton("Cancelar", new Color(225, 228, 240), new Color(80, 85, 110));
        btnCancelar.addActionListener(e -> dispose());

        JButton btnGuardar = boton("Guardar", COLOR_PRIMARIO, Color.WHITE);
        btnGuardar.addActionListener(e -> guardar());

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);

        principal.add(header,       BorderLayout.NORTH);
        principal.add(form,         BorderLayout.CENTER);
        principal.add(panelBotones, BorderLayout.SOUTH);

        setContentPane(principal);
    }

    // =========================================================
    // HELPERS DE ESTILO
    // =========================================================

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(COLOR_TEXTO);
        return l;
    }

    private JTextField campoTexto() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(0, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return f;
    }

    /**
     * Crea un campo de texto con máscara dd/MM/yyyy.
     * MaskFormatter garantiza que solo se puedan ingresar dígitos
     * en las posiciones correctas (día, mes, año) y pone '/' automáticamente.
     */
    private JFormattedTextField campoFecha() {
        JFormattedTextField f;
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            f = new JFormattedTextField(mask);
        } catch (ParseException e) {
            // No debería ocurrir con un patrón válido
            f = new JFormattedTextField();
        }
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(0, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        f.setFocusLostBehavior(JFormattedTextField.PERSIST);
        f.setToolTipText("Formato: dd/mm/aaaa");
        return f;
    }

    /**
     * Extrae el texto del campo de fecha limpiando el placeholder '_'.
     * Si el campo está incompleto retorna cadena vacía.
     */
    private String textoFecha(JFormattedTextField campo) {
        String txt = campo.getText().replace("_", "").trim();
        // Un campo completo tiene exactamente 10 chars: dd/MM/yyyy
        return txt.length() == 10 ? txt : "";
    }

    private JButton boton(String texto, Color fondo, Color fuente) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(fuente);
        b.setBackground(fondo);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(100, 36));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // =========================================================
    // CARGA DE DATOS
    // =========================================================

    private void cargarCombos() {
        // Clientes
        List<Cliente> clientes = clienteDAO.listarTodos();
        for (Cliente c : clientes) cmbCliente.addItem(c);

        // Habitaciones disponibles (o todas si es edición)
        List<Habitacion> habitaciones = reservacion.getId() == 0
            ? habitacionDAO.listarDisponibles()
            : habitacionDAO.listarTodas();
        for (Habitacion h : habitaciones) cmbHabitacion.addItem(h);
    }

    private void cargarDatos() {
        // Seleccionar cliente
        for (int i = 0; i < cmbCliente.getItemCount(); i++) {
            if (cmbCliente.getItemAt(i).getId() == reservacion.getCliente().getId()) {
                cmbCliente.setSelectedIndex(i);
                break;
            }
        }
        // Seleccionar habitación
        for (int i = 0; i < cmbHabitacion.getItemCount(); i++) {
            if (cmbHabitacion.getItemAt(i).getId() == reservacion.getHabitacion().getId()) {
                cmbHabitacion.setSelectedIndex(i);
                break;
            }
        }
        // Fechas
        if (reservacion.getFechaCheckin() != null)
            txtCheckin.setText(reservacion.getFechaCheckin().toLocalDate().format(FMT));
        if (reservacion.getFechaCheckout() != null)
            txtCheckout.setText(reservacion.getFechaCheckout().toLocalDate().format(FMT));
        // Estado
        if (cmbEstado != null)
            cmbEstado.setSelectedItem(reservacion.getEstado());
        // Observaciones
        if (reservacion.getObservaciones() != null)
            txtObservaciones.setText(reservacion.getObservaciones());

        actualizarInfoPrecio();
    }

    private void actualizarInfoPrecio() {
        Habitacion hab = (Habitacion) cmbHabitacion.getSelectedItem();
        if (hab == null) { lblPrecioInfo.setText(" "); return; }

        try {
            String tci = textoFecha(txtCheckin);
            String tco = textoFecha(txtCheckout);
            if (tci.isEmpty() || tco.isEmpty()) throw new DateTimeParseException("", "", 0);
            LocalDate ci = LocalDate.parse(tci, FMT);
            LocalDate co = LocalDate.parse(tco, FMT);
            long noches = java.time.temporal.ChronoUnit.DAYS.between(ci, co);
            if (noches > 0) {
                double total = noches * hab.getPrecioNoche();
                lblPrecioInfo.setText(String.format(
                    "Q%.2f/noche  ×  %d noches  =  Q%.2f",
                    hab.getPrecioNoche(), noches, total));
                lblPrecioInfo.setForeground(COLOR_EXITO);
            } else {
                lblPrecioInfo.setText("Q" + String.format("%.2f", hab.getPrecioNoche()) + "/noche");
                lblPrecioInfo.setForeground(new Color(80, 100, 180));
            }
        } catch (DateTimeParseException ex) {
            lblPrecioInfo.setText("Q" + String.format("%.2f", hab.getPrecioNoche()) + "/noche");
            lblPrecioInfo.setForeground(new Color(80, 100, 180));
        }
    }

    // =========================================================
    // GUARDAR
    // =========================================================

    private void guardar() {
        // Validaciones
        if (cmbCliente.getSelectedItem() == null) {
            lblMensaje.setText("⚠  Selecciona un cliente");
            return;
        }
        if (cmbHabitacion.getSelectedItem() == null) {
            lblMensaje.setText("⚠  Selecciona una habitación");
            return;
        }

        LocalDate ci, co;
        try {
            String tci = textoFecha(txtCheckin);
            String tco = textoFecha(txtCheckout);
            if (tci.isEmpty() || tco.isEmpty()) {
                lblMensaje.setText("⚠  Ingresa las fechas completas (dd/mm/aaaa)");
                return;
            }
            ci = LocalDate.parse(tci, FMT);
            co = LocalDate.parse(tco, FMT);
        } catch (DateTimeParseException ex) {
            lblMensaje.setText("⚠  Fechas inválidas. Usa dd/mm/aaaa");
            return;
        }

        if (!co.isAfter(ci)) {
            lblMensaje.setText("⚠  El checkout debe ser posterior al check-in");
            return;
        }

        if (ci.isBefore(LocalDate.now()) && reservacion.getId() == 0) {
            lblMensaje.setText("⚠  La fecha de check-in no puede ser en el pasado");
            return;
        }

        Cliente    cliente    = (Cliente)    cmbCliente.getSelectedItem();
        Habitacion habitacion = (Habitacion) cmbHabitacion.getSelectedItem();
        Date fechaCI = Date.valueOf(ci);
        Date fechaCO = Date.valueOf(co);

        // Verificar disponibilidad
        boolean disponible = reservacionDAO.habitacionDisponible(
            habitacion.getId(), fechaCI, fechaCO, reservacion.getId()
        );
        if (!disponible) {
            lblMensaje.setText("⚠  La habitación no está disponible en esas fechas");
            return;
        }

        // Armar objeto
        reservacion.setCliente       (cliente);
        reservacion.setHabitacion    (habitacion);
        reservacion.setFechaCheckin  (fechaCI);
        reservacion.setFechaCheckout (fechaCO);
        reservacion.setObservaciones (txtObservaciones.getText().trim());
        if (cmbEstado != null)
            reservacion.setEstado((String) cmbEstado.getSelectedItem());
        else
            reservacion.setEstado(Reservacion.ESTADO_PENDIENTE);

        if (reservacion.getUsuarioRegistro() == null)
            reservacion.setUsuarioRegistro(usuarioActual);

        boolean ok = reservacion.getId() == 0
            ? reservacionDAO.guardar(reservacion)
            : reservacionDAO.actualizar(reservacion);

        if (ok) {
            guardadoExitoso = true;
            dispose();
        } else {
            lblMensaje.setText("✗  Error al guardar. Intenta de nuevo.");
        }
    }

    public boolean isGuardadoExitoso() { return guardadoExitoso; }
}
