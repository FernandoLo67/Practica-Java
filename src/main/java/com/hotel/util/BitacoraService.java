package com.hotel.util;

import com.hotel.dao.impl.BitacoraDAOImpl;
import com.hotel.modelo.Bitacora;
import com.hotel.modelo.Usuario;

/**
 * Servicio estático de auditoría.
 *
 * Centraliza el registro en la bitácora desde cualquier clase del sistema.
 *
 * Uso típico:
 * <pre>
 *   BitacoraService.log(Bitacora.ACCION_CREAR, Bitacora.MODULO_CLIENTES,
 *                       "Cliente creado: " + cliente.getNombreCompleto());
 * </pre>
 *
 * El usuario actual se obtiene automáticamente de {@link SesionActual}.
 *
 * @author Fernando
 * @version 1.0
 */
public final class BitacoraService {

    private static final BitacoraDAOImpl dao = new BitacoraDAOImpl();

    private BitacoraService() {}

    /**
     * Registra una acción usando el usuario actualmente en sesión.
     */
    public static void log(String accion, String modulo, String descripcion) {
        Usuario u = SesionActual.getUsuario();
        registrar(u, accion, modulo, descripcion);
    }

    /**
     * Registra una acción con un usuario explícito (útil en login/logout
     * donde la sesión puede no estar establecida aún).
     */
    public static void log(Usuario usuario, String accion, String modulo, String descripcion) {
        registrar(usuario, accion, modulo, descripcion);
    }

    // =========================================================
    // INTERNOS
    // =========================================================

    private static void registrar(Usuario usuario, String accion, String modulo, String descripcion) {
        Bitacora b = new Bitacora();
        if (usuario != null) {
            b.setIdUsuario(usuario.getId());
            b.setUsuarioNombre(usuario.getNombre() + " (" + usuario.getUsuario() + ")");
        } else {
            b.setUsuarioNombre("Sistema");
        }
        b.setAccion(accion);
        b.setModulo(modulo);
        b.setDescripcion(descripcion);
        dao.registrar(b);
    }
}
