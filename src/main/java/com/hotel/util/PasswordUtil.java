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
 * Prefijos BCrypt soportados:
 *   $2a$ — formato original (Java jBCrypt)
 *   $2b$ — formato corregido (moderno)
 *   $2y$ — formato usado por PHP/OpenSSL y sistemas externos  ← [A-05] añadido
 *
 * Los tres son semánticamente equivalentes para verificación.
 *
 * Uso:
 *   String hash = PasswordUtil.hashear("miPassword");
 *   boolean ok  = PasswordUtil.verificar("miPassword", hash);
 *
 * @author Fernando
 * @version 1.1
 */
public final class PasswordUtil {

    /** Factor de costo BCrypt (10 = ~100ms por hash, buen balance seguridad/velocidad) */
    private static final int COSTO = 10;

    private PasswordUtil() {}

    /**
     * Genera el hash BCrypt de una contraseña.
     *
     * @param passwordPlano Contraseña en texto plano
     * @return Hash BCrypt seguro para almacenar en la BD (prefijo $2a$)
     */
    public static String hashear(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(COSTO));
    }

    /**
     * Verifica si una contraseña coincide con su hash almacenado.
     *
     * Soporta:
     *   - Hashes BCrypt con prefijos $2a$, $2b$, $2y$ (A-05)
     *   - Contraseñas antiguas en texto plano (migración backward-compat)
     *
     * NOTA: Si la contraseña era texto plano y es correcta, la migración
     * a BCrypt se realiza automáticamente en UsuarioDAOImpl.autenticar().
     *
     * @param passwordPlano  Contraseña ingresada por el usuario
     * @param hashAlmacenado Hash guardado en la BD (BCrypt o texto plano)
     * @return true si la contraseña es correcta
     */
    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        if (hashAlmacenado == null || passwordPlano == null) return false;

        // [A-05] Incluye $2y$ (PHP/OpenSSL) además de $2a$ y $2b$
        if (estaHasheada(hashAlmacenado)) {
            // jBCrypt acepta $2a$, $2b$ y $2y$ nativamente para checkpw
            return BCrypt.checkpw(passwordPlano, hashAlmacenado);
        }

        // Backward compatibility: contraseñas antiguas en texto plano
        return passwordPlano.equals(hashAlmacenado);
    }

    /**
     * Indica si un valor es un hash BCrypt válido.
     * Reconoce los tres prefijos estándar: $2a$, $2b$, $2y$.
     *
     * @param password El valor a verificar
     * @return true si es un hash BCrypt (no texto plano)
     */
    public static boolean estaHasheada(String password) {
        return password != null &&
               (password.startsWith("$2a$")
             || password.startsWith("$2b$")
             || password.startsWith("$2y$")); // [A-05] soporte para hashes de PHP/OpenSSL
    }
}
