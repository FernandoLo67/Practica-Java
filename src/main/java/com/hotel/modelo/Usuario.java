package com.hotel.modelo;

import java.sql.Timestamp;

/**
 * Modelo que representa a un usuario del sistema.
 *
 * Un usuario puede ser ADMIN o RECEPCIONISTA.
 * Esta clase es un POJO (Plain Old Java Object): solo tiene
 * atributos, getters y setters. No tiene lógica de negocio.
 *
 * Corresponde a la tabla 'usuarios' en la base de datos.
 *
 * @author Fernando
 * @version 1.0
 */
public class Usuario {

    // =========================================================
    // ATRIBUTOS (corresponden a columnas de la tabla 'usuarios')
    // =========================================================

    private int id;
    private String nombre;
    private String usuario;
    private String password;

    /** Rol del usuario: "ADMIN" o "RECEPCIONISTA" */
    private String rol;

    /** true = puede usar el sistema, false = cuenta desactivada */
    private boolean activo;

    private Timestamp fechaCreacion;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    /** Constructor vacío requerido por JDBC y frameworks */
    public Usuario() {}

    /**
     * Constructor para crear un usuario nuevo.
     *
     * @param nombre   Nombre completo
     * @param usuario  Nombre de usuario para login
     * @param password Contraseña
     * @param rol      "ADMIN" o "RECEPCIONISTA"
     */
    public Usuario(String nombre, String usuario, String password, String rol) {
        this.nombre   = nombre;
        this.usuario  = usuario;
        this.password = password;
        this.rol      = rol;
        this.activo   = true;
    }

    // =========================================================
    // GETTERS Y SETTERS
    // =========================================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // =========================================================
    // MÉTODOS DE UTILIDAD
    // =========================================================

    /**
     * Verifica si el usuario tiene rol de Administrador.
     * @return true si es ADMIN
     */
    public boolean esAdmin() {
        return "ADMIN".equalsIgnoreCase(this.rol);
    }

    /**
     * Representación en texto del objeto (útil para debugging).
     */
    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nombre='" + nombre + "', usuario='" + usuario +
               "', rol='" + rol + "', activo=" + activo + "}";
    }
}
