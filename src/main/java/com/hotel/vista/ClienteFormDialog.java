package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.Cliente;
import com.hotel.util.BitacoraService;
import com.hotel.util.Validaciones;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Ventana modal (diálogo) para REGISTRAR y EDITAR clientes.
 *
 * Es un JDialog (no un JFrame) porque aparece encima de la ventana principal
 * y hay que cerrarla antes de continuar — esto se llama ventana MODAL.
 *
 * Se usa para DOS operaciones:
 *   - NUEVO:  clienteEditar = null  → se crea un cliente nuevo
 *   - EDITAR: clienteEditar != null → se actualizan sus datos
 *
 * CÓMO USARLO desde otro panel:
 *   ClienteFormDialog dialog = new ClienteFormDialog(ventanaPadre, null);      // Nuevo
 *   ClienteFormDialog dialog = new ClienteFormDialog(ventanaPadre, cliente);   // Editar
 *   dialog.setVisible(true);
 *   if (dialog.isGuardadoExitoso()) { // recargar la tabla }
 *
 * @author Fernando
 * @version 1.0
 */
public class ClienteFormDialog extends JDialog {

    // =========================================================
    // CAMPOS DEL FORMULARIO
    // =========================================================
    private JTextField    txtNombre;
    private JTextField    txtApellido;
    private JComboBox<String> cmbTipoDocumento;
    private JTextField    txtDocumento;
    private JTextField    txtTelefono;
    private JTextField    txtEmail;
    private JTextField    txtNacionalidad;
    private JTextArea     txtDireccion;
    private JCheckBox     chkActivo;
    private JButton       btnGuardar;
    private JButton       btnCancelar;

    // =========================================================
    // DATOS Y ESTADO
    // =========================================================

    /** El cliente a editar (null si es un registro nuevo) */
    private final Cliente clienteEditar;

    /** DAO para guardar/actualizar en la base de datos */
    private final ClienteDAOImpl clienteDAO;

    /** Indica si el guardado fue exitoso (para que el panel padre lo sepa) */
    private boolean guardadoExitoso = false;

    // =========================================================
    // COLORES
    // =========================================================
    private static final Color COLOR_PRIMARIO = new Color(26, 35, 126);
    private static final Color COLOR_FONDO    = new Color(248, 250, 255);

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * @param padre         Ventana padre (para centrar el diálogo)
     * @param clienteEditar Cliente a editar, o null para crear uno nuevo
     */
    public ClienteFormDialog(Frame padre, Cliente clienteEditar) {
        super(padre, true); // true = modal (bloquea la ventana padre)
        this.clienteEditar = clienteEditar;
        this.clienteDAO    = new ClienteDAOImpl();
        initComponents();
        configurarDialog();

        // Si estamos editando, cargar los datos actuales en los campos
        if (clienteEditar != null) {
            cargarDatos(clienteEditar);
        }
    }

    // =========================================================
    // CONFIGURACIÓN
    // =========================================================

    private void configurarDialog() {
        String titulo = (clienteEditar == null) ? "Registrar Nuevo Cliente" : "Editar Cliente";
        setTitle(titulo);
        setSize(500, 560);
        setLocationRelativeTo(getOwner()); // Centra sobre la ventana padre
        setResizable(false);
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA INTERFAZ
    // =========================================================

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO);

        add(crearHeader(),     BorderLayout.NORTH);
        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotones(),    BorderLayout.SOUTH);
    }

    /**
     * Encabezado superior con título e ícono.
     */
    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_PRIMARIO);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        String titulo = (clienteEditar == null) ? "👤  Nuevo Cliente" : "✏️  Editar Cliente";
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        header.add(lblTitulo, BorderLayout.WEST);
        return header;
    }

    /**
     * Panel central con todos los campos del formulario.
     * Usa GridBagLayout para alinear etiquetas y campos en columnas.
     */
    private JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(new EmptyBorder(20, 25, 10, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // ---- Fila 0: Nombre ----
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(crearEtiqueta("Nombre *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtNombre = crearCampo("Ej: Juan Carlos");
        panel.add(txtNombre, gbc);

        // ---- Fila 1: Apellido ----
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(crearEtiqueta("Apellido *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtApellido = crearCampo("Ej: García López");
        panel.add(txtApellido, gbc);

        // ---- Fila 2: Tipo Documento ----
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(crearEtiqueta("Tipo Doc. *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        cmbTipoDocumento = new JComboBox<>(new String[]{"DPI", "PASAPORTE", "CEDULA"});
        cmbTipoDocumento.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbTipoDocumento.setPreferredSize(new Dimension(0, 35));
        panel.add(cmbTipoDocumento, gbc);

        // ---- Fila 3: Número Documento ----
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(crearEtiqueta("Documento *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtDocumento = crearCampo("Ej: 12345678");
        panel.add(txtDocumento, gbc);

        // ---- Fila 4: Teléfono ----
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        panel.add(crearEtiqueta("Teléfono"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtTelefono = crearCampo("Ej: 999-123-456");
        panel.add(txtTelefono, gbc);

        // ---- Fila 5: Email ----
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        panel.add(crearEtiqueta("Email"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtEmail = crearCampo("Ej: correo@ejemplo.com");
        panel.add(txtEmail, gbc);

        // ---- Fila 6: Nacionalidad ----
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        panel.add(crearEtiqueta("Nacionalidad"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtNacionalidad = crearCampo("Ej: Peruana");
        panel.add(txtNacionalidad, gbc);

        // ---- Fila 7: Dirección (área de texto) ----
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTH;
        panel.add(crearEtiqueta("Dirección"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.anchor = GridBagConstraints.CENTER;
        txtDireccion = new JTextArea(3, 1);
        txtDireccion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDireccion.setLineWrap(true);
        txtDireccion.setWrapStyleWord(true);
        txtDireccion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        panel.add(new JScrollPane(txtDireccion), gbc);

        // ---- Fila 8: Activo (solo en edición) ----
        if (clienteEditar != null) {
            gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
            chkActivo = new JCheckBox("  Cliente activo");
            chkActivo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            chkActivo.setBackground(COLOR_FONDO);
            chkActivo.setSelected(true);
            panel.add(chkActivo, gbc);
            gbc.gridy = 9;
        } else {
            gbc.gridy = 8;
        }

        // Nota de campos obligatorios
        gbc.gridx = 0; gbc.gridwidth = 2;
        JLabel lblNota = new JLabel("  * Campos obligatorios");
        lblNota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblNota.setForeground(Color.GRAY);
        panel.add(lblNota, gbc);

        return panel;
    }

    /**
     * Panel inferior con los botones Guardar y Cancelar.
     */
    private JPanel crearBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setBackground(new Color(240, 242, 250));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancelar.setForeground(new Color(80, 80, 100));
        btnCancelar.setBackground(Color.WHITE);
        btnCancelar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 215), 1),
            new EmptyBorder(8, 20, 8, 20)
        ));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new JButton(clienteEditar == null ? "  Registrar Cliente" : "  Guardar Cambios");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(COLOR_PRIMARIO);
        btnGuardar.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardar());

        // Enter en cualquier campo dispara el guardado
        KeyAdapter enterGuardar = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) guardar();
            }
        };
        txtNombre.addKeyListener(enterGuardar);
        txtDocumento.addKeyListener(enterGuardar);

        panel.add(btnCancelar);
        panel.add(btnGuardar);
        return panel;
    }

    // =========================================================
    // HELPERS DE ESTILO
    // =========================================================

    private JLabel crearEtiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(50, 55, 80));
        label.setPreferredSize(new Dimension(105, 35));
        return label;
    }

    private JTextField crearCampo(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        // Placeholder visual (texto gris que desaparece al escribir)
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
        return field;
    }

    /** Obtiene el texto de un campo, ignorando el placeholder. */
    private String getValorCampo(JTextField field, String placeholder) {
        String val = field.getText().trim();
        return val.equals(placeholder) ? "" : val;
    }

    // =========================================================
    // LÓGICA
    // =========================================================

    /**
     * Carga los datos de un cliente existente en los campos del formulario.
     * Se llama solo cuando estamos en modo EDITAR.
     */
    private void cargarDatos(Cliente c) {
        txtNombre.setText(c.getNombre());           txtNombre.setForeground(Color.BLACK);
        txtApellido.setText(c.getApellido());       txtApellido.setForeground(Color.BLACK);
        txtDocumento.setText(c.getDocumento());     txtDocumento.setForeground(Color.BLACK);
        txtTelefono.setText(c.getTelefono() != null ? c.getTelefono() : "");
        txtTelefono.setForeground(Color.BLACK);
        txtEmail.setText(c.getEmail() != null ? c.getEmail() : "");
        txtEmail.setForeground(Color.BLACK);
        txtNacionalidad.setText(c.getNacionalidad() != null ? c.getNacionalidad() : "");
        txtNacionalidad.setForeground(Color.BLACK);
        txtDireccion.setText(c.getDireccion() != null ? c.getDireccion() : "");
        cmbTipoDocumento.setSelectedItem(c.getTipoDocumento());
        if (chkActivo != null) chkActivo.setSelected(c.isActivo());
    }

    /**
     * Valida los campos y guarda o actualiza el cliente en la base de datos.
     */
    private void guardar() {
        // Recolectar datos de los campos
        String nombre       = getValorCampo(txtNombre,       "Ej: Juan Carlos");
        String apellido     = getValorCampo(txtApellido,     "Ej: García López");
        String documento    = getValorCampo(txtDocumento,    "Ej: 12345678");
        String telefono     = getValorCampo(txtTelefono,     "Ej: 999-123-456");
        String email        = getValorCampo(txtEmail,        "Ej: correo@ejemplo.com");
        String nacionalidad = getValorCampo(txtNacionalidad, "Ej: Peruana");
        String direccion    = txtDireccion.getText().trim();
        String tipoDoc      = (String) cmbTipoDocumento.getSelectedItem();

        // ---- VALIDACIONES ----
        String errorNombre = Validaciones.validarTexto("Nombre", nombre, 2, 100);
        if (errorNombre != null) {
            mostrarError(errorNombre);
            txtNombre.requestFocus();
            return;
        }

        String errorApellido = Validaciones.validarTexto("Apellido", apellido, 2, 100);
        if (errorApellido != null) {
            mostrarError(errorApellido);
            txtApellido.requestFocus();
            return;
        }

        String errorDoc = Validaciones.validarDocumento(documento);
        if (errorDoc != null) {
            mostrarError(errorDoc);
            txtDocumento.requestFocus();
            return;
        }

        // Validar email solo si fue ingresado
        if (!email.isEmpty() && !Validaciones.esEmailValido(email)) {
            mostrarError("El email ingresado no tiene un formato válido.");
            txtEmail.requestFocus();
            return;
        }

        // Verificar si el documento ya existe (solo al crear nuevo o si cambió)
        boolean documentoCambio = clienteEditar == null ||
                                  !clienteEditar.getDocumento().equals(documento);
        if (documentoCambio) {
            Cliente existente = clienteDAO.buscarPorDocumento(documento);
            if (existente != null) {
                mostrarError("Ya existe un cliente con el documento: " + documento);
                txtDocumento.requestFocus();
                return;
            }
        }

        // ---- CONSTRUIR EL OBJETO CLIENTE ----
        Cliente cliente = (clienteEditar != null) ? clienteEditar : new Cliente();
        cliente.setNombre(Validaciones.capitalizar(nombre));
        cliente.setApellido(Validaciones.capitalizar(apellido));
        cliente.setTipoDocumento(tipoDoc);
        cliente.setDocumento(documento.toUpperCase());
        cliente.setTelefono(telefono.isEmpty() ? null : telefono);
        cliente.setEmail(email.isEmpty() ? null : email.toLowerCase());
        cliente.setNacionalidad(nacionalidad.isEmpty() ? null : Validaciones.capitalizar(nacionalidad));
        cliente.setDireccion(direccion.isEmpty() ? null : direccion);
        if (chkActivo != null) cliente.setActivo(chkActivo.isSelected());

        // ---- GUARDAR O ACTUALIZAR EN BD ----
        boolean exito;
        if (clienteEditar == null) {
            exito = clienteDAO.guardar(cliente);
        } else {
            exito = clienteDAO.actualizar(cliente);
        }

        if (exito) {
            guardadoExitoso = true;
            if (clienteEditar == null) {
                BitacoraService.log(Bitacora.ACCION_CREAR, Bitacora.MODULO_CLIENTES,
                    "Cliente registrado: " + cliente.getNombreCompleto()
                    + " — " + cliente.getTipoDocumento() + ": " + cliente.getDocumento());
            } else {
                BitacoraService.log(Bitacora.ACCION_EDITAR, Bitacora.MODULO_CLIENTES,
                    "Cliente editado: " + cliente.getNombreCompleto()
                    + " — Doc: " + cliente.getDocumento()
                    + (chkActivo != null ? " — Activo: " + cliente.isActivo() : ""));
            }
            String msg = (clienteEditar == null)
                ? "Cliente registrado exitosamente."
                : "Cliente actualizado exitosamente.";
            JOptionPane.showMessageDialog(this, msg, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose(); // Cerrar el diálogo
        } else {
            mostrarError("Ocurrió un error al guardar. Intenta de nuevo.");
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error de Validación", JOptionPane.WARNING_MESSAGE);
    }

    // =========================================================
    // GETTER para que el panel padre sepa si hubo cambios
    // =========================================================

    /**
     * @return true si el cliente fue guardado/actualizado exitosamente
     */
    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }
}
