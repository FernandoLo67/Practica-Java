package com.hotel.vista;

import com.hotel.modelo.Bitacora;
import com.hotel.util.BitacoraService;
import com.hotel.util.HotelConfig;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.util.Properties;

/**
 * Panel de configuración de los datos generales del hotel.
 *
 * Permite al administrador editar nombre, slogan, dirección,
 * teléfono, email, NIT y sitio web del hotel.
 *
 * Los cambios se persisten en hotel.properties (directorio de trabajo)
 * y se reflejan de inmediato en la vista previa lateral.
 *
 * Acceso: solo ADMIN.
 *
 * @author Fernando
 * @version 1.0
 */
public class DatosHotelPanel extends JPanel {

    // =========================================================
    // COMPONENTES DEL FORMULARIO
    // =========================================================
    private JTextField txtNombre;
    private JTextField txtSlogan;
    private JTextField txtDireccion;
    private JTextField txtTelefono;
    private JTextField txtEmail;
    private JTextField txtNit;
    private JTextField txtWeb;

    // Vista previa
    private JLabel lblPrevNombre;
    private JLabel lblPrevSlogan;
    private JLabel lblPrevDireccion;
    private JLabel lblPrevTelefono;
    private JLabel lblPrevEmail;
    private JLabel lblPrevNit;
    private JLabel lblPrevWeb;

    private JLabel lblMensaje;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DatosHotelPanel() {
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearCuerpo(),     BorderLayout.CENTER);

        cargarEnFormulario();
    }

    // =========================================================
    // UI — ENCABEZADO
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = UIHelper.panelEncabezado();

        JPanel izq = new JPanel(new GridLayout(2, 1, 0, 2));
        izq.setOpaque(false);
        izq.add(UIHelper.titulo("🏨  Datos del Hotel"));
        JLabel sub = new JLabel("Configuración general · visible en reportes y correos");
        sub.setFont(Tema.FUENTE_SMALL);
        sub.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
        izq.add(sub);
        p.add(izq, BorderLayout.WEST);

        lblMensaje = new JLabel(" ");
        lblMensaje.setFont(Tema.FUENTE_SMALL);
        p.add(lblMensaje, BorderLayout.EAST);

        return p;
    }

    // =========================================================
    // UI — CUERPO (formulario + vista previa)
    // =========================================================

    private JScrollPane crearCuerpo() {
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setBackground(Tema.COLOR_FONDO);
        cuerpo.setBorder(new EmptyBorder(20, 22, 20, 22));

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.insets  = new Insets(0, 0, 0, 16);
        g.weightx = 0.55;
        g.weighty = 1;
        g.gridx   = 0;
        g.gridy   = 0;
        cuerpo.add(crearFormulario(), g);

        g.gridx  = 1;
        g.weightx = 0.45;
        g.insets  = new Insets(0, 0, 0, 0);
        cuerpo.add(crearVistaPreviaPanel(), g);

        JScrollPane scroll = new JScrollPane(cuerpo);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Tema.COLOR_FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // =========================================================
    // FORMULARIO
    // =========================================================

    private JPanel crearFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.COLOR_BORDE, 1),
            new EmptyBorder(22, 24, 22, 24)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets  = new Insets(4, 0, 4, 0);
        g.gridx   = 0;

        // Título de sección
        g.gridy = 0;
        JLabel seccion1 = seccion("Información principal");
        p.add(seccion1, g);

        g.gridy = 1; p.add(etiqueta("Nombre del hotel *"), g);
        g.gridy = 2; txtNombre = campo(); p.add(txtNombre, g);

        g.gridy = 3; p.add(etiqueta("Eslogan / descripción breve"), g);
        g.gridy = 4; txtSlogan = campo(); p.add(txtSlogan, g);

        // Separador
        g.gridy = 5; g.insets = new Insets(12, 0, 4, 0);
        p.add(seccion("Datos de contacto"), g);
        g.insets = new Insets(4, 0, 4, 0);

        g.gridy = 6;  p.add(etiqueta("Dirección"), g);
        g.gridy = 7;  txtDireccion = campo(); p.add(txtDireccion, g);

        g.gridy = 8;  p.add(etiqueta("Teléfono"), g);
        g.gridy = 9;  txtTelefono = campo(); p.add(txtTelefono, g);

        g.gridy = 10; p.add(etiqueta("Correo electrónico de contacto"), g);
        g.gridy = 11; txtEmail = campo(); p.add(txtEmail, g);

        g.gridy = 12; g.insets = new Insets(12, 0, 4, 0);
        p.add(seccion("Datos fiscales y web"), g);
        g.insets = new Insets(4, 0, 4, 0);

        g.gridy = 13; p.add(etiqueta("NIT / Número fiscal"), g);
        g.gridy = 14; txtNit = campo(); p.add(txtNit, g);

        g.gridy = 15; p.add(etiqueta("Sitio web"), g);
        g.gridy = 16; txtWeb = campo(); p.add(txtWeb, g);

        // Botones
        g.gridy = 17; g.insets = new Insets(20, 0, 0, 0);
        p.add(crearBotones(), g);

        // Listener para actualizar vista previa en tiempo real
        DocumentListener previewListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { actualizarPrevia(); }
            @Override public void removeUpdate(DocumentEvent e)  { actualizarPrevia(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizarPrevia(); }
        };
        txtNombre.getDocument().addDocumentListener(previewListener);
        txtSlogan.getDocument().addDocumentListener(previewListener);
        txtDireccion.getDocument().addDocumentListener(previewListener);
        txtTelefono.getDocument().addDocumentListener(previewListener);
        txtEmail.getDocument().addDocumentListener(previewListener);
        txtNit.getDocument().addDocumentListener(previewListener);
        txtWeb.getDocument().addDocumentListener(previewListener);

        return p;
    }

    private JPanel crearBotones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);

        JButton btnGuardar = new JButton("💾  Guardar cambios");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(Tema.COLOR_PRIMARIO);
        btnGuardar.setOpaque(true);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.setBorder(new EmptyBorder(9, 20, 9, 20));
        btnGuardar.addActionListener(e -> guardar());

        JButton btnRestaurar = new JButton("↺  Recargar");
        btnRestaurar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRestaurar.setForeground(Tema.COLOR_PRIMARIO);
        btnRestaurar.setBackground(new Color(235, 239, 255));
        btnRestaurar.setOpaque(true);
        btnRestaurar.setBorderPainted(false);
        btnRestaurar.setFocusPainted(false);
        btnRestaurar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRestaurar.setBorder(new EmptyBorder(9, 16, 9, 16));
        btnRestaurar.addActionListener(e -> {
            HotelConfig.recargar();
            cargarEnFormulario();
            mensaje("Datos recargados desde el archivo.", Tema.COLOR_INFO);
        });

        p.add(btnGuardar);
        p.add(Box.createHorizontalStrut(10));
        p.add(btnRestaurar);
        return p;
    }

    // =========================================================
    // VISTA PREVIA
    // =========================================================

    private JPanel crearVistaPreviaPanel() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);

        JLabel titulo = new JLabel("Vista previa");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titulo.setForeground(new Color(100, 110, 150));
        titulo.setBorder(new EmptyBorder(0, 0, 10, 0));
        contenedor.add(titulo, BorderLayout.NORTH);

        JPanel tarjeta = new JPanel(new GridBagLayout());
        tarjeta.setBackground(Tema.COLOR_PRIMARIO);
        tarjeta.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx   = 0;
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets  = new Insets(3, 0, 3, 0);

        lblPrevNombre = new JLabel("Hotel Vista");
        lblPrevNombre.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblPrevNombre.setForeground(Color.WHITE);
        g.gridy = 0; tarjeta.add(lblPrevNombre, g);

        lblPrevSlogan = new JLabel("Bienvenido a su hogar lejos del hogar");
        lblPrevSlogan.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPrevSlogan.setForeground(new Color(180, 195, 255));
        g.gridy = 1; tarjeta.add(lblPrevSlogan, g);

        // Separador
        g.gridy = 2;
        g.insets = new Insets(14, 0, 8, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 80, 160));
        tarjeta.add(sep, g);
        g.insets = new Insets(3, 0, 3, 0);

        lblPrevDireccion = filaPrevia("📍", "");
        g.gridy = 3; tarjeta.add(lblPrevDireccion, g);

        lblPrevTelefono = filaPrevia("📞", "");
        g.gridy = 4; tarjeta.add(lblPrevTelefono, g);

        lblPrevEmail = filaPrevia("✉", "");
        g.gridy = 5; tarjeta.add(lblPrevEmail, g);

        lblPrevNit = filaPrevia("🧾", "");
        g.gridy = 6; tarjeta.add(lblPrevNit, g);

        lblPrevWeb = filaPrevia("🌐", "");
        g.gridy = 7; tarjeta.add(lblPrevWeb, g);

        // Glue para empujar todo hacia arriba
        GridBagConstraints glue = new GridBagConstraints();
        glue.gridy   = 8;
        glue.weighty = 1;
        glue.fill    = GridBagConstraints.VERTICAL;
        tarjeta.add(Box.createVerticalGlue(), glue);

        contenedor.add(tarjeta, BorderLayout.CENTER);

        // Nota al pie
        JLabel nota = new JLabel("  * Aparece en reportes PDF y correos de confirmación");
        nota.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        nota.setForeground(new Color(140, 150, 180));
        nota.setBorder(new EmptyBorder(8, 0, 0, 0));
        contenedor.add(nota, BorderLayout.SOUTH);

        return contenedor;
    }

    /** Crea una JLabel para la vista previa con icono + texto. */
    private JLabel filaPrevia(String icono, String valor) {
        JLabel l = new JLabel(icono + "  " + (valor.isEmpty() ? "—" : valor));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(200, 210, 245));
        return l;
    }

    private void setFilaPrevia(JLabel lbl, String icono, String valor) {
        lbl.setText(icono + "  " + (valor == null || valor.isBlank() ? "—" : valor));
    }

    // =========================================================
    // LÓGICA
    // =========================================================

    private void cargarEnFormulario() {
        txtNombre.setText(HotelConfig.getNombre());
        txtSlogan.setText(HotelConfig.getSlogan());
        txtDireccion.setText(HotelConfig.getDireccion());
        txtTelefono.setText(HotelConfig.getTelefono());
        txtEmail.setText(HotelConfig.getEmail());
        txtNit.setText(HotelConfig.getNit());
        txtWeb.setText(HotelConfig.getWeb());
        actualizarPrevia();
    }

    private void actualizarPrevia() {
        lblPrevNombre.setText(
            txtNombre.getText().isBlank() ? "Nombre del hotel" : txtNombre.getText().trim());
        lblPrevSlogan.setText(
            txtSlogan.getText().isBlank() ? " " : txtSlogan.getText().trim());
        setFilaPrevia(lblPrevDireccion, "📍", txtDireccion.getText());
        setFilaPrevia(lblPrevTelefono,  "📞", txtTelefono.getText());
        setFilaPrevia(lblPrevEmail,     "✉",  txtEmail.getText());
        setFilaPrevia(lblPrevNit,       "🧾", txtNit.getText());
        setFilaPrevia(lblPrevWeb,       "🌐", txtWeb.getText());
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mensaje("El nombre del hotel es obligatorio.", Tema.COLOR_ERROR);
            txtNombre.requestFocus();
            return;
        }

        Properties p = new Properties();
        p.setProperty("hotel.nombre",    nombre);
        p.setProperty("hotel.slogan",    txtSlogan.getText().trim());
        p.setProperty("hotel.direccion", txtDireccion.getText().trim());
        p.setProperty("hotel.telefono",  txtTelefono.getText().trim());
        p.setProperty("hotel.email",     txtEmail.getText().trim());
        p.setProperty("hotel.nit",       txtNit.getText().trim());
        p.setProperty("hotel.web",       txtWeb.getText().trim());

        try {
            HotelConfig.guardar(p);
            BitacoraService.log(Bitacora.ACCION_EDITAR, "CONFIGURACION",
                "Datos del hotel actualizados: " + nombre);
            mensaje("✅  Datos guardados correctamente.", Tema.COLOR_EXITO);
        } catch (IOException ex) {
            mensaje("⚠  Error al guardar: " + ex.getMessage(), Tema.COLOR_ERROR);
        }
    }

    private void mensaje(String texto, Color color) {
        lblMensaje.setText(texto);
        lblMensaje.setForeground(color);
        // Limpiar el mensaje después de 4 segundos
        Timer t = new Timer(4000, e -> lblMensaje.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // =========================================================
    // HELPERS DE UI
    // =========================================================

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(55, 60, 90));
        l.setBorder(new EmptyBorder(6, 0, 2, 0));
        return l;
    }

    private JLabel seccion(String texto) {
        JLabel l = new JLabel(texto.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(26, 35, 126));
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 240)),
            new EmptyBorder(0, 0, 4, 0)
        ));
        return l;
    }

    private JTextField campo() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(0, 34));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(195, 200, 225), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return f;
    }
}
