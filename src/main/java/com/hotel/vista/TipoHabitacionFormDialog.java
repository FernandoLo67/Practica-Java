package com.hotel.vista;

import com.hotel.dao.impl.TipoHabitacionDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.BitacoraService;
import com.hotel.util.Validaciones;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diálogo modal para crear y editar tipos de habitación.
 *
 * @author Fernando
 * @version 1.0
 */
public class TipoHabitacionFormDialog extends JDialog {

    private JTextField  txtNombre;
    private JTextField  txtPrecio;
    private JSpinner    spCapacidad;
    private JTextArea   txtDescripcion;

    private final TipoHabitacion tipoEditar;
    private final TipoHabitacionDAOImpl dao;
    private boolean guardadoExitoso = false;

    private static final Color COLOR_PRIMARIO = new Color(26, 35, 126);
    private static final Color COLOR_FONDO    = new Color(248, 250, 255);

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public TipoHabitacionFormDialog(Frame padre, TipoHabitacion tipoEditar) {
        super(padre, true);
        this.tipoEditar = tipoEditar;
        this.dao        = new TipoHabitacionDAOImpl();
        initComponents();
        setTitle(tipoEditar == null ? "Nuevo Tipo de Habitación" : "Editar Tipo de Habitación");
        setSize(440, 420);
        setLocationRelativeTo(padre);
        setResizable(false);
        if (tipoEditar != null) cargarDatos();
    }

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
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        String tit = tipoEditar == null ? "🏷  Nuevo Tipo de Habitación"
                                        : "✏️  Editar: " + tipoEditar.getNombre();
        JLabel lbl = new JLabel(tit);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JPanel crearFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_FONDO);
        p.setBorder(new EmptyBorder(18, 24, 10, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 5, 5, 5);

        // Nombre
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        p.add(etiqueta("Nombre *"), g);
        g.gridx = 1; g.weightx = 1;
        txtNombre = campo();
        p.add(txtNombre, g);

        // Precio base
        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        p.add(etiqueta("Precio base (Q) *"), g);
        g.gridx = 1; g.weightx = 1;
        txtPrecio = campo();
        txtPrecio.setToolTipText("Precio por noche en quetzales");
        p.add(txtPrecio, g);

        // Capacidad
        g.gridx = 0; g.gridy = 2; g.weightx = 0;
        p.add(etiqueta("Capacidad *"), g);
        g.gridx = 1; g.weightx = 1;
        spCapacidad = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        spCapacidad.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spCapacidad.setPreferredSize(new Dimension(0, 34));
        p.add(spCapacidad, g);

        // Descripción
        g.gridx = 0; g.gridy = 3; g.weightx = 0; g.anchor = GridBagConstraints.NORTH;
        p.add(etiqueta("Descripción"), g);
        g.gridx = 1; g.weightx = 1; g.anchor = GridBagConstraints.CENTER;
        txtDescripcion = new JTextArea(4, 1);
        txtDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        txtDescripcion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        p.add(new JScrollPane(txtDescripcion), g);

        // Nota
        g.gridx = 0; g.gridy = 4; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST;
        JLabel nota = new JLabel("  * Campos obligatorios");
        nota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        nota.setForeground(Color.GRAY);
        p.add(nota, g);

        return p;
    }

    private JPanel crearBotones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancelar.addActionListener(e -> dispose());

        JButton btnGuardar = new JButton(tipoEditar == null ? "Crear tipo" : "Guardar cambios");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(COLOR_PRIMARIO);
        btnGuardar.setOpaque(true);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setFocusPainted(false);
        btnGuardar.addActionListener(e -> guardar());

        p.add(btnCancelar);
        p.add(btnGuardar);
        return p;
    }

    // =========================================================
    // DATOS
    // =========================================================

    private void cargarDatos() {
        txtNombre.setText(tipoEditar.getNombre());
        txtPrecio.setText(String.format("%.2f", tipoEditar.getPrecioBase()));
        spCapacidad.setValue(tipoEditar.getCapacidad());
        if (tipoEditar.getDescripcion() != null)
            txtDescripcion.setText(tipoEditar.getDescripcion());
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String precioStr = txtPrecio.getText().trim();

        String errNombre = Validaciones.validarTexto("Nombre", nombre, 2, 50);
        if (errNombre != null) {
            error(errNombre); txtNombre.requestFocus(); return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr.replace(",", "."));
            if (precio <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            error("El precio debe ser un número positivo (ej: 80.00)");
            txtPrecio.requestFocus();
            return;
        }

        int capacidad = (int) spCapacidad.getValue();

        TipoHabitacion t = tipoEditar != null ? tipoEditar : new TipoHabitacion();
        t.setNombre     (Validaciones.capitalizar(nombre));
        t.setPrecioBase (precio);
        t.setCapacidad  (capacidad);
        t.setDescripcion(txtDescripcion.getText().trim().isEmpty() ? null
                         : txtDescripcion.getText().trim());

        boolean exito = tipoEditar == null ? dao.guardar(t) : dao.actualizar(t);

        if (exito) {
            guardadoExitoso = true;
            String accion = tipoEditar == null ? Bitacora.ACCION_CREAR : Bitacora.ACCION_EDITAR;
            BitacoraService.log(accion, Bitacora.MODULO_HABITACIONES,
                "Tipo habitación " + (tipoEditar == null ? "creado" : "editado")
                + ": " + t.getNombre() + " — Q" + String.format("%.2f", precio)
                + " — " + capacidad + " pers.");
            JOptionPane.showMessageDialog(this,
                tipoEditar == null ? "Tipo creado exitosamente." : "Tipo actualizado exitosamente.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            error("No se pudo guardar. Verifica los datos e intenta de nuevo.");
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(50, 55, 80));
        l.setPreferredSize(new Dimension(130, 34));
        return l;
    }

    private JTextField campo() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(0, 34));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return f;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error de validación",
            JOptionPane.WARNING_MESSAGE);
    }

    public boolean isGuardadoExitoso() { return guardadoExitoso; }
}
