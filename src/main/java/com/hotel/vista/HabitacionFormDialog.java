package com.hotel.vista;

import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.modelo.Habitacion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diálogo modal para ver y editar una habitación.
 * Permite cambiar el estado y la descripción.
 *
 * @author Fernando
 * @version 1.0
 */
public class HabitacionFormDialog extends JDialog {

    private final Habitacion habitacion;
    private final HabitacionDAOImpl dao;
    private boolean guardadoExitoso = false;

    // Componentes
    private JComboBox<String> cmbEstado;
    private JTextField        txtPrecioEspecial;
    private JTextArea         txtDescripcion;

    // Colores
    private static final Color COLOR_PRIMARIO = new Color(26, 35, 126);
    private static final Color COLOR_TEXTO    = new Color(33, 33, 33);

    public HabitacionFormDialog(Frame padre, Habitacion habitacion) {
        super(padre, "Editar Habitación", true);
        this.habitacion = habitacion;
        this.dao        = new HabitacionDAOImpl();
        initComponents();
        configurarDialogo();
        cargarDatos();
    }

    private void configurarDialogo() {
        setSize(440, 500);
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBackground(Color.WHITE);

        // --- Encabezado ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_PRIMARIO);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblTitulo = new JLabel("🛏  Habitación " + habitacion.getNumero());
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Piso " + habitacion.getPiso() +
                "  |  " + habitacion.getTipo().getNombre() +
                "  |  Q" + String.format("%.2f", habitacion.getPrecioNoche()) + "/noche");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(180, 195, 255));

        JPanel panelTextos = new JPanel(new GridLayout(2, 1, 0, 3));
        panelTextos.setOpaque(false);
        panelTextos.add(lblTitulo);
        panelTextos.add(lblSub);
        header.add(panelTextos, BorderLayout.CENTER);

        // --- Formulario ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 6, 0);

        // Estado
        JLabel lblEstado = new JLabel("Estado");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblEstado.setForeground(COLOR_TEXTO);
        form.add(lblEstado, g);

        g.gridy++;
        cmbEstado = new JComboBox<>(new String[]{
            "DISPONIBLE", "OCUPADA", "RESERVADA", "MANTENIMIENTO"
        });
        cmbEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbEstado.setPreferredSize(new Dimension(0, 36));
        cmbEstado.setBackground(Color.WHITE);
        form.add(cmbEstado, g);

        // Precio especial
        g.gridy++; g.insets = new Insets(14, 0, 6, 0);
        JLabel lblPrecio = new JLabel("Precio especial (Q)");
        lblPrecio.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrecio.setForeground(COLOR_TEXTO);
        form.add(lblPrecio, g);

        g.gridy++; g.insets = new Insets(0, 0, 4, 0);
        txtPrecioEspecial = new JTextField();
        txtPrecioEspecial.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPrecioEspecial.setPreferredSize(new Dimension(0, 36));
        txtPrecioEspecial.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        txtPrecioEspecial.setToolTipText(
            "Deja en blanco para usar el precio base del tipo (" +
            String.format("Q %.2f", habitacion.getTipo().getPrecioBase()) + ")");
        form.add(txtPrecioEspecial, g);

        // Descripción
        g.gridy++; g.insets = new Insets(10, 0, 6, 0);
        JLabel lblDesc = new JLabel("Descripción");
        lblDesc.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDesc.setForeground(COLOR_TEXTO);
        form.add(lblDesc, g);

        g.gridy++; g.weighty = 1.0;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 0);
        txtDescripcion = new JTextArea(4, 20);
        txtDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        txtDescripcion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        form.add(new JScrollPane(txtDescripcion), g);

        // --- Botones ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panelBotones.setBackground(new Color(245, 247, 255));
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancelar.setForeground(new Color(80, 85, 110));
        btnCancelar.setBackground(new Color(225, 228, 240));
        btnCancelar.setOpaque(true);
        btnCancelar.setBorderPainted(false);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setPreferredSize(new Dimension(100, 36));
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(COLOR_PRIMARIO);
        btnGuardar.setOpaque(true);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setPreferredSize(new Dimension(100, 36));
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardar());

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);

        panelPrincipal.add(header,        BorderLayout.NORTH);
        panelPrincipal.add(form,          BorderLayout.CENTER);
        panelPrincipal.add(panelBotones,  BorderLayout.SOUTH);

        setContentPane(panelPrincipal);
    }

    private void cargarDatos() {
        cmbEstado.setSelectedItem(habitacion.getEstado());
        txtDescripcion.setText(habitacion.getDescripcion() != null ? habitacion.getDescripcion() : "");
        if (habitacion.getPrecioEspecial() != null) {
            txtPrecioEspecial.setText(String.format("%.2f", habitacion.getPrecioEspecial()));
        }
    }

    private void guardar() {
        String nuevoEstado = (String) cmbEstado.getSelectedItem();
        String nuevaDesc   = txtDescripcion.getText().trim();
        String precioStr   = txtPrecioEspecial.getText().trim();

        // Validar precio especial (opcional)
        Double precioEspecial = null;
        if (!precioStr.isEmpty()) {
            try {
                precioEspecial = Double.parseDouble(precioStr.replace(",", "."));
                if (precioEspecial <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "El precio especial debe ser un número positivo, o déjalo en blanco.",
                    "Error", JOptionPane.WARNING_MESSAGE);
                txtPrecioEspecial.requestFocus();
                return;
            }
        }

        habitacion.setEstado(nuevoEstado);
        habitacion.setDescripcion(nuevaDesc.isEmpty() ? null : nuevaDesc);
        habitacion.setPrecioEspecial(precioEspecial);

        boolean ok = dao.actualizar(habitacion);
        if (ok) {
            guardadoExitoso = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                "No se pudo guardar los cambios.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }
}
