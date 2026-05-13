package com.hotel.modelo;

import java.sql.Timestamp;

/**
 * Modelo que representa a un cliente (huésped) del hotel.
 * Corresponde a la tabla 'clientes' en la base de datos.
 *
 * @author Fernando
 * @version 1.0
 */
public class Cliente {

    private int id;
    private String nombre;
    private String apellido;

    /** Tipo de documento: "DNI", "PASAPORTE" o "CEDULA" */
    private String tipoDocumento;

    /** Número del documento de identidad (único) */
    private String documento;

    private String telefono;
    private String email;
    private String direccion;
    private String nacionalidad;
    private boolean activo = true;
    private Timestamp fechaRegistro;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    public Cliente() {}

    public Cliente(String nombre, String apellido, String tipoDocumento,
                   String documento, String telefono, String email) {
        this.nombre        = nombre;
        this.apellido      = apellido;
        this.tipoDocumento = tipoDocumento;
        this.documento     = documento;
        this.telefono      = telefono;
        this.email         = email;
    }

    // =========================================================
    // GETTERS Y SETTERS
    // =========================================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    // =========================================================
    // MÉTODOS DE UTILIDAD
    // =========================================================

    /**
     * Devuelve el nombre completo del cliente.
     * @return "Nombre Apellido"
     */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public String toString() {
        return getNombreCompleto() + " [" + tipoDocumento + ": " + documento + "]";
    }
}
