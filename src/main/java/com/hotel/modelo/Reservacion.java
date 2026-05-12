package com.hotel.modelo;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;

/**
 * Modelo que representa una reservación en el hotel.
 * Vincula un cliente con una habitación en un período de fechas.
 * Corresponde a la tabla 'reservaciones' en la base de datos.
 *
 * Ciclo de vida de una reservación:
 *   PENDIENTE → CONFIRMADA → CHECKIN → CHECKOUT
 *   (en cualquier punto puede pasar a CANCELADA)
 *
 * @author Fernando
 * @version 1.0
 */
public class Reservacion {

    // Estados como constantes
    public static final String ESTADO_PENDIENTE   = "PENDIENTE";
    public static final String ESTADO_CONFIRMADA  = "CONFIRMADA";
    public static final String ESTADO_CHECKIN     = "CHECKIN";
    public static final String ESTADO_CHECKOUT    = "CHECKOUT";
    public static final String ESTADO_CANCELADA   = "CANCELADA";

    private int id;
    private Cliente cliente;
    private Habitacion habitacion;
    private Date fechaCheckin;
    private Date fechaCheckout;
    private String estado;
    private String observaciones;
    private Usuario usuarioRegistro;
    private Timestamp fechaReserva;

    // Constructores
    public Reservacion() {}

    public Reservacion(Cliente cliente, Habitacion habitacion,
                       Date fechaCheckin, Date fechaCheckout, Usuario usuario) {
        this.cliente         = cliente;
        this.habitacion      = habitacion;
        this.fechaCheckin    = fechaCheckin;
        this.fechaCheckout   = fechaCheckout;
        this.estado          = ESTADO_PENDIENTE;
        this.usuarioRegistro = usuario;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Habitacion getHabitacion() { return habitacion; }
    public void setHabitacion(Habitacion habitacion) { this.habitacion = habitacion; }

    public Date getFechaCheckin() { return fechaCheckin; }
    public void setFechaCheckin(Date fechaCheckin) { this.fechaCheckin = fechaCheckin; }

    public Date getFechaCheckout() { return fechaCheckout; }
    public void setFechaCheckout(Date fechaCheckout) { this.fechaCheckout = fechaCheckout; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Usuario getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(Usuario usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public Timestamp getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(Timestamp fechaReserva) { this.fechaReserva = fechaReserva; }

    // Métodos de utilidad

    /**
     * Calcula el número de noches de la estadía.
     * @return número de noches (diferencia entre checkout y checkin)
     */
    public long getNoches() {
        if (fechaCheckin == null || fechaCheckout == null) return 0;
        return ChronoUnit.DAYS.between(
            fechaCheckin.toLocalDate(),
            fechaCheckout.toLocalDate()
        );
    }

    /**
     * Calcula el total de la reservación sin impuestos.
     * @return precio total (noches × precio por noche)
     */
    public double getTotalSinImpuesto() {
        if (habitacion == null) return 0;
        return getNoches() * habitacion.getPrecioNoche();
    }

    /**
     * Calcula el total con impuesto (18% IGV).
     * @return precio total con impuesto
     */
    public double getTotalConImpuesto() {
        return getTotalSinImpuesto() * 1.18;
    }

    @Override
    public String toString() {
        return "Reservacion{id=" + id +
               ", cliente=" + (cliente != null ? cliente.getNombreCompleto() : "N/A") +
               ", habitacion=" + (habitacion != null ? habitacion.getNumero() : "N/A") +
               ", checkin=" + fechaCheckin +
               ", checkout=" + fechaCheckout +
               ", estado='" + estado + "'}";
    }
}
