package com.hotel.util;

import com.hotel.modelo.Usuario;

/**
 * Mantiene el usuario actualmente autenticado en la sesión.
 *
 * Al hacer login se llama a {@code SesionActual.setUsuario(u)}.
 * Al cerrar sesión se llama a {@code SesionActual.cerrar()}.
 *
 * Cualquier clase puede obtener el usuario actual con
 * {@code SesionActual.getUsuario()}.
 *
 * @author Fernando
 * @version 1.0
 */
public final class SesionActual {

    private static volatile Usuario usuario;

    private SesionActual() {}

    public static void setUsuario(Usuario u) { usuario = u; }

    public static Usuario getUsuario() { return usuario; }

    /** Cierra la sesión actual (pone el usuario en null). */
    public static void cerrar() { usuario = null; }

    /** @return true si hay un usuario autenticado */
    public static boolean estaAutenticado() { return usuario != null; }
}
