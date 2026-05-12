package com.hotel.modelo;

import java.sql.Timestamp;

/**
 * Modelo que representa una factura generada al hacer el checkout.
 * Corresponde a la tabla 'facturas' en la base de datos.
 *
 * @author Fernando
 * @version 1.0
 */
public class Factura {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_PAGADA    = "PAGADA";
    public static final String ESTADO_ANULADA   = "ANULADA";

    public static final String PAGO_EFECTIVO      = "EFECTIVO";
    public static final String PAGO_TARJETA       = "TARJETA";
    public static final String PAGO_TRANSFERENCIA = "TRANSFERENCIA";

    private int id;
    private Reservacion reservacion;
    private Timestamp fechaEmision;
    private double subtotal;
    private double impuesto;
    private double total;
    private String estado;
    private String metodoPago;
    private String observaciones;

    // Constructores
    public Factura() {}

    public Factura(Reservacion reservacion, String metodoPago) {
        this.reservacion  = reservacion;
        this.subtotal     = reservacion.getTotalSinImpuesto();
        this.impuesto     = subtotal * 0.18;
        this.total        = subtotal + impuesto;
        this.estado       = ESTADO_PENDIENTE;
        this.metodoPago   = metodoPago;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Reservacion getReservacion() { return reservacion; }
    public void setReservacion(Reservacion reservacion) { this.reservacion = reservacion; }

    public Timestamp getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(Timestamp fechaEmision) { this.fechaEmision = fechaEmision; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getImpuesto() { return impuesto; }
    public void setImpuesto(double impuesto) { this.impuesto = impuesto; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public String toString() {
        return String.format("Factura #%d | Total: S/. %.2f | Estado: %s | Pago: %s",
                id, total, estado, metodoPago);
    }
}
