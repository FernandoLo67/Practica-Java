package com.hotel.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para manejo seguro de contraseñas con BCrypt.
 *
 * BCrypt es un algoritmo de hashing diseñado para contraseñas:
 *   - Incluye un "salt" aleatorio automáticamente
 *   - Es lento por diseño (dificulta ataques de fuerza bruta)
 *   - El mismo password genera hashes diferentes cada vez
 *
 * Uso:
 *   String hash = PasswordUtil.hashear("miPassword");
 *   boolean ok  = PasswordUtil.verificar("miPassword", hash);
 *
 * @author Fernando
 * @version 1.0
 */
public final class PasswordUtil {

    /** Factor de costo BCrypt (10 = ~100ms por hash, buen balance seguridad/velocidad) */
    private static final int COSTO = 10;

    private PasswordUtil() {}

    /**
     * Genera el hash BCrypt de una contraseña.
     *
     * @param passwordPlano Contraseña en texto plano
     * @return Hash BCrypt seguro para almacenar en la BD
     */
    public static String hashear(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(COSTO));
    }

    /**
     * Verifica si una contraseña coincide con su hash almacenado.
     * Compatible con contraseñas antiguas en texto plano (migración).
     *
     * @param passwordPlano  Contraseña ingresada por el usuario
     * @param hashAlmacenado Hash guardado en la BD (BCrypt o texto plano)
     * @return true si la contraseña es correcta
     */
    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        if (hashAlmacenado == null || passwordPlano == null) return false;

        // Si el hash empieza con $2a$ o $2b$, es BCrypt
        if (hashAlmacenado.startsWith("$2a$") || hashAlmacenado.startsWith("$2b$")) {
            return BCrypt.checkpw(passwordPlano, hashAlmacenado);
        }

        // Backward compatibility: contraseñas antiguas en texto plano
        return passwordPlano.equals(hashAlmacenado);
    }

    /**
     * Indica si una contraseña ya está hasheada con BCrypt.
     */
    public static boolean estaHasheada(String password) {
        return password != null &&
               (password.startsWith("$2a$") || password.startsWith("$2b$"));
    }
}
