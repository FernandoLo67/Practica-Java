package com.hotel.exception;

/**
 * Excepción personalizada del sistema hotelero.
 *
 * Se usa para propagar errores de negocio y de base de datos
 * de forma controlada hacia la capa de vista, en lugar de
 * simplemente imprimir en consola y retornar null/false.
 *
 * Ejemplos de uso:
 *   throw new HotelException("No se pudo guardar el cliente", e);
 *   throw new HotelException("La habitación ya está ocupada");
 *
 * @author Fernando
 * @version 1.0
 */
public class HotelException extends RuntimeException {

    private final String codigoError;

    public HotelException(String mensaje) {
        super(mensaje);
        this.codigoError = "ERROR_GENERAL";
    }

    public HotelException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = "ERROR_BD";
    }

    public HotelException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public HotelException(String codigoError, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = codigoError;
    }

    public String getCodigoError() {
        return codigoError;
    }

    /** Códigos de error estándar del sistema */
    public static final String HABITACION_OCUPADA      = "HABITACION_OCUPADA";
    public static final String CLIENTE_DUPLICADO       = "CLIENTE_DUPLICADO";
    public static final String USUARIO_DUPLICADO       = "USUARIO_DUPLICADO";
    public static final String FECHA_INVALIDA          = "FECHA_INVALIDA";
    public static final String RESERVACION_CONFLICTO   = "RESERVACION_CONFLICTO";
    public static final String CONEXION_FALLIDA        = "CONEXION_FALLIDA";
    public static final String PERMISO_DENEGADO        = "PERMISO_DENEGADO";
}
