package com.hotel.modelo;

import java.sql.Timestamp;

/**
 * Modelo para el registro de auditoría del sistema.
 * Corresponde a la tabla 'bitacora' en la base de datos.
 *
 * @author Fernando
 * @version 1.0
 */
public class Bitacora {

    // Acciones estándar del sistema
    public static final String ACCION_LOGIN          = "LOGIN";
    public static final String ACCION_LOGIN_FALLIDO  = "LOGIN_FALLIDO";
    public static final String ACCION_LOGOUT         = "LOGOUT";
    public static final String ACCION_CREAR          = "CREAR";
    public static final String ACCION_EDITAR         = "EDITAR";
    public static final String ACCION_ELIMINAR       = "ELIMINAR";
    public static final String ACCION_CHECKIN        = "CHECK_IN";
    public static final String ACCION_CHECKOUT       = "CHECK_OUT";
    public static final String ACCION_CAMBIAR_ESTADO = "CAMBIAR_ESTADO";

    // Módulos del sistema
    public static final String MODULO_SISTEMA        = "SISTEMA";
    public static final String MODULO_CLIENTES       = "CLIENTES";
    public static final String MODULO_HABITACIONES   = "HABITACIONES";
    public static final String MODULO_RESERVACIONES  = "RESERVACIONES";
    public static final String MODULO_CHECKINOUT     = "CHECK_IN_OUT";
    public static final String MODULO_FACTURAS       = "FACTURAS";
    public static final String MODULO_USUARIOS       = "USUARIOS";

    private int       id;
    private Integer   idUsuario;      // puede ser null (login fallido)
    private String    usuarioNombre;
    private String    accion;
    private String    modulo;
    private String    descripcion;
    private Timestamp fecha;

    public Bitacora() {}

    public Bitacora(Integer idUsuario, String usuarioNombre,
                    String accion, String modulo, String descripcion) {
        this.idUsuario     = idUsuario;
        this.usuarioNombre = usuarioNombre;
        this.accion        = accion;
        this.modulo        = modulo;
        this.descripcion   = descripcion;
    }

    public int       getId()            { return id; }
    public void      setId(int id)      { this.id = id; }

    public Integer   getIdUsuario()                    { return idUsuario; }
    public void      setIdUsuario(Integer idUsuario)   { this.idUsuario = idUsuario; }

    public String    getUsuarioNombre()                    { return usuarioNombre; }
    public void      setUsuarioNombre(String usuarioNombre){ this.usuarioNombre = usuarioNombre; }

    public String    getAccion()                  { return accion; }
    public void      setAccion(String accion)     { this.accion = accion; }

    public String    getModulo()                  { return modulo; }
    public void      setModulo(String modulo)     { this.modulo = modulo; }

    public String    getDescripcion()                     { return descripcion; }
    public void      setDescripcion(String descripcion)   { this.descripcion = descripcion; }

    public Timestamp getFecha()                  { return fecha; }
    public void      setFecha(Timestamp fecha)   { this.fecha = fecha; }

    @Override
    public String toString() {
        return "Bitacora{id=" + id + ", usuario=" + usuarioNombre +
               ", accion=" + accion + ", modulo=" + modulo + ", fecha=" + fecha + "}";
    }
}
