package com.hotel.util;

import java.util.regex.Pattern;

/**
 * Clase con métodos estáticos para validar datos en los formularios.
 *
 * CÓMO USARLA:
 *   boolean ok = Validaciones.esEmailValido("correo@ejemplo.com");
 *   String msg = Validaciones.validarTexto("nombre", nombreIngresado, 2, 100);
 *
 * @author Fernando
 * @version 1.0
 */
public class Validaciones {

    // Expresión regular para validar emails
    private static final Pattern PATRON_EMAIL =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Expresión regular para teléfonos (permite +, -, espacios y dígitos)
    private static final Pattern PATRON_TELEFONO =
        Pattern.compile("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{3,6}$");

    /** Constructor privado - clase de utilidades, no se instancia */
    private Validaciones() {}

    // =========================================================
    // VALIDACIONES DE TEXTO
    // =========================================================

    /**
     * Verifica que un campo de texto no esté vacío.
     *
     * @param valor El texto a validar
     * @return true si el texto tiene contenido, false si está vacío o es null
     */
    public static boolean noEstaVacio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    /**
     * Valida un campo de texto con longitud mínima y máxima.
     *
     * @param nombreCampo  Nombre del campo (para el mensaje de error)
     * @param valor        Texto ingresado por el usuario
     * @param minimo       Longitud mínima permitida
     * @param maximo       Longitud máxima permitida
     * @return null si es válido, o un mensaje de error si no lo es
     */
    public static String validarTexto(String nombreCampo, String valor, int minimo, int maximo) {
        if (valor == null || valor.trim().isEmpty()) {
            return "El campo '" + nombreCampo + "' es obligatorio.";
        }
        String limpio = valor.trim();
        if (limpio.length() < minimo) {
            return "El campo '" + nombreCampo + "' debe tener al menos " + minimo + " caracteres.";
        }
        if (limpio.length() > maximo) {
            return "El campo '" + nombreCampo + "' no puede tener más de " + maximo + " caracteres.";
        }
        return null; // null significa que es válido
    }

    // =========================================================
    // VALIDACIONES DE EMAIL
    // =========================================================

    /**
     * Verifica que un email tenga formato correcto (ej: user@domain.com).
     *
     * @param email El email a validar
     * @return true si el formato es válido
     */
    public static boolean esEmailValido(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return PATRON_EMAIL.matcher(email.trim()).matches();
    }

    // =========================================================
    // VALIDACIONES DE TELÉFONO
    // =========================================================

    /**
     * Verifica que un número de teléfono tenga formato válido.
     *
     * @param telefono El teléfono a validar
     * @return true si el formato es válido
     */
    public static boolean esTelefonoValido(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return true; // Teléfono es opcional en este sistema
        }
        return PATRON_TELEFONO.matcher(telefono.trim()).matches();
    }

    // =========================================================
    // VALIDACIONES NUMÉRICAS
    // =========================================================

    /**
     * Verifica que un string sea un número entero válido y positivo.
     *
     * @param valor El texto que debería ser un número
     * @return true si es un entero positivo
     */
    public static boolean esNumeroPositivo(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return false;
        }
        try {
            int numero = Integer.parseInt(valor.trim());
            return numero > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Verifica que un string sea un número decimal válido y positivo.
     * Útil para validar precios.
     *
     * @param valor El texto que debería ser un precio
     * @return true si es un decimal positivo
     */
    public static boolean esPrecioValido(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return false;
        }
        try {
            double numero = Double.parseDouble(valor.trim());
            return numero > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // =========================================================
    // VALIDACIONES DE DOCUMENTOS
    // =========================================================

    /**
     * Valida un número de documento (DNI, Cédula, etc.)
     * Solo acepta números y letras, sin espacios.
     *
     * @param documento El número de documento
     * @return null si es válido, mensaje de error si no
     */
    public static String validarDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return "El número de documento es obligatorio.";
        }
        String limpio = documento.trim();
        if (limpio.length() < 5) {
            return "El documento debe tener al menos 5 caracteres.";
        }
        if (limpio.length() > 20) {
            return "El documento no puede tener más de 20 caracteres.";
        }
        if (!limpio.matches("[A-Za-z0-9-]+")) {
            return "El documento solo puede contener letras, números y guiones.";
        }
        return null; // válido
    }

    // =========================================================
    // VALIDACIONES DE CONTRASEÑA
    // =========================================================

    /**
     * Valida que una contraseña cumpla requisitos mínimos de seguridad.
     *
     * @param password La contraseña a validar
     * @return null si es válida, mensaje de error si no
     */
    public static String validarPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "La contraseña es obligatoria.";
        }
        if (password.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres.";
        }
        if (password.length() > 50) {
            return "La contraseña no puede tener más de 50 caracteres.";
        }
        return null; // válida
    }

    // =========================================================
    // UTILIDADES DE LIMPIEZA
    // =========================================================

    /**
     * Limpia un texto eliminando espacios al inicio y al final.
     * Si el valor es null, devuelve cadena vacía.
     *
     * @param valor El texto a limpiar
     * @return El texto sin espacios en los extremos
     */
    public static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /**
     * Capitaliza la primera letra de un texto.
     * Ejemplo: "juan" → "Juan"
     *
     * @param texto El texto a capitalizar
     * @return El texto con la primera letra en mayúscula
     */
    public static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        String limpio = texto.trim().toLowerCase();
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }
}
