package com.hotel.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para Validaciones.
 *
 * @author Fernando
 */
@DisplayName("Validaciones — Formularios")
class ValidacionesTest {

    // =========================================================
    // noEstaVacio()
    // =========================================================

    @Nested
    @DisplayName("noEstaVacio()")
    class NoEstaVacio {

        @Test void conValorNormal_devuelveTrue()  { assertTrue(Validaciones.noEstaVacio("Hola")); }
        @Test void conNulo_devuelveFalse()         { assertFalse(Validaciones.noEstaVacio(null)); }
        @Test void conCadenaVacia_devuelveFalse()  { assertFalse(Validaciones.noEstaVacio("")); }
        @Test void conSoloEspacios_devuelveFalse() { assertFalse(Validaciones.noEstaVacio("   ")); }
    }

    // =========================================================
    // validarTexto()
    // =========================================================

    @Nested
    @DisplayName("validarTexto()")
    class ValidarTexto {

        @Test
        @DisplayName("texto válido devuelve null (sin error)")
        void textoValido_devuelveNull() {
            assertNull(Validaciones.validarTexto("nombre", "Fernando", 2, 50));
        }

        @Test
        @DisplayName("texto nulo devuelve mensaje de error")
        void textoNulo_devuelveMensaje() {
            assertNotNull(Validaciones.validarTexto("nombre", null, 2, 50));
        }

        @Test
        @DisplayName("texto demasiado corto devuelve mensaje de error")
        void textoCortito_devuelveMensaje() {
            String error = Validaciones.validarTexto("nombre", "A", 3, 50);
            assertNotNull(error);
            assertTrue(error.contains("3")); // menciona el mínimo
        }

        @Test
        @DisplayName("texto demasiado largo devuelve mensaje de error")
        void textoLargo_devuelveMensaje() {
            String largo = "A".repeat(101);
            assertNotNull(Validaciones.validarTexto("nombre", largo, 1, 100));
        }
    }

    // =========================================================
    // esEmailValido()
    // =========================================================

    @Nested
    @DisplayName("esEmailValido()")
    class Email {

        @ParameterizedTest(name = "email válido: {0}")
        @ValueSource(strings = {"user@gmail.com", "fernando@hotel.gt", "a.b+tag@co.uk"})
        void emailsValidos(String email) {
            assertTrue(Validaciones.esEmailValido(email));
        }

        @ParameterizedTest(name = "email inválido: {0}")
        @ValueSource(strings = {"sinArroba", "sin@dominio", "@sinusuario.com", ""})
        void emailsInvalidos(String email) {
            assertFalse(Validaciones.esEmailValido(email));
        }

        @Test void emailNulo_devuelveFalse() { assertFalse(Validaciones.esEmailValido(null)); }
    }

    // =========================================================
    // esNumeroPositivo() / esPrecioValido()
    // =========================================================

    @Nested
    @DisplayName("Números y precios")
    class Numeros {

        @Test void numeroPositivo_devuelveTrue()      { assertTrue(Validaciones.esNumeroPositivo("5")); }
        @Test void numeroCero_devuelveFalse()          { assertFalse(Validaciones.esNumeroPositivo("0")); }
        @Test void numeroNegativo_devuelveFalse()      { assertFalse(Validaciones.esNumeroPositivo("-3")); }
        @Test void textoNoNumerico_devuelveFalse()     { assertFalse(Validaciones.esNumeroPositivo("abc")); }

        @Test void precioValido_devuelveTrue()         { assertTrue(Validaciones.esPrecioValido("250.50")); }
        @Test void precioCero_devuelveFalse()           { assertFalse(Validaciones.esPrecioValido("0")); }
        @Test void precioTexto_devuelveFalse()          { assertFalse(Validaciones.esPrecioValido("precio")); }
    }

    // =========================================================
    // validarDocumento()
    // =========================================================

    @Nested
    @DisplayName("validarDocumento()")
    class Documento {

        @Test void documentoValido_devuelveNull()   { assertNull(Validaciones.validarDocumento("DPI12345")); }
        @Test void documentoCorto_devuelveMensaje() { assertNotNull(Validaciones.validarDocumento("AB")); }
        @Test void documentoNulo_devuelveMensaje()  { assertNotNull(Validaciones.validarDocumento(null)); }
        @Test void documentoConEspacios_devuelveMensaje() {
            assertNotNull(Validaciones.validarDocumento("1234 5678"));
        }
    }

    // =========================================================
    // validarPassword()
    // =========================================================

    @Nested
    @DisplayName("validarPassword()")
    class Password {

        @Test void passwordCorta_devuelveMensaje()  { assertNotNull(Validaciones.validarPassword("123")); }
        @Test void passwordValida_devuelveNull()    { assertNull(Validaciones.validarPassword("admin123")); }
        @Test void passwordNula_devuelveMensaje()   { assertNotNull(Validaciones.validarPassword(null)); }
    }

    // =========================================================
    // capitalizar() / limpiar()
    // =========================================================

    @Nested
    @DisplayName("Utilidades de texto")
    class Utilidades {

        @Test void capitalizar_minuscula_primeraMayuscula()  { assertEquals("Fernando", Validaciones.capitalizar("fernando")); }
        @Test void capitalizar_yaMayuscula_sinCambio()       { assertEquals("Fernando", Validaciones.capitalizar("FERNANDO")); }
        @Test void capitalizar_nulo_devuelveNulo()           { assertNull(Validaciones.capitalizar(null)); }

        @Test void limpiar_conEspacios_eliminaEspacios()     { assertEquals("texto", Validaciones.limpiar("  texto  ")); }
        @Test void limpiar_nulo_devuelveCadenaVacia()        { assertEquals("", Validaciones.limpiar(null)); }
    }
}
