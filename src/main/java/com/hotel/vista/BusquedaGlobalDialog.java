package com.hotel.vista;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reservacion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo de búsqueda global del sistema.
 *
 * Busca en tiempo real (con debounce de 300 ms y SwingWorker asíncrono)
 * en las tres entidades principales: Clientes, Habitaciones y Reservaciones.
 *
 * Uso:
 *   BusquedaGlobalDialog dlg = new BusquedaGlobalDialog(frame, this::navegarA);
 *   dlg.setVisible(true);
 *
 * Al hacer clic en un resultado, llama al callback con el módulo destino
 * y cierra el diálogo.
 *
 * @author Fernando
 * @version 1.0
 */
public class BusquedaGlobalDialog extends JDialog {

    // =========================================================
    // COLORES
    // =========================================================
    private static final Color COLOR_PRIMARIO   = new Color(26, 35, 126);
    private static final Color COLOR_HOVER      = new Color(237, 241, 255);
    private static final Color COLOR_FONDO      = new Color(248, 250, 255);
    private static final Color COLOR_SEPARADOR  = new Color(215, 220, 240);
    private static final Color COLOR_BADGE_CLI  = new Color(30, 100, 200);
    private static final Color COLOR_BADGE_HAB  = new Color(46, 125, 50);
    private static final Color COLOR_BADGE_RES  = new Color(123, 31, 162);

    // =========================================================
    // MODELO DE RESULTADO
    // =========================================================

    /**
     * Encapsula un resultado de búsqueda de cualquier módulo.
     */
    static class Resultado {
        final String icono;
        final String titulo;
        final String subtitulo;
        final String modulo;    // identificador para navegarA()
        final int    id;
        final Color  colorBadge;
        final String textoBadge;

        Resultado(String icono, String titulo, String subtitulo,
                  String modulo, int id, Color colorBadge, String textoBadge) {
            this.icono       = icono;
            this.titulo      = titulo;
            this.subtitulo   = subtitulo;
            this.modulo      = modulo;
            this.id          = id;
            this.colorBadge  = colorBadge;
            this.textoBadge  = textoBadge;
        }
    }

    /** Marcador de cabecera de grupo (String separador). */
    static class Cabecera {
        final String texto;
        Cabecera(String texto) { this.texto = texto; }
    }

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTextField        txtBuscar;
    private JList<Object>     listaResultados;
    private DefaultListModel<Object> modelo;
    private JLabel            lblEstado;

    // =========================================================
    // ESTADO
    // =========================================================
    private final Consumer<String>     onNavegar;
    private       Timer                debounceTimer;
    private       SwingWorker<List<Object>, Void> workerActual;

    // =========================================================
    // DAOs
    // =========================================================
    private final ClienteDAOImpl     daoClientes     = new ClienteDAOImpl();
    private final HabitacionDAOImpl  daoHabitaciones = new HabitacionDAOImpl();
    private final ReservacionDAOImpl daoReservaciones = new ReservacionDAOImpl();

    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM/yyyy");

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * @param padre      Ventana padre (para centrado y modalidad)
     * @param onNavegar  Callback llamado con el módulo destino al seleccionar un resultado
     */
    public BusquedaGlobalDialog(Frame padre, Consumer<String> onNavegar) {
        super(padre, "Búsqueda global", true);
        this.onNavegar = onNavegar;
        initComponents();
        setSize(640, 520);
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
        add(crearCuerpo(),     BorderLayout.CENTER);
        add(crearPiePagina(),  BorderLayout.SOUTH);

        // Cerrar con Escape
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Navegar con Enter en la lista
        listaResultados.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) seleccionarResultado();
            }
        });

        // Foco inicial en el campo de búsqueda
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { txtBuscar.requestFocusInWindow(); }
        });

        mostrarBienvenida();
    }

    private JPanel crearHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(COLOR_PRIMARIO);
        p.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Icono grande
        JLabel icono = new JLabel("🔍");
        icono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        icono.setVerticalAlignment(SwingConstants.CENTER);

        // Campo de búsqueda prominente
        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 190, 230), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        txtBuscar.setBackground(new Color(240, 244, 255));
        txtBuscar.setForeground(new Color(20, 25, 60));

        // Placeholder hint
        JLabel hint = new JLabel("Buscar clientes, habitaciones, reservaciones…");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        hint.setForeground(new Color(160, 175, 220));
        hint.setBorder(new EmptyBorder(4, 22, 0, 0));

        // Debounce: espera 300 ms tras el último keypress antes de buscar
        debounceTimer = new Timer(300, e -> lanzarBusqueda());
        debounceTimer.setRepeats(false);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { reiniciarDebounce(); }
            @Override public void removeUpdate(DocumentEvent e)  { reiniciarDebounce(); }
            @Override public void changedUpdate(DocumentEvent e) { reiniciarDebounce(); }
        });

        // Navegar con ↓ desde el campo hacia la lista
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && modelo.getSize() > 0) {
                    listaResultados.requestFocusInWindow();
                    if (listaResultados.getSelectedIndex() < 0) {
                        seleccionarPrimerResultado();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    seleccionarResultado();
                }
            }
        });

        JPanel campoPanel = new JPanel(new BorderLayout(0, 4));
        campoPanel.setOpaque(false);
        campoPanel.add(txtBuscar, BorderLayout.CENTER);
        campoPanel.add(hint,      BorderLayout.SOUTH);

        p.add(icono,      BorderLayout.WEST);
        p.add(campoPanel, BorderLayout.CENTER);

        return p;
    }

    private JScrollPane crearCuerpo() {
        modelo = new DefaultListModel<>();
        listaResultados = new JList<>(modelo);
        listaResultados.setBackground(COLOR_FONDO);
        listaResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaResultados.setFixedCellHeight(-1);    // altura variable por renderer
        listaResultados.setCellRenderer(new RendererResultado());
        listaResultados.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int idx = listaResultados.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        listaResultados.setSelectedIndex(idx);
                        seleccionarResultado();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(listaResultados);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_SEPARADOR));
        scroll.getViewport().setBackground(COLOR_FONDO);
        return scroll;
    }

    private JPanel crearPiePagina() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_SEPARADOR),
            new EmptyBorder(7, 16, 7, 16)
        ));

        lblEstado = new JLabel(" ");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(130, 140, 170));

        JLabel lblAtajos = new JLabel("↑↓ Navegar  •  Enter Abrir  •  Esc Cerrar");
        lblAtajos.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblAtajos.setForeground(new Color(160, 170, 200));

        p.add(lblEstado,  BorderLayout.WEST);
        p.add(lblAtajos,  BorderLayout.EAST);
        return p;
    }

    // =========================================================
    // LÓGICA DE BÚSQUEDA
    // =========================================================

    private void reiniciarDebounce() {
        debounceTimer.restart();
    }

    private void lanzarBusqueda() {
        String termino = txtBuscar.getText().trim();

        if (termino.length() < 2) {
            if (termino.isEmpty()) mostrarBienvenida();
            else estado("Escribe al menos 2 caracteres para buscar.");
            return;
        }

        // Cancelar búsqueda anterior si sigue en curso
        if (workerActual != null && !workerActual.isDone()) {
            workerActual.cancel(true);
        }

        estado("Buscando…");
        modelo.clear();

        workerActual = new SwingWorker<List<Object>, Void>() {

            @Override
            protected List<Object> doInBackground() {
                List<Object> items = new ArrayList<>();

                // --- Clientes ---
                List<Cliente> clientes = daoClientes.buscar(termino);
                if (!clientes.isEmpty()) {
                    items.add(new Cabecera("CLIENTES  (" + clientes.size() + ")"));
                    for (Cliente c : clientes) {
                        items.add(new Resultado(
                            "👤",
                            c.getNombreCompleto(),
                            c.getTipoDocumento() + ": " + c.getDocumento()
                                + (c.getEmail() != null && !c.getEmail().isEmpty()
                                   ? "  •  " + c.getEmail() : "")
                                + (!c.isActivo() ? "  [INACTIVO]" : ""),
                            "clientes", c.getId(),
                            COLOR_BADGE_CLI, "Cliente"
                        ));
                    }
                }

                // --- Habitaciones ---
                List<Habitacion> habitaciones = daoHabitaciones.buscar(termino);
                if (!habitaciones.isEmpty()) {
                    items.add(new Cabecera("HABITACIONES  (" + habitaciones.size() + ")"));
                    for (Habitacion h : habitaciones) {
                        String precio = String.format("Q %.2f/noche", h.getPrecioNoche());
                        items.add(new Resultado(
                            "🛏",
                            "Habitación N° " + h.getNumero()
                                + "  —  " + (h.getTipo() != null ? h.getTipo().getNombre() : ""),
                            "Piso " + h.getPiso() + "  •  " + h.getEstado() + "  •  " + precio,
                            "habitaciones", h.getId(),
                            COLOR_BADGE_HAB, "Habitación"
                        ));
                    }
                }

                // --- Reservaciones ---
                List<Reservacion> reservaciones = daoReservaciones.buscar(termino);
                if (!reservaciones.isEmpty()) {
                    items.add(new Cabecera("RESERVACIONES  (" + reservaciones.size() + ")"));
                    for (Reservacion r : reservaciones) {
                        String cliente = r.getCliente() != null ? r.getCliente().getNombreCompleto() : "—";
                        String numHab  = r.getHabitacion() != null ? "Hab. " + r.getHabitacion().getNumero() : "—";
                        String fechas  = (r.getFechaCheckin() != null ? FMT.format(r.getFechaCheckin()) : "?")
                                       + " → "
                                       + (r.getFechaCheckout() != null ? FMT.format(r.getFechaCheckout()) : "?");
                        items.add(new Resultado(
                            "📅",
                            cliente + "  —  " + numHab,
                            fechas + "  •  " + r.getEstado(),
                            "reservaciones", r.getId(),
                            COLOR_BADGE_RES, "Reservación"
                        ));
                    }
                }

                return items;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<Object> items = get();
                    modelo.clear();
                    if (items.isEmpty()) {
                        estado("Sin resultados para '" + termino + "'.");
                        mostrarVacio(termino);
                    } else {
                        for (Object o : items) modelo.addElement(o);
                        int total = items.stream().filter(o -> o instanceof Resultado).mapToInt(o -> 1).sum();
                        estado(total + " resultado" + (total == 1 ? "" : "s") + " para '" + termino + "'");
                        // Preseleccionar primer resultado real
                        seleccionarPrimerResultado();
                    }
                } catch (Exception e) {
                    estado("Error durante la búsqueda.");
                }
            }
        };

        workerActual.execute();
    }

    /** Selecciona el primer elemento de tipo Resultado en la lista. */
    private void seleccionarPrimerResultado() {
        for (int i = 0; i < modelo.getSize(); i++) {
            if (modelo.getElementAt(i) instanceof Resultado) {
                listaResultados.setSelectedIndex(i);
                listaResultados.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    /** Abre el módulo del resultado actualmente seleccionado. */
    private void seleccionarResultado() {
        Object sel = listaResultados.getSelectedValue();
        if (!(sel instanceof Resultado)) return;
        String modulo = ((Resultado) sel).modulo;
        dispose();
        onNavegar.accept(modulo);
    }

    // =========================================================
    // ESTADOS VISUALES
    // =========================================================

    private void mostrarBienvenida() {
        modelo.clear();
        modelo.addElement(new Cabecera("SUGERENCIAS"));
        modelo.addElement(new Resultado("👤", "Busca clientes por nombre o documento", "Ej: \"García\" o \"DPI 1234\"", "clientes", -1, COLOR_BADGE_CLI, "Cliente"));
        modelo.addElement(new Resultado("🛏", "Busca habitaciones por número o tipo",   "Ej: \"101\" o \"Suite\"",           "habitaciones", -1, COLOR_BADGE_HAB, "Habitación"));
        modelo.addElement(new Resultado("📅", "Busca reservaciones por estado o cliente","Ej: \"CONFIRMADA\" o \"López\"",    "reservaciones", -1, COLOR_BADGE_RES, "Reservación"));
        estado("Escribe para comenzar la búsqueda  •  Ctrl+F para abrir en cualquier momento");
    }

    private void mostrarVacio(String termino) {
        modelo.clear();
        modelo.addElement(new Cabecera("SIN RESULTADOS"));
        modelo.addElement(new Resultado("🔎",
            "No se encontro '" + termino + "' en ningun modulo",
            "Intenta con otro termino o revisa la ortografia",
            "", -1, new Color(160, 160, 160), ""));
    }

    private void estado(String msg) {
        lblEstado.setText(msg);
    }

    // =========================================================
    // RENDERER
    // =========================================================

    /**
     * Renderiza cada celda de la lista:
     *   - Cabecera → etiqueta gris pequeña como separador de grupo
     *   - Resultado → fila con icono, título en negrita, subtítulo gris y badge
     */
    private static class RendererResultado implements ListCellRenderer<Object> {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            if (value instanceof Cabecera) {
                return renderCabecera(((Cabecera) value).texto);
            }
            if (value instanceof Resultado) {
                return renderResultado((Resultado) value, isSelected);
            }
            return new JLabel();
        }

        private Component renderCabecera(String texto) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(new Color(235, 238, 250));
            p.setBorder(new EmptyBorder(6, 16, 4, 16));

            JLabel lbl = new JLabel(texto);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setForeground(new Color(100, 115, 165));
            p.add(lbl, BorderLayout.CENTER);

            // Línea separadora superior
            p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)),
                new EmptyBorder(6, 16, 4, 16)
            ));
            return p;
        }

        private Component renderResultado(Resultado r, boolean selected) {
            JPanel fila = new JPanel(new BorderLayout(12, 0));
            fila.setBackground(selected ? new Color(225, 232, 255) : Color.WHITE);
            fila.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(238, 240, 250)),
                new EmptyBorder(10, 16, 10, 16)
            ));
            fila.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Icono
            JLabel lblIcono = new JLabel(r.icono);
            lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
            lblIcono.setVerticalAlignment(SwingConstants.TOP);
            lblIcono.setPreferredSize(new Dimension(32, 32));

            // Textos
            JPanel textos = new JPanel();
            textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
            textos.setOpaque(false);

            JLabel titulo = new JLabel(r.titulo);
            titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titulo.setForeground(selected ? new Color(26, 35, 126) : new Color(30, 35, 60));

            JLabel subtitulo = new JLabel(r.subtitulo);
            subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            subtitulo.setForeground(new Color(130, 140, 170));

            textos.add(titulo);
            textos.add(Box.createVerticalStrut(2));
            textos.add(subtitulo);

            // Badge de módulo (solo si tiene texto)
            JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
            derecha.setOpaque(false);
            if (r.textoBadge != null && !r.textoBadge.isEmpty()) {
                JLabel badge = new JLabel("  " + r.textoBadge + "  ");
                badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                badge.setForeground(Color.WHITE);
                badge.setOpaque(true);
                badge.setBackground(r.colorBadge);
                badge.setBorder(new EmptyBorder(2, 5, 2, 5));
                derecha.add(badge);
            }

            fila.add(lblIcono, BorderLayout.WEST);
            fila.add(textos,   BorderLayout.CENTER);
            fila.add(derecha,  BorderLayout.EAST);

            return fila;
        }
    }
}
