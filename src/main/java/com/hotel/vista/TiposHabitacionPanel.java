package com.hotel.vista;

import com.hotel.dao.impl.TipoHabitacionDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.BitacoraService;
import com.hotel.util.Tema;
import com.hotel.util.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel CRUD para la tabla tipo_habitacion.
 *
 * Permite al administrador gestionar los tipos (Simple, Doble, Suite…)
 * y sus precios base + capacidad.
 * Las habitaciones con precio_especial heredan su precio de aquí
 * si no tienen uno propio.
 *
 * @author Fernando
 * @version 1.0
 */
public class TiposHabitacionPanel extends JPanel {

    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Precio Base (Q)", "Capacidad (pers.)", "Descripción"
    };

    // =========================================================
    // COMPONENTES
    // =========================================================
    private JTable             tabla;
    private DefaultTableModel  modeloTabla;
    private JLabel             lblEstado;
    private JButton            btnNuevo;
    private JButton            btnEditar;
    private JButton            btnEliminar;
    private JButton            btnActualizar;

    private final TipoHabitacionDAOImpl dao;
    private final Frame ventanaPadre;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public TiposHabitacionPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.dao          = new TipoHabitacionDAOImpl();
        setLayout(new BorderLayout());
        setBackground(Tema.COLOR_FONDO);

        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCentro(),      BorderLayout.CENTER);
        add(crearBarra(),       BorderLayout.SOUTH);

        cargar();
    }

    // =========================================================
    // UI
    // =========================================================

    private JPanel crearEncabezado() {
        JPanel p = UIHelper.panelEncabezado();
        p.add(UIHelper.titulo("🏷  Tipos de Habitación"), BorderLayout.WEST);

        btnActualizar = UIHelper.botonNeutro("↺  Actualizar");
        btnNuevo      = UIHelper.botonExito("➕  Nuevo tipo");
        btnEditar     = UIHelper.botonPrimario("✏  Editar");
        btnEliminar   = UIHelper.botonPeligro("🗑  Eliminar");

        btnEditar  .setEnabled(false);
        btnEliminar.setEnabled(false);

        btnActualizar.addActionListener(e -> cargar());
        btnNuevo     .addActionListener(e -> abrirFormNuevo());
        btnEditar    .addActionListener(e -> abrirFormEditar());
        btnEliminar  .addActionListener(e -> eliminar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        botones.add(btnActualizar);
        botones.add(btnNuevo);
        botones.add(btnEditar);
        botones.add(btnEliminar);

        p.add(botones, BorderLayout.EAST);

        // Nota informativa
        JLabel nota = new JLabel(
            "El precio base aplica a todas las habitaciones del tipo. " +
            "Puedes fijar un precio especial por habitación individual.");
        nota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        nota.setForeground(new Color(100, 110, 150));
        nota.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(p,    BorderLayout.NORTH);
        wrap.add(nota, BorderLayout.SOUTH);
        wrap.setBorder(new EmptyBorder(0, 20, 6, 20));
        return wrap;
    }

    private JPanel crearCentro() {
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        UIHelper.estilizarTabla(tabla);
        UIHelper.ocultarColumnaId(tabla);

        int[] anchos = {0, 130, 130, 120, 300};
        for (int i = 1; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        tabla.setRowSorter(new TableRowSorter<>(modeloTabla));

        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirFormEditar();
            }
        });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean sel = tabla.getSelectedRow() >= 0;
                btnEditar  .setEnabled(sel);
                btnEliminar.setEnabled(sel);
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.COLOR_BORDE));

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.COLOR_FONDO);
        p.setBorder(new EmptyBorder(6, 14, 10, 14));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearBarra() {
        lblEstado = new JLabel("Cargando...");
        return UIHelper.barraEstado(lblEstado,
            new JLabel("Doble clic para editar  |  El precio base aplica a todas las habitaciones del tipo"));
    }

    // =========================================================
    // DATOS
    // =========================================================

    public void cargar() {
        modeloTabla.setRowCount(0);
        List<TipoHabitacion> lista = dao.listarTodos();
        for (TipoHabitacion t : lista) {
            modeloTabla.addRow(new Object[]{
                t.getId(),
                t.getNombre(),
                String.format("Q %.2f", t.getPrecioBase()),
                t.getCapacidad(),
                t.getDescripcion() != null ? t.getDescripcion() : ""
            });
        }
        lblEstado.setText("🏷  " + lista.size() + " tipo(s) de habitación");
        btnEditar  .setEnabled(false);
        btnEliminar.setEnabled(false);
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    private void abrirFormNuevo() {
        TipoHabitacionFormDialog dlg = new TipoHabitacionFormDialog(ventanaPadre, null);
        dlg.setVisible(true);
        if (dlg.isGuardadoExitoso()) cargar();
    }

    private void abrirFormEditar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int id = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        TipoHabitacion t = dao.buscarPorId(id);
        if (t == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el tipo.", "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        TipoHabitacionFormDialog dlg = new TipoHabitacionFormDialog(ventanaPadre, t);
        dlg.setVisible(true);
        if (dlg.isGuardadoExitoso()) cargar();
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int    modelo = tabla.convertRowIndexToModel(fila);
        int    id     = (int) modeloTabla.getValueAt(modelo, 0);
        String nombre = modeloTabla.getValueAt(modelo, 1).toString();

        int ok = JOptionPane.showConfirmDialog(this,
            "¿Eliminar el tipo \"" + nombre + "\"?\n" +
            "Solo es posible si no tiene habitaciones asignadas.",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (ok == JOptionPane.YES_OPTION) {
            if (dao.eliminar(id)) {
                BitacoraService.log(Bitacora.ACCION_ELIMINAR, Bitacora.MODULO_HABITACIONES,
                    "Tipo de habitación eliminado: " + nombre + " (ID: " + id + ")");
                cargar();
                lblEstado.setText("✓  Tipo eliminado.");
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar.\n" +
                    "Es posible que tenga habitaciones asignadas.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
