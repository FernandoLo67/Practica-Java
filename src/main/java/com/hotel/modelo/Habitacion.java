package com.hotel.modelo;

/**
 * Modelo que representa una habitación física del hotel.
 * Corresponde a la tabla 'habitaciones' en la base de datos.
 *
 * Estados posibles:
 *   DISPONIBLE  - La habitación está libre para reservar
 *   OCUPADA     - Hay huéspedes actualmente
 *   RESERVADA   - Tiene una reserva confirmada pendiente
 *   MANTENIMIENTO - En limpieza o reparación
 *
 * @author Fernando
 * @version 1.0
 */
public class Habitacion {

    // Estados como constantes (evita errores de tipeo)
    public static final String ESTADO_DISPONIBLE    = "DISPONIBLE";
    public static final String ESTADO_OCUPADA       = "OCUPADA";
    public static final String ESTADO_RESERVADA     = "RESERVADA";
    public static final String ESTADO_MANTENIMIENTO = "MANTENIMIENTO";

    private int id;

    /** Número de habitación: "101", "202", "SUITE-1", etc. */
    private String numero;

    private int piso;

    /** Tipo de habitación (relación con TipoHabitacion) */
    private TipoHabitacion tipo;

    private String estado;
    private String descripcion;
    private String imagenUrl;

    /**
     * Precio especial por noche para ESTA habitación.
     * Cuando está seteado, sobreescribe el precio base del tipo.
     * null = usar precio del tipo.
     */
    private Double precioEspecial;

    // Constructores
    public Habitacion() {}

    public Habitacion(String numero, int piso, TipoHabitacion tipo) {
        this.numero = numero;
        this.piso   = piso;
        this.tipo   = tipo;
        this.estado = ESTADO_DISPONIBLE;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public int getPiso() { return piso; }
    public void setPiso(int piso) { this.piso = piso; }

    public TipoHabitacion getTipo() { return tipo; }
    public void setTipo(TipoHabitacion tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Double getPrecioEspecial() { return precioEspecial; }
    public void setPrecioEspecial(Double precioEspecial) { this.precioEspecial = precioEspecial; }

    // Métodos de utilidad

    /** @return true si la habitación está libre para reservar */
    public boolean estaDisponible() {
        return ESTADO_DISPONIBLE.equals(this.estado);
    }

    /**
     * Retorna el precio efectivo por noche:
     *   - Si precioEspecial está seteado (no null), lo usa.
     *   - De lo contrario usa el precio base del tipo.
     * Esto permite fijar tarifas individuales por habitación sin
     * modificar la tarifa del tipo completo.
     */
    public double getPrecioNoche() {
        if (precioEspecial != null) return precioEspecial;
        return tipo != null ? tipo.getPrecioBase() : 0;
    }

    @Override
    public String toString() {
        return "Hab. " + numero + " (Piso " + piso + ") - " +
               (tipo != null ? tipo.getNombre() : "Sin tipo") + " [" + estado + "]";
    }
}
