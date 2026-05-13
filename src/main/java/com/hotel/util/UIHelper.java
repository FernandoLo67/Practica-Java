package com.hotel.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Fábrica de componentes Swing reutilizables.
 *
 * Centraliza la creación de botones, campos, tablas y etiquetas
 * con el estilo del sistema, eliminando código duplicado en los paneles.
 *
 * Uso:
 *   JButton btn = UIHelper.botonPrimario("Guardar");
 *   JTextField campo = UIHelper.campoTexto();
 *
 * @author Fernando
 * @version 1.0
 */
public final class UIHelper {

    private UIHelper() {}

    // =========================================================
    // BOTONES
    // =========================================================

    /** Botón con color personalizado y efecto hover */
    public static JButton boton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(Tema.FUENTE_BOTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Color hover = color.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hover);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    public static JButton botonPrimario(String texto) {
        return boton(texto, Tema.COLOR_PRIMARIO);
    }

    public static JButton botonExito(String texto) {
        return boton(texto, Tema.COLOR_EXITO);
    }

    public static JButton botonPeligro(String texto) {
        return boton(texto, Tema.COLOR_ERROR);
    }

    public static JButton botonSecundario(String texto) {
        JButton btn = boton(texto, new Color(225, 228, 240));
        btn.setForeground(new Color(80, 85, 110));
        return btn;
    }

    public static JButton botonNeutro(String texto) {
        return boton(texto, new Color(100, 116, 139));
    }

    // =========================================================
    // CAMPOS DE TEXTO
    // =========================================================

    public static JTextField campoTexto() {
        JTextField f = new JTextField();
        f.setFont(Tema.FUENTE_CAMPO);
        f.setPreferredSize(new Dimension(0, 38));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        return f;
    }

    public static JPasswordField campoPassword() {
        JPasswordField f = new JPasswordField();
        f.setFont(Tema.FUENTE_CAMPO);
        f.setPreferredSize(new Dimension(0, 38));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 220), 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        return f;
    }

    // =========================================================
    // ETIQUETAS
    // =========================================================

    public static JLabel etiquetaCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.FUENTE_BOLD);
        l.setForeground(Tema.COLOR_TEXTO);
        return l;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.FUENTE_TITULO);
        l.setForeground(Tema.COLOR_PRIMARIO);
        return l;
    }

    // =========================================================
    // TABLAS
    // =========================================================

    /** Configura el estilo visual estándar de una JTable */
    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(Tema.FUENTE_NORMAL);
        tabla.setRowHeight(32);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(230, 233, 245));
        tabla.setSelectionBackground(Tema.COLOR_FILA_SELECCION);
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setFillsViewportHeight(true);

        tabla.getTableHeader().setFont(Tema.FUENTE_BOLD);
        tabla.getTableHeader().setBackground(Tema.COLOR_HEADER_TABLA);
        tabla.getTableHeader().setForeground(Tema.COLOR_PRIMARIO);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
    }

    /** Renderer con filas alternadas y color por estado en la columna indicada */
    public static DefaultTableCellRenderer rendererConEstado(JTable tabla, int columnaEstado) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    setBackground(row % 2 == 0 ? Tema.COLOR_FILA_PAR : Tema.COLOR_FILA_IMPAR);
                    int cm = tabla.convertColumnIndexToModel(col);
                    if (cm == columnaEstado && v != null) {
                        setForeground(Tema.colorEstado(v.toString()));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(Color.BLACK);
                        setFont(Tema.FUENTE_NORMAL);
                    }
                }
                return this;
            }
        };
    }

    /** Oculta la columna 0 (ID) de una JTable */
    public static void ocultarColumnaId(JTable tabla) {
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);
    }

    // =========================================================
    // PANELES
    // =========================================================

    /** Panel encabezado blanco con borde inferior */
    public static JPanel panelEncabezado() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.COLOR_BORDE),
            new EmptyBorder(14, 20, 14, 20)
        ));
        return p;
    }

    /** Tarjeta blanca con borde redondeado */
    public static JPanel tarjetaKPI(String icono, String titulo, JLabel lblValor, Color color) {
        JPanel t = new JPanel(new GridBagLayout());
        t.setBackground(Color.WHITE);
        t.setPreferredSize(new Dimension(155, 80));
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.COLOR_BORDE, 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;

        JLabel lblTit = new JLabel(icono + "  " + titulo);
        lblTit.setFont(Tema.FUENTE_SMALL);
        lblTit.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
        t.add(lblTit, g);

        g.gridy = 1;
        lblValor.setFont(Tema.FUENTE_KPI);
        lblValor.setForeground(color);
        t.add(lblValor, g);

        return t;
    }

    /** Barra de estado inferior estándar */
    public static JPanel barraEstado(JLabel lblEstado, JLabel lblAyuda) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(240, 242, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.COLOR_BORDE),
            new EmptyBorder(6, 16, 6, 16)
        ));
        lblEstado.setFont(Tema.FUENTE_NORMAL);
        lblEstado.setForeground(Tema.COLOR_TEXTO_SECUNDARIO);
        if (lblAyuda != null) {
            lblAyuda.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblAyuda.setForeground(new Color(150, 155, 175));
            p.add(lblAyuda, BorderLayout.EAST);
        }
        p.add(lblEstado, BorderLayout.WEST);
        return p;
    }
}
