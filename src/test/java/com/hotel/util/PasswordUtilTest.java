package com.hotel.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para PasswordUtil.
 *
 * Verifica que el hashing BCrypt y la verificación retrocompatible
 * funcionen correctamente en todos los escenarios.
 *
 * @author Fernando
 */
@DisplayName("PasswordUtil — Hash BCrypt y verificación")
class PasswordUtilTest {

    // =========================================================
    // hashear()
    // =========================================================

    @Test
    @DisplayName("hashear() devuelve un hash BCrypt válido")
    void hashear_devuelveHashBcrypt() {
        String hash = PasswordUtil.hashear("miPassword123");

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
            "El hash debe iniciar con $2a$ o $2b$ (prefijo BCrypt)");
        assertTrue(hash.length() > 50, "El hash BCrypt debe tener más de 50 caracteres");
    }

    @Test
    @DisplayName("hashear() genera hashes diferentes para la misma contraseña (salt aleatorio)")
    void hashear_saltAleatorio_generaHashesDiferentes() {
        String pass = "mismaContrasena";
        String hash1 = PasswordUtil.hashear(pass);
        String hash2 = PasswordUtil.hashear(pass);

        assertNotEquals(hash1, hash2,
            "BCrypt debe generar hashes diferentes por el salt aleatorio");
    }

    // =========================================================
    // verificar() — con hash BCrypt
    // =========================================================

    @Test
    @DisplayName("verificar() devuelve true con contraseña correcta y hash BCrypt")
    void verificar_contrasenaCorrecta_devuelveTrue() {
        String pass = "Admin#2024";
        String hash = PasswordUtil.hashear(pass);

        assertTrue(PasswordUtil.verificar(pass, hash));
    }

    @Test
    @DisplayName("verificar() devuelve false con contraseña incorrecta y hash BCrypt")
    void verificar_contrasenaIncorrecta_devuelveFalse() {
        String hash = PasswordUtil.hashear("correcta");

        assertFalse(PasswordUtil.verificar("incorrecta", hash));
    }

    // =========================================================
    // verificar() — retrocompatibilidad texto plano
    // =========================================================

    @Test
    @DisplayName("verificar() acepta contraseña en texto plano (retrocompatibilidad)")
    void verificar_textoPlano_retrocompatibilidad() {
        String pass = "admin123";

        assertTrue(PasswordUtil.verificar(pass, pass),
            "Contraseña en texto plano debe verificar correctamente");
    }

    @Test
    @DisplayName("verificar() rechaza contraseña texto plano incorrecta")
    void verificar_textoPlanoIncorrecto_devuelveFalse() {
        assertFalse(PasswordUtil.verificar("admin123", "otraContrasena"));
    }

    // =========================================================
    // estaHasheada()
    // =========================================================

    @Test
    @DisplayName("estaHasheada() devuelve true para hash BCrypt")
    void estaHasheada_hashBcrypt_devuelveTrue() {
        String hash = PasswordUtil.hashear("cualquier");
        assertTrue(PasswordUtil.estaHasheada(hash));
    }

    @Test
    @DisplayName("estaHasheada() devuelve false para texto plano")
    void estaHasheada_textoPlano_devuelveFalse() {
        assertFalse(PasswordUtil.estaHasheada("admin123"));
        assertFalse(PasswordUtil.estaHasheada(""));
        assertFalse(PasswordUtil.estaHasheada(null));
    }
}
