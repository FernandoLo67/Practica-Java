package com.hotel.vista;

import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.modelo.Habitacion;
import com.hotel.util.Tema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Diálogo para buscar habitaciones disponibles en un rango de fechas.
 *
 * Permite seleccionar check-in y check-out, filtrar por tipo y capacidad,
 * y muestra una tabla con las habitaciones libres junto con su precio estimado
 * para el período solicitado.
 *
 * @author Fernando
 * @version 1.0
 */
public class DisponibilidadDialog extends JDialog {

    // =========================================================
    // COLORES
    // =========================================================
    private final Color COLOR_FONDO      = Tema.COLOR_FONDO;
    private final Color COLOR_HEADER     = Tema.COLOR_PRIMARIO;
    private final Color COLOR_TEXTO      = Tema.COLOR_TEXTO;
    private final Color COLOR_ROW_PAR    = Tema.COLOR_FILA_PAR;
    private final Color COLOR_ROW_IMPAR  = Tema.COLOR_FILA_IMPAR;

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JSpinner            spnCheckin;
    private JSpinner            spnCheckout;
    private JComboBox<String>   cmbTipo;
    private JComboBox<String>   cmbCapacidad;
    private JButton             btnBuscar;
    private JButton             btnSeleccionar;
    private JTable              tabla;
    private DefaultTableModel   modelo;
    private JLabel              lblResultado;
    private JLabel              lblNoches;

    /** Resultado seleccionado por el usuario (null si cerró sin seleccionar). */
    private Habitacion habitacionSeleccionada;
    private Date       fechaCheckinSeleccionada;
    private Date       fechaCheckoutSeleccionada;

    /** Lista de habitaciones de la última búsqueda (para recuperar el objeto al seleccionar). */
    private List<Habitacion> ultimaBusqueda;

    private final HabitacionDAO habitacionDAO = new HabitacionDAOImpl();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DisponibilidadDialog(Frame parent) {
        super(parent, "🔍  Buscador de Disponibilidad", true);
        buildUI();
        setSize(840, 560);
        setMinimumSize(new Dimension(700, 460));
        setLocationRelativeTo(parent);
        setResizable(true);
    }

    // =========================================================
    // CONSTRUCCIÓN DE LA UI
    // =========================================================

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(COLOR_FONDO);

        add(buildHeader(),   BorderLayout.NORTH);
        add(buildTable(),    BorderLayout.CENTER);
        add(buildFooter(),   BorderLayout.SOUTH);
    }

    /** Cabecera azul + panel de filtros */
    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());

        // — Tira de título —
        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setBackground(COLOR_HEADER);
        titulo.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lbl = new JLabel("🔍  Buscar Disponibilidad por Fechas");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Selecciona las fechas y filtra por tipo o capacidad");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 255));

        JPanel texto = new JPanel(new GridLayout(2, 1, 0, 2));
        texto.setOpaque(false);
        texto.add(lbl);
        texto.add(sub);
        titulo.add(texto, BorderLayout.CENTER);

        // — Panel de filtros —
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filtros.setBackground(new Color(240, 244, 255));
        filtros.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
            new Color(210, 218, 240)));

        // Check-In
        filtros.add(lbl("Check-In:"));
        spnCheckin = new JSpinner(new SpinnerDateModel());
        spnCheckin.setEditor(new JSpinner.DateEditor(spnCheckin, "dd/MM/yyyy"));
        spnCheckin.setPreferredSize(new Dimension(118, 30));
        spnCheckin.setValue(new java.util.Date());
        filtros.add(spnCheckin);

        // Check-Out
        filtros.add(lbl("Check-Out:"));
        spnCheckout = new JSpinner(new SpinnerDateModel());
        spnCheckout.setEditor(new JSpinner.DateEditor(spnCheckout, "dd/MM/yyyy"));
        spnCheckout.setPreferredSize(new Dimension(118, 30));
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        spnCheckout.setValue(cal.getTime());
        filtros.add(spnCheckout);

        lblNoches = new JLabel("(1 noche)");
        lblNoches.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblNoches.setForeground(new Color(70, 130, 80));
        filtros.add(lblNoches);

        ChangeListener cl = (ChangeEvent e) -> actualizarNoches();
        spnCheckin .addChangeListener(cl);
        spnCheckout.addChangeListener(cl);

        filtros.add(Box.createHorizontalStrut(6));

        // Tipo
        filtros.add(lbl("Tipo:"));
        cmbTipo = new JComboBox<>(new String[]{
            "Todos", "Simple", "Doble", "Suite", "Familiar", "Junior Suite"
        });
        cmbTipo.setPreferredSize(new Dimension(130, 30));
        filtros.add(cmbTipo);

        // Capacidad
        filtros.add(lbl("Capacidad:"));
        cmbCapacidad = new JComboBox<>(new String[]{
            "Cualquiera", "1+", "2+", "3+", "4+"
        });
        cmbCapacidad.setPreferredSize(new Dimension(100, 30));
        filtros.add(cmbCapacidad);

        filtros.add(Box.createHorizontalStrut(6));

        // Botón buscar
        btnBuscar = estilo(new JButton("🔍  Buscar"),
            new Color(26, 35, 126), Color.WHITE);
        btnBuscar.addActionListener(e -> buscar());
        filtros.add(btnBuscar);

        wrapper.add(titulo,  BorderLayout.NORTH);
        wrapper.add(filtros, BorderLayout.SOUTH);
        return wrapper;
    }

    /** Tabla de resultados */
    private JPanel buildTable() {
        String[] cols = {
            "Hab.", "Piso", "Tipo", "Capacidad",
            "Precio/noche", "Total estancia", "Disponibilidad"
        };
        modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        javax.swing.table.JTableHeader th = tabla.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(new Color(230, 235, 255));
        th.setForeground(new Color(40, 50, 120));
        th.setReorderingAllowed(false);

        int[] anchos = {60, 60, 130, 95, 110, 160, 110};
        for (int i = 0; i < anchos.length; i++)
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        // Renderer
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(col >= 4 ? CENTER : LEFT);
                if (sel) {
                    setBackground(new Color(180, 200, 255));
                    setForeground(new Color(20, 30, 80));
                } else {
                    setBackground(row % 2 == 0 ? COLOR_ROW_PAR : COLOR_ROW_IMPAR);
                    if (col == 6) {
                        setForeground(new Color(39, 120, 60));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(COLOR_TEXTO);
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                }
                setBorder(new EmptyBorder(2, 8, 2, 8));
                return this;
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) confirmar();
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                btnSeleccionar.setEnabled(tabla.getSelectedRow() >= 0);
        });

        lblResultado = new JLabel("Introduce las fechas y haz clic en Buscar.");
        lblResultado.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblResultado.setForeground(new Color(100, 110, 160));
        lblResultado.setBorder(new EmptyBorder(6, 10, 4, 10));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 218, 240)));

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(new EmptyBorder(8, 14, 4, 14));
        panel.add(lblResultado, BorderLayout.NORTH);
        panel.add(scroll,       BorderLayout.CENTER);
        return panel;
    }

    /** Botones inferiores */
    private JPanel buildFooter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        panel.setBackground(new Color(240, 244, 255));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
            new Color(210, 218, 240)));

        btnSeleccionar = estilo(
            new JButton("✅  Seleccionar habitación"),
            new Color(46, 125, 50), Color.WHITE);
        btnSeleccionar.setEnabled(false);
        btnSeleccionar.addActionListener(e -> confirmar());

        JButton btnCerrar = estilo(
            new JButton("Cerrar"),
            new Color(230, 230, 235), new Color(50, 50, 80));
        btnCerrar.addActionListener(e -> dispose());

        panel.add(btnSeleccionar);
        panel.add(btnCerrar);
        return panel;
    }

    // =========================================================
    // LÓGICA
    // =========================================================

    private void actualizarNoches() {
        try {
            java.util.Date ci = (java.util.Date) spnCheckin.getValue();
            java.util.Date co = (java.util.Date) spnCheckout.getValue();
            long dias = (co.getTime() - ci.getTime()) / (1000L * 60 * 60 * 24);
            if (dias > 0) {
                lblNoches.setText("(" + dias + " noche" + (dias == 1 ? "" : "s") + ")");
                lblNoches.setForeground(new Color(60, 130, 80));
            } else {
                lblNoches.setText("(⚠ fechas inválidas)");
                lblNoches.setForeground(new Color(180, 40, 40));
            }
        } catch (Exception ignored) {}
    }

    private void buscar() {
        modelo.setRowCount(0);
        btnSeleccionar.setEnabled(false);
        ultimaBusqueda = null;

        java.util.Date ci = (java.util.Date) spnCheckin.getValue();
        java.util.Date co = (java.util.Date) spnCheckout.getValue();

        if (!ci.before(co)) {
            JOptionPane.showMessageDialog(this,
                "La fecha de check-out debe ser posterior al check-in.",
                "Fechas inválidas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date sqlCI = new Date(ci.getTime());
        Date sqlCO = new Date(co.getTime());
        long noches = ChronoUnit.DAYS.between(
            sqlCI.toLocalDate(), sqlCO.toLocalDate());

        List<Habitacion> disponibles = habitacionDAO.listarDisponiblesEnRango(sqlCI, sqlCO);
        ultimaBusqueda = disponibles;

        // Filtros adicionales
        String tipoFiltro = (String) cmbTipo.getSelectedItem();
        int capMin = 0;
        String capFiltro = (String) cmbCapacidad.getSelectedItem();
        if (capFiltro != null) {
            switch (capFiltro) {
                case "1+": capMin = 1; break;
                case "2+": capMin = 2; break;
                case "3+": capMin = 3; break;
                case "4+": capMin = 4; break;
            }
        }

        int mostradas = 0;
        for (Habitacion h : disponibles) {
            if (tipoFiltro != null && !"Todos".equals(tipoFiltro)
                    && !h.getTipo().getNombre().toLowerCase()
                           .contains(tipoFiltro.toLowerCase())) continue;
            if (h.getTipo().getCapacidad() < capMin) continue;

            double precioNoche = h.getPrecioNoche();
            double total       = precioNoche * noches;

            modelo.addRow(new Object[]{
                h.getNumero(),
                "Piso " + h.getPiso(),
                h.getTipo().getNombre(),
                h.getTipo().getCapacidad() + " pers.",
                String.format("$%.2f", precioNoche),
                String.format("$%.2f  (%d noche%s)", total, noches, noches==1?"":"s"),
                "✓ Disponible"
            });
            mostradas++;
        }

        if (mostradas == 0) {
            lblResultado.setText("⚠  Sin habitaciones disponibles para el período seleccionado.");
            lblResultado.setForeground(new Color(160, 80, 0));
        } else {
            lblResultado.setText("✓  " + mostradas + " habitación(es) disponible(s)  —  del "
                + SDF.format(ci) + " al " + SDF.format(co));
            lblResultado.setForeground(new Color(39, 120, 60));
        }
    }

    private void confirmar() {
        int row = tabla.getSelectedRow();
        if (row < 0 || ultimaBusqueda == null) return;

        String numero = (String) modelo.getValueAt(row, 0);

        // Aplicar los mismos filtros para recuperar el objeto correcto
        java.util.Date ci = (java.util.Date) spnCheckin.getValue();
        java.util.Date co = (java.util.Date) spnCheckout.getValue();

        for (Habitacion h : ultimaBusqueda) {
            if (h.getNumero().equals(numero)) {
                habitacionSeleccionada    = h;
                fechaCheckinSeleccionada  = new Date(ci.getTime());
                fechaCheckoutSeleccionada = new Date(co.getTime());
                dispose();
                return;
            }
        }
    }

    // =========================================================
    // HELPERS UI
    // =========================================================

    private static JLabel lbl(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(50, 60, 100));
        return l;
    }

    private static JButton estilo(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        return btn;
    }

    // =========================================================
    // ACCESO A RESULTADOS
    // =========================================================

    /** Habitación seleccionada, o null si el usuario cerró sin elegir. */
    public Habitacion getHabitacionSeleccionada() { return habitacionSeleccionada; }

    /** Fecha check-in elegida. */
    public Date getFechaCheckin()  { return fechaCheckinSeleccionada; }

    /** Fecha check-out elegida. */
    public Date getFechaCheckout() { return fechaCheckoutSeleccionada; }

    /** true si el usuario confirmó una habitación. */
    public boolean isConfirmado()  { return habitacionSeleccionada != null; }
}
