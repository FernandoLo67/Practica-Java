package com.hotel.vista;

import com.hotel.dao.impl.BitacoraDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.util.ExcelExporter;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel de Bitácora — registro de auditoría de acciones del sistema.
 *
 * Solo visible para usuarios con rol ADMIN.
 * Muestra quién hizo qué, en qué módulo y cuándo.
 *
 * @author Fernando
 * @version 1.0
 */
public class BitacoraPanel extends JPanel {

    private static final String[] COLUMNAS = {
        "ID", "Fecha / Hora", "Usuario", "Módulo", "Acción", "Descripción"
    };

    private JTable                            tabla;
    private DefaultTableModel                 modeloTabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField                        txtBuscar;
    private JComboBox<String>                 cmbModulo;
    private JLabel                            lblEstado;

    private final BitacoraDAOImpl bitacoraDAO;
    private final Frame           ventanaPadre;

    public BitacoraPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.bitacoraDAO  = new BitacoraDAOImpl();
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);
        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearTabla(),       BorderLayout.CENTER);
        add(crearBarra(),       BorderLayout.SOUTH);
        cargarBitacora();
    }

    // =========================================================
    // ENCABEZADO
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = UIHelper.panelEncabezado();
        p.add(UIHelper.titulo("🔍  Bitácora de Auditoría"), BorderLayout.WEST);

        // Buscador + filtro módulo
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        filtros.setOpaque(false);

        txtBuscar = UIHelper.campoTexto();
        txtBuscar.setPreferredSize(new Dimension(220, 32));
        txtBuscar.setToolTipText("Buscar por usuario, acción o descripción");

        cmbModulo = new JComboBox<>(new String[]{
            "Todos",
            Bitacora.MODULO_SISTEMA,
            Bitacora.MODULO_CLIENTES,
            Bitacora.MODULO_HABITACIONES,
            Bitacora.MODULO_RESERVACIONES,
            Bitacora.MODULO_CHECKINOUT,
            Bitacora.MODULO_FACTURAS,
            Bitacora.MODULO_USUARIOS
        });
        cmbModulo.setFont(Tema.FUENTE_NORMAL);
        cmbModulo.setPreferredSize(new Dimension(160, 32));
        cmbModulo.setBackground(Color.WHITE);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });
        cmbModulo.addActionListener(e -> filtrar());

        filtros.add(new JLabel("🔍"));
        filtros.add(txtBuscar);
        filtros.add(new JLabel("Módulo:"));
        filtros.add(cmbModulo);
        p.add(filtros, BorderLayout.CENTER);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);

        JButton btnRefresh = UIHelper.botonNeutro("↺  Actualizar");
        JButton btnExcel   = UIHelper.botonNeutro("📊 Excel");

        btnRefresh.addActionListener(e -> cargarBitacora());
        btnExcel.addActionListener(e -> ExcelExporter.exportar(tabla, "Bitacora", ventanaPadre));

        botones.add(btnRefresh);
        botones.add(btnExcel);
        p.add(botones, BorderLayout.EAST);

        return p;
    }

    // =========================================================
    // TABLA
    // =========================================================

    private JScrollPane crearTabla() {
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        UIHelper.estilizarTabla(tabla);
        UIHelper.ocultarColumnaId(tabla);
        tabla.setRowHeight(30);

        int[] anchos = {0, 145, 170, 110, 120, 300};
        for (int i = 1; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Colorear filas según acción
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    setBackground(row % 2 == 0 ? Tema.COLOR_FILA_PAR : Tema.COLOR_FILA_IMPAR);
                    setForeground(Color.BLACK);

                    int cm = tabla.convertColumnIndexToModel(col);
                    if (cm == 4 && v != null) { // columna Acción
                        switch (v.toString()) {
                            case Bitacora.ACCION_LOGIN:
                                setForeground(Tema.COLOR_EXITO); break;
                            case Bitacora.ACCION_LOGIN_FALLIDO:
                            case Bitacora.ACCION_ELIMINAR:
                                setForeground(Tema.COLOR_ERROR); break;
                            case Bitacora.ACCION_CHECKOUT:
                            case Bitacora.ACCION_CHECKIN:
                                setForeground(Tema.COLOR_INFO); break;
                            case Bitacora.ACCION_CAMBIAR_ESTADO:
                                setForeground(Tema.COLOR_ACENTO); break;
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return this;
            }
        });

        // Doble clic para ver descripción completa
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) verDetalle();
            }
        });

        sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel crearBarra() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 220, 240)),
            new EmptyBorder(6, 16, 6, 16)
        ));
        lblEstado = new JLabel(" ");
        lblEstado.setFont(Tema.FUENTE_SMALL);
        lblEstado.setForeground(new Color(90, 95, 120));
        p.add(lblEstado, BorderLayout.WEST);
        return p;
    }

    // =========================================================
    // DATOS
    // =========================================================

    public void cargarBitacora() {
        modeloTabla.setRowCount(0);
        List<Bitacora> lista = bitacoraDAO.listarTodas();
        for (Bitacora b : lista) {
            modeloTabla.addRow(new Object[]{
                b.getId(),
                b.getFecha() != null ? b.getFecha().toString().substring(0, 19) : "-",
                b.getUsuarioNombre(),
                b.getModulo(),
                b.getAccion(),
                b.getDescripcion() != null ? b.getDescripcion() : ""
            });
        }
        lblEstado.setText("📋  " + lista.size() + " registro(s) encontrados");
    }

    private void filtrar() {
        String texto  = txtBuscar.getText().trim();
        String modulo = (String) cmbModulo.getSelectedItem();

        RowFilter<DefaultTableModel, Object> filtroTexto = null;
        RowFilter<DefaultTableModel, Object> filtroMod   = null;

        if (!texto.isEmpty()) {
            try { filtroTexto = RowFilter.regexFilter("(?i)" + texto, 2, 3, 4, 5); }
            catch (java.util.regex.PatternSyntaxException ignored) {}
        }

        if (modulo != null && !modulo.equals("Todos")) {
            final String m = modulo;
            filtroMod = RowFilter.regexFilter("(?i)^" + m + "$", 3);
        }

        if (filtroTexto != null && filtroMod != null) {
            sorter.setRowFilter(RowFilter.andFilter(
                java.util.Arrays.asList(filtroTexto, filtroMod)));
        } else if (filtroTexto != null) {
            sorter.setRowFilter(filtroTexto);
        } else if (filtroMod != null) {
            sorter.setRowFilter(filtroMod);
        } else {
            sorter.setRowFilter(null);
        }
    }

    private void verDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int filaM = tabla.convertRowIndexToModel(fila);

        String fecha   = (String) modeloTabla.getValueAt(filaM, 1);
        String usuario = (String) modeloTabla.getValueAt(filaM, 2);
        String modulo  = (String) modeloTabla.getValueAt(filaM, 3);
        String accion  = (String) modeloTabla.getValueAt(filaM, 4);
        String desc    = (String) modeloTabla.getValueAt(filaM, 5);

        String detalle = String.format(
            "Fecha:       %s\nUsuario:     %s\nMódulo:      %s\nAcción:      %s\n\nDescripción:\n%s",
            fecha, usuario, modulo, accion, desc
        );

        JTextArea area = new JTextArea(detalle);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(250, 252, 255));
        area.setPreferredSize(new Dimension(480, 180));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Detalle de registro", JOptionPane.PLAIN_MESSAGE);
    }
}
