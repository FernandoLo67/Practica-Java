package com.hotel.modelo;

/**
 * Modelo que representa el tipo de una habitación.
 * Ejemplos: Simple, Doble, Suite, Familiar.
 * Corresponde a la tabla 'tipo_habitacion' en la base de datos.
 *
 * @author Fernando
 * @version 1.0
 */
public class TipoHabitacion {

    private int id;
    private String nombre;
    private String descripcion;

    /** Precio por noche en la moneda del sistema */
    private double precioBase;

    /** Número máximo de personas que puede alojar */
    private int capacidad;

    // Constructores
    public TipoHabitacion() {}

    public TipoHabitacion(String nombre, String descripcion, double precioBase, int capacidad) {
        this.nombre      = nombre;
        this.descripcion = descripcion;
        this.precioBase  = precioBase;
        this.capacidad   = capacidad;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecioBase() { return precioBase; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    @Override
    public String toString() {
        return nombre + " - S/. " + String.format("%.2f", precioBase) + "/noche";
    }
}
