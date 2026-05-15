package com.hotel.vista;

import com.hotel.dao.impl.FacturaDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.Factura;
import com.hotel.modelo.Reservacion;
import com.hotel.util.BitacoraService;
import com.hotel.util.HotelConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Panel de Facturación — lista todas las facturas generadas,
 * permite marcarlas como PAGADAS o ANULADAS.
 *
 * @author Fernando
 * @version 1.0
 */
public class FacturasPanel extends JPanel {

    private JTable            tabla;
    private DefaultTableModel modeloTabla;
    private JLabel            lblEstado;
    private JButton           btnPagar;
    private JButton           btnAnular;
    private JButton           btnDetalle;
    private JButton           btnImprimir;

    private final FacturaDAOImpl facturaDAO;
    private final Frame          ventanaPadre;

    // Colores delegados a Tema.java
    private final Color COLOR_PRIMARIO   = com.hotel.util.Tema.COLOR_PRIMARIO;
    private final Color COLOR_FONDO      = com.hotel.util.Tema.COLOR_FONDO;
    private final Color COLOR_HEADER     = com.hotel.util.Tema.COLOR_HEADER_TABLA;
    private final Color COLOR_PENDIENTE  = com.hotel.util.Tema.COLOR_PENDIENTE;
    private final Color COLOR_PAGADA     = com.hotel.util.Tema.COLOR_EXITO;
    private final Color COLOR_ANULADA    = com.hotel.util.Tema.COLOR_ERROR;
    private final Color COLOR_FILA_PAR   = com.hotel.util.Tema.COLOR_FILA_PAR;
    private final Color COLOR_FILA_IMPAR = com.hotel.util.Tema.COLOR_FILA_IMPAR;

    private static final String[] COLUMNAS = {
        "ID", "Cliente", "Habitación", "Fecha Emisión", "Subtotal (Q)", "IVA (Q)", "Total (Q)", "Método Pago", "Estado"
    };

    public FacturasPanel(Frame ventanaPadre) {
        this.ventanaPadre = ventanaPadre;
        this.facturaDAO   = new FacturaDAOImpl();
        setLayout(new BorderLayout());
        setBackground(COLOR_FONDO);
        initComponents();
        cargarFacturas();
        registrarAtajos();
    }

    /** F5 = Actualizar, Ctrl+P = Imprimir PDF */
    private void registrarAtajos() {
        javax.swing.InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "actualizar");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P,
            java.awt.event.InputEvent.CTRL_DOWN_MASK), "pdf");

        am.put("actualizar", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cargarFacturas(); }
        });
        am.put("pdf", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnImprimir.isEnabled()) imprimirFacturaPDF();
            }
        });
    }


    private void initComponents() {
        add(crearEncabezado(),  BorderLayout.NORTH);
        add(crearCuerpo(),      BorderLayout.CENTER);
        add(crearBarra(),       BorderLayout.SOUTH);
    }

    private JPanel crearEncabezado() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 220, 240)),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel lbl = new JLabel("🧾  Facturación");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(COLOR_PRIMARIO);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);

        JButton btnRefresh = crearBoton("🔄 Actualizar", new Color(100, 116, 139), false);
        btnDetalle = crearBoton("📄 Ver Detalle", COLOR_PRIMARIO, true);
        btnPagar   = crearBoton("✅ Marcar Pagada", new Color(46, 125, 50), true);
        btnAnular  = crearBoton("✖ Anular", new Color(198, 40, 40), true);

        btnImprimir = crearBoton("🖨 Imprimir PDF", new Color(123, 31, 162), true);

        btnRefresh .addActionListener(e -> cargarFacturas());
        btnDetalle .addActionListener(e -> verDetalle());
        btnPagar   .addActionListener(e -> cambiarEstado(Factura.ESTADO_PAGADA));
        btnAnular  .addActionListener(e -> cambiarEstado(Factura.ESTADO_ANULADA));
        btnImprimir.addActionListener(e -> imprimirFacturaPDF());

        botones.add(btnRefresh);
        botones.add(btnDetalle);
        botones.add(btnImprimir);
        botones.add(btnPagar);
        botones.add(btnAnular);

        p.add(lbl,     BorderLayout.WEST);
        p.add(botones, BorderLayout.EAST);
        return p;
    }

    private JPanel crearCuerpo() {
        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setBackground(COLOR_FONDO);
        cuerpo.add(crearTarjetas(), BorderLayout.NORTH);
        cuerpo.add(crearTabla(),    BorderLayout.CENTER);
        return cuerpo;
    }

    private JPanel crearTarjetas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 14));
        panel.setBackground(COLOR_FONDO);

        JLabel lblPend  = new JLabel("0");
        JLabel lblPag   = new JLabel("0");
        JLabel lblTotal = new JLabel("Q 0.00");

        actualizarTarjetasConLabels(lblPend, lblPag, lblTotal);

        panel.add(tarjeta("⏳", "Pendientes",    lblPend,  COLOR_PENDIENTE));
        panel.add(tarjeta("✅", "Pagadas",        lblPag,   COLOR_PAGADA));
        panel.add(tarjeta("💰", "Ingresos Totales", lblTotal, new Color(63, 81, 181)));

        return panel;
    }

    private void actualizarTarjetasConLabels(JLabel p, JLabel pg, JLabel tot) {
        p  .setText(String.valueOf(facturaDAO.contarPorEstado(Factura.ESTADO_PENDIENTE)));
        pg .setText(String.valueOf(facturaDAO.contarPorEstado(Factura.ESTADO_PAGADA)));
        tot.setText(String.format("Q %.2f", facturaDAO.sumarTotalPorEstado(Factura.ESTADO_PAGADA)));
    }

    private JPanel tarjeta(String icono, String titulo, JLabel numero, Color color) {
        JPanel t = new JPanel(new GridBagLayout());
        t.setBackground(Color.WHITE);
        t.setPreferredSize(new Dimension(180, 80));
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 220, 240), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(icono + "  " + titulo);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(100, 110, 140));
        t.add(lbl, g);
        g.gridy = 1;
        numero.setFont(new Font("Segoe UI", Font.BOLD, 22));
        numero.setForeground(color);
        t.add(numero, g);
        return t;
    }

    private JScrollPane crearTabla() {
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(230, 233, 245));
        tabla.setSelectionBackground(new Color(197, 210, 255));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(COLOR_HEADER);
        tabla.getTableHeader().setForeground(COLOR_PRIMARIO);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tabla.setRowSorter(new TableRowSorter<>(modeloTabla));

        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 150, 80, 130, 100, 90, 100, 110, 90};
        for (int i = 0; i < anchos.length; i++)
            if (anchos[i] > 0)
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    setBackground(row % 2 == 0 ? COLOR_FILA_PAR : COLOR_FILA_IMPAR);
                    int cm = tabla.convertColumnIndexToModel(col);
                    if (cm == 8 && v != null) {
                        switch (v.toString()) {
                            case "PENDIENTE": setForeground(COLOR_PENDIENTE); break;
                            case "PAGADA":    setForeground(COLOR_PAGADA);    break;
                            case "ANULADA":   setForeground(COLOR_ANULADA);   break;
                            default:          setForeground(Color.BLACK);
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(Color.BLACK);
                        setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    }
                }
                return this;
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) verDetalle();
            }
        });

        tabla.getSelectionModel().addListSelectionListener(e -> {
            boolean hay = tabla.getSelectedRow() >= 0;
            btnPagar.setEnabled(hay);
            btnAnular.setEnabled(hay);
            btnDetalle.setEnabled(hay);
            btnImprimir.setEnabled(hay);
        });

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
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(90, 95, 120));
        p.add(lblEstado, BorderLayout.WEST);
        return p;
    }

    public void cargarFacturas() {
        modeloTabla.setRowCount(0);
        List<Factura> lista = facturaDAO.listarTodas();
        for (Factura f : lista) {
            modeloTabla.addRow(new Object[]{
                f.getId(),
                f.getReservacion().getCliente().getNombreCompleto(),
                "Hab. " + f.getReservacion().getHabitacion().getNumero(),
                f.getFechaEmision() != null ? f.getFechaEmision().toString().substring(0, 16) : "-",
                String.format("Q %.2f", f.getSubtotal()),
                String.format("Q %.2f", f.getImpuesto()),
                String.format("Q %.2f", f.getTotal()),
                f.getMetodoPago(),
                f.getEstado()
            });
        }
        lblEstado.setText("🧾  " + lista.size() + " factura(s) encontradas");
        btnPagar.setEnabled(false);
        btnAnular.setEnabled(false);
        btnDetalle.setEnabled(false);
        btnImprimir.setEnabled(false);
    }

    private void verDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int filaM = tabla.convertRowIndexToModel(fila);
        int id = (int) modeloTabla.getValueAt(filaM, 0);
        Factura f = facturaDAO.buscarPorId(id);
        if (f == null) return;

        String detalle = String.format(
            "══════════════════════════════════\n" +
            "          FACTURA #%d\n" +
            "══════════════════════════════════\n" +
            "Cliente:       %s\n" +
            "Documento:     %s\n" +
            "Habitación:    %s (Piso %d)\n" +
            "Tipo:          %s\n" +
            "Check-In:      %s\n" +
            "Check-Out:     %s\n" +
            "Noches:        %d\n" +
            "──────────────────────────────────\n" +
            "Subtotal:      Q %.2f\n" +
            "IVA (18%%):     Q %.2f\n" +
            "TOTAL:         Q %.2f\n" +
            "──────────────────────────────────\n" +
            "Método de pago: %s\n" +
            "Estado:         %s\n" +
            "Fecha emisión:  %s\n" +
            "══════════════════════════════════",
            f.getId(),
            f.getReservacion().getCliente().getNombreCompleto(),
            f.getReservacion().getCliente().getDocumento(),
            f.getReservacion().getHabitacion().getNumero(),
            f.getReservacion().getHabitacion().getPiso(),
            f.getReservacion().getHabitacion().getTipo().getNombre(),
            f.getReservacion().getFechaCheckin(),
            f.getReservacion().getFechaCheckout(),
            f.getReservacion().getNoches(),
            f.getSubtotal(), f.getImpuesto(), f.getTotal(),
            f.getMetodoPago(), f.getEstado(),
            f.getFechaEmision() != null ? f.getFechaEmision().toString().substring(0, 16) : "-"
        );

        JTextArea area = new JTextArea(detalle);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(250, 252, 255));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Detalle de Factura #" + f.getId(), JOptionPane.PLAIN_MESSAGE);
    }

    private void cambiarEstado(String nuevoEstado) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int filaM = tabla.convertRowIndexToModel(fila);
        int id    = (int)    modeloTabla.getValueAt(filaM, 0);
        String est = (String) modeloTabla.getValueAt(filaM, 8);

        if (est.equals(nuevoEstado)) {
            JOptionPane.showMessageDialog(this, "La factura ya está en estado " + nuevoEstado + ".");
            return;
        }

        String accion = nuevoEstado.equals(Factura.ESTADO_PAGADA) ? "marcar como PAGADA" : "ANULAR";
        int op = JOptionPane.showConfirmDialog(this,
            "¿Deseas " + accion + " la Factura #" + id + "?",
            "Confirmar", JOptionPane.YES_NO_OPTION);

        if (op == JOptionPane.YES_OPTION) {
            if (facturaDAO.actualizarEstado(id, nuevoEstado)) {
                BitacoraService.log(Bitacora.ACCION_CAMBIAR_ESTADO,
                    Bitacora.MODULO_FACTURAS,
                    "Factura #" + id + " → " + nuevoEstado +
                    " (antes: " + est + ")");
                cargarFacturas();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMPRESIÓN PDF
    // ─────────────────────────────────────────────────────────────────────────

    private void imprimirFacturaPDF() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        int filaM = tabla.convertRowIndexToModel(fila);
        int id    = (int) modeloTabla.getValueAt(filaM, 0);

        Factura f = facturaDAO.buscarPorId(id);
        if (f == null) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar la factura.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ── Elegir ubicación de guardado ──────────────────────────────────────
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar Factura PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        String nombreSugerido = "Factura_" + f.getId() + "_" +
            f.getReservacion().getCliente().getNombreCompleto()
                .replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        fc.setSelectedFile(new File(nombreSugerido));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File destino = fc.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf"))
            destino = new File(destino.getAbsolutePath() + ".pdf");

        // ── Generar PDF con PDFBox ────────────────────────────────────────────
        try {
            generarPDF(f, destino);
            lblEstado.setText("PDF generado: " + destino.getName());

            int op = JOptionPane.showConfirmDialog(this,
                "Factura guardada en:\n" + destino.getAbsolutePath() +
                "\n\n¿Deseas abrir el archivo ahora?",
                "PDF generado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                try { Desktop.getDesktop().open(destino); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                        "No se pudo abrir el archivo automaticamente.\nUbicacion: " + destino.getAbsolutePath(),
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Error al generar el PDF:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarPDF(Factura f, File destino) throws IOException {
        // Datos de la factura
        Reservacion res = f.getReservacion();
        String nombreCliente  = res.getCliente().getNombreCompleto();
        String docCliente     = res.getCliente().getDocumento();
        String numHab         = res.getHabitacion().getNumero();
        int    pisoHab        = res.getHabitacion().getPiso();
        String tipoHab        = res.getHabitacion().getTipo().getNombre();
        String checkin        = res.getFechaCheckin()  != null ? res.getFechaCheckin().toString()  : "-";
        String checkout       = res.getFechaCheckout() != null ? res.getFechaCheckout().toString() : "-";
        int    noches         = (int) res.getNoches();
        String fechaEmision   = f.getFechaEmision() != null
            ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(f.getFechaEmision()) : "-";

        // Colores
        Color azulOscuro = new Color(26, 35, 126);
        Color azulClaro  = new Color(63, 81, 181);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float W  = page.getMediaBox().getWidth();   // 595 pt
            float H  = page.getMediaBox().getHeight();  // 842 pt
            float mg = 48f;                              // margen izquierdo/derecho

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ── Encabezado azul ──────────────────────────────────────────
                setColor(cs, azulOscuro);
                cs.addRect(0, H - 90, W, 90);
                cs.fill();

                // Nombre del hotel
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                setColorText(cs, Color.WHITE);
                cs.newLineAtOffset(mg, H - 42);
                cs.showText(HotelConfig.getNombre());
                cs.endText();

                // Slogan y dirección
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                setColorText(cs, new Color(200, 210, 255));
                cs.newLineAtOffset(mg, H - 57);
                cs.showText(HotelConfig.getSlogan());
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                setColorText(cs, new Color(200, 210, 255));
                cs.newLineAtOffset(mg, H - 70);
                cs.showText(HotelConfig.getDireccion() + "   |   " +
                    HotelConfig.getTelefono() + "   |   " + HotelConfig.getEmail());
                cs.endText();

                // NIT (derecha)
                String nitTxt = "NIT: " + HotelConfig.getNit();
                float nitW = PDType1Font.HELVETICA_BOLD.getStringWidth(nitTxt) / 1000 * 10;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                setColorText(cs, Color.WHITE);
                cs.newLineAtOffset(W - mg - nitW, H - 42);
                cs.showText(nitTxt);
                cs.endText();

                // ── Título FACTURA ────────────────────────────────────────────
                float y = H - 115;
                setColor(cs, azulClaro);
                cs.addRect(mg, y, W - 2 * mg, 26);
                cs.fill();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                setColorText(cs, Color.WHITE);
                cs.newLineAtOffset(mg + 8, y + 8);
                cs.showText("FACTURA No. " + String.format("%05d", f.getId()));
                cs.endText();

                // Fecha emisión (derecha del banner)
                String fechaTxt = "Emision: " + fechaEmision;
                float fechaW = PDType1Font.HELVETICA.getStringWidth(fechaTxt) / 1000 * 11;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                setColorText(cs, Color.WHITE);
                cs.newLineAtOffset(W - mg - fechaW - 8, y + 8);
                cs.showText(fechaTxt);
                cs.endText();

                // ── Sección datos del cliente ─────────────────────────────────
                y -= 30;
                setColorText(cs, azulOscuro);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                setColorText(cs, azulOscuro);
                cs.newLineAtOffset(mg, y);
                cs.showText("DATOS DEL CLIENTE");
                cs.endText();

                lineaHorizontal(cs, mg, y - 4, W - 2 * mg, azulClaro);
                y -= 18;

                y = filaTexto(cs, mg, y, "Cliente:",  nombreCliente);
                y = filaTexto(cs, mg, y, "Documento:", docCliente);

                // ── Sección datos de la reservación ───────────────────────────
                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                setColorText(cs, azulOscuro);
                cs.newLineAtOffset(mg, y);
                cs.showText("DETALLE DE LA RESERVACION");
                cs.endText();

                lineaHorizontal(cs, mg, y - 4, W - 2 * mg, azulClaro);
                y -= 18;

                y = filaTexto(cs, mg, y, "Habitacion:",   "No. " + numHab + " — Piso " + pisoHab + " — " + tipoHab);
                y = filaTexto(cs, mg, y, "Check-In:",     checkin);
                y = filaTexto(cs, mg, y, "Check-Out:",    checkout);
                y = filaTexto(cs, mg, y, "Noches:",       String.valueOf(noches));

                // ── Tabla de importes ─────────────────────────────────────────
                y -= 20;
                float colLabel = mg;
                float colValue = W - mg - 90;
                float rowH     = 22f;

                // Cabecera tabla
                setColor(cs, azulOscuro);
                cs.addRect(colLabel, y - 2, W - 2 * mg, rowH);
                cs.fill();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                setColorText(cs, Color.WHITE);
                cs.newLineAtOffset(colLabel + 8, y + 6);
                cs.showText("CONCEPTO");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                setColorText(cs, Color.WHITE);
                cs.newLineAtOffset(colValue, y + 6);
                cs.showText("IMPORTE");
                cs.endText();

                y -= rowH;

                // Fila Subtotal
                y = filaImporte(cs, colLabel, colValue, y, rowH,
                    "Hospedaje (" + noches + " noche(s) x Q " +
                        String.format("%.2f", f.getSubtotal() / noches) + ")",
                    String.format("Q %.2f", f.getSubtotal()), new Color(240, 244, 255));

                // Fila IVA
                y = filaImporte(cs, colLabel, colValue, y, rowH,
                    "IVA (18%)",
                    String.format("Q %.2f", f.getImpuesto()), Color.WHITE);

                // Separador
                lineaHorizontal(cs, mg, y + rowH - 4, W - 2 * mg, azulClaro);

                // Fila Total — resaltada
                setColor(cs, new Color(230, 235, 255));
                cs.addRect(colLabel, y - 2, W - 2 * mg, rowH);
                cs.fill();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                setColorText(cs, azulOscuro);
                cs.newLineAtOffset(colLabel + 8, y + 6);
                cs.showText("TOTAL A PAGAR");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                setColorText(cs, azulOscuro);
                cs.newLineAtOffset(colValue, y + 5);
                cs.showText(String.format("Q %.2f", f.getTotal()));
                cs.endText();

                y -= rowH + 18;

                // ── Método de pago y estado ───────────────────────────────────
                y = filaTexto(cs, mg, y, "Metodo de pago:", f.getMetodoPago());
                Color colorEstado = "PAGADA".equals(f.getEstado()) ? new Color(27, 94, 32)
                    : "ANULADA".equals(f.getEstado()) ? new Color(183, 28, 28)
                    : new Color(230, 81, 0);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                setColorText(cs, new Color(80, 80, 80));
                cs.newLineAtOffset(mg, y);
                cs.showText("Estado:");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                setColorText(cs, colorEstado);
                cs.newLineAtOffset(mg + 130, y);
                cs.showText(f.getEstado());
                cs.endText();
                y -= 16;

                // ── Pie de página ─────────────────────────────────────────────
                float pieY = 36f;
                lineaHorizontal(cs, mg, pieY + 14, W - 2 * mg, new Color(180, 190, 220));

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                setColorText(cs, new Color(130, 140, 170));
                cs.newLineAtOffset(mg, pieY);
                cs.showText("Este documento es la factura oficial emitida por " +
                    HotelConfig.getNombre() + ".  Conserve este comprobante.");
                cs.endText();

                String webTxt = HotelConfig.getWeb();
                float webW = PDType1Font.HELVETICA.getStringWidth(webTxt) / 1000 * 8;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                setColorText(cs, new Color(130, 140, 170));
                cs.newLineAtOffset(W - mg - webW, pieY);
                cs.showText(webTxt);
                cs.endText();
            }

            doc.save(destino);
        }
    }

    // ── Helpers de dibujo PDFBox ──────────────────────────────────────────────

    private void setColor(PDPageContentStream cs, Color c) throws IOException {
        cs.setNonStrokingColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
    }

    private void setColorText(PDPageContentStream cs, Color c) throws IOException {
        cs.setNonStrokingColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
    }

    private void lineaHorizontal(PDPageContentStream cs, float x, float y, float ancho, Color c) throws IOException {
        setColor(cs, c);
        cs.addRect(x, y, ancho, 1);
        cs.fill();
    }

    /** Dibuja una fila etiqueta-valor y retorna la siguiente Y. */
    private float filaTexto(PDPageContentStream cs, float x, float y, String label, String valor) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        setColorText(cs, new Color(80, 80, 80));
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        setColorText(cs, new Color(30, 30, 30));
        cs.newLineAtOffset(x + 130, y);
        cs.showText(valor);
        cs.endText();

        return y - 16;
    }

    /** Dibuja una fila de la tabla de importes y retorna la siguiente Y. */
    private float filaImporte(PDPageContentStream cs, float xLabel, float xValue,
            float y, float rowH, String concepto, String importe, Color fondo) throws IOException {
        setColor(cs, fondo);
        cs.addRect(xLabel, y - 2, xValue + 80 - xLabel, rowH);
        cs.fill();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        setColorText(cs, new Color(50, 50, 50));
        cs.newLineAtOffset(xLabel + 8, y + 6);
        cs.showText(concepto);
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        setColorText(cs, new Color(50, 50, 50));
        cs.newLineAtOffset(xValue, y + 6);
        cs.showText(importe);
        cs.endText();

        return y - rowH;
    }

    private JButton crearBoton(String texto, Color color, boolean deshabilitado) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setEnabled(!deshabilitado);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(color.darker());
            }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(color); }
        });
        return b;
    }
}
