package com.hotel.util;

import java.awt.Color;
import java.awt.Font;

/**
 * Clase centralizada del tema visual del sistema.
 *
 * Soporta modo claro y modo oscuro.
 * Llama a {@link #setModoOscuro(boolean)} antes de construir los paneles,
 * luego todos los {@code Tema.COLOR_*} devolverán los colores correctos.
 *
 * Uso:
 *   panel.setBackground(Tema.COLOR_PRIMARIO);
 *   label.setFont(Tema.FUENTE_TITULO);
 *
 * @author Fernando
 * @version 2.0
 */
public final class Tema {

    private Tema() {}

    // =========================================================
    // MODO (oscuro / claro)
    // =========================================================

    private static boolean modoOscuro = false;

    public static boolean isModoOscuro()      { return modoOscuro; }

    /**
     * Cambia el modo del tema y recalcula TODOS los colores.
     * Después de llamarlo, los paneles nuevos usarán el nuevo tema.
     */
    public static void setModoOscuro(boolean oscuro) {
        modoOscuro = oscuro;
        aplicarColores();
    }

    /** Alterna entre claro y oscuro. */
    public static void toggleModo() { setModoOscuro(!modoOscuro); }

    // =========================================================
    // COLORES (dinámicos — se reasignan al cambiar de modo)
    // =========================================================

    public static Color COLOR_PRIMARIO;
    public static Color COLOR_PRIMARIO_HOVER;
    public static Color COLOR_ACENTO;
    public static Color COLOR_FONDO;
    public static Color COLOR_HEADER_TABLA;
    public static Color COLOR_BLANCO;
    public static Color COLOR_TEXTO;
    public static Color COLOR_TEXTO_SECUNDARIO;
    public static Color COLOR_BORDE;

    // Estados
    public static Color COLOR_EXITO;
    public static Color COLOR_ERROR;
    public static Color COLOR_ADVERTENCIA;
    public static Color COLOR_INFO;
    public static Color COLOR_GRIS;

    // Filas de tabla
    public static Color COLOR_FILA_PAR;
    public static Color COLOR_FILA_IMPAR;
    public static Color COLOR_FILA_SELECCION;

    // Estados de habitación
    public static Color COLOR_DISPONIBLE;
    public static Color COLOR_OCUPADA;
    public static Color COLOR_RESERVADA;
    public static Color COLOR_MANTENIMIENTO;

    // Estados de reservación
    public static Color COLOR_PENDIENTE;
    public static Color COLOR_CONFIRMADA;
    public static Color COLOR_CHECKIN;
    public static Color COLOR_CHECKOUT;
    public static Color COLOR_CANCELADA;

    // =========================================================
    // FUENTES (no cambian con el modo)
    // =========================================================

    public static final Font FUENTE_TITULO    = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FUENTE_SUBTITULO = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FUENTE_NORMAL    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_SMALL     = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FUENTE_BOLD      = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FUENTE_CAMPO     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FUENTE_BOTON     = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FUENTE_KPI       = new Font("Segoe UI", Font.BOLD,  26);

    // =========================================================
    // INICIALIZACIÓN (colores claros por defecto al cargar la clase)
    // =========================================================

    static { aplicarColores(); }

    private static void aplicarColores() {
        if (modoOscuro) aplicarOscuro();
        else            aplicarClaro();
    }

    private static void aplicarClaro() {
        COLOR_PRIMARIO          = new Color(26, 35, 126);
        COLOR_PRIMARIO_HOVER    = new Color(40, 53, 147);
        COLOR_ACENTO            = new Color(255, 160, 0);
        COLOR_FONDO             = new Color(243, 246, 253);
        COLOR_HEADER_TABLA      = new Color(232, 236, 255);
        COLOR_BLANCO            = Color.WHITE;
        COLOR_TEXTO             = new Color(33, 33, 33);
        COLOR_TEXTO_SECUNDARIO  = new Color(90, 95, 120);
        COLOR_BORDE             = new Color(215, 220, 240);

        COLOR_EXITO             = new Color(46, 125, 50);
        COLOR_ERROR             = new Color(198, 40, 40);
        COLOR_ADVERTENCIA       = new Color(230, 81, 0);
        COLOR_INFO              = new Color(21, 101, 192);
        COLOR_GRIS              = new Color(100, 100, 100);

        COLOR_FILA_PAR          = Color.WHITE;
        COLOR_FILA_IMPAR        = new Color(248, 250, 255);
        COLOR_FILA_SELECCION    = new Color(197, 210, 255);

        COLOR_DISPONIBLE        = new Color(46, 125, 50);
        COLOR_OCUPADA           = new Color(198, 40, 40);
        COLOR_RESERVADA         = new Color(230, 81, 0);
        COLOR_MANTENIMIENTO     = new Color(100, 100, 100);

        COLOR_PENDIENTE         = new Color(230, 81, 0);
        COLOR_CONFIRMADA        = new Color(46, 125, 50);
        COLOR_CHECKIN           = new Color(21, 101, 192);
        COLOR_CHECKOUT          = new Color(100, 100, 100);
        COLOR_CANCELADA         = new Color(198, 40, 40);
    }

    private static void aplicarOscuro() {
        COLOR_PRIMARIO          = new Color(92, 107, 192);   // indigo claro para contraste
        COLOR_PRIMARIO_HOVER    = new Color(121, 134, 203);
        COLOR_ACENTO            = new Color(255, 180, 30);   // dorado más brillante
        COLOR_FONDO             = new Color(15, 16, 35);     // fondo casi negro-azul
        COLOR_HEADER_TABLA      = new Color(30, 33, 64);     // encabezado tabla oscuro
        COLOR_BLANCO            = new Color(26, 29, 53);     // "blanco" oscuro para tarjetas
        COLOR_TEXTO             = new Color(220, 224, 245);  // texto claro
        COLOR_TEXTO_SECUNDARIO  = new Color(140, 148, 190);  // texto secundario
        COLOR_BORDE             = new Color(45, 50, 82);     // borde oscuro

        COLOR_EXITO             = new Color(76, 175, 80);    // verde más brillante
        COLOR_ERROR             = new Color(239, 83, 80);    // rojo más brillante
        COLOR_ADVERTENCIA       = new Color(255, 152, 0);    // naranja más brillante
        COLOR_INFO              = new Color(66, 165, 245);   // azul más brillante
        COLOR_GRIS              = new Color(158, 158, 158);

        COLOR_FILA_PAR          = new Color(26, 29, 53);     // fila par oscura
        COLOR_FILA_IMPAR        = new Color(30, 33, 64);     // fila impar oscura
        COLOR_FILA_SELECCION    = new Color(55, 71, 140);    // selección azul oscuro

        COLOR_DISPONIBLE        = new Color(76, 175, 80);
        COLOR_OCUPADA           = new Color(239, 83, 80);
        COLOR_RESERVADA         = new Color(255, 152, 0);
        COLOR_MANTENIMIENTO     = new Color(158, 158, 158);

        COLOR_PENDIENTE         = new Color(255, 152, 0);
        COLOR_CONFIRMADA        = new Color(76, 175, 80);
        COLOR_CHECKIN           = new Color(66, 165, 245);
        COLOR_CHECKOUT          = new Color(158, 158, 158);
        COLOR_CANCELADA         = new Color(239, 83, 80);
    }

    // =========================================================
    // UTILIDADES
    // =========================================================

    public static Color colorEstadoHabitacion(String estado) {
        if (estado == null) return COLOR_TEXTO;
        switch (estado) {
            case "DISPONIBLE":    return COLOR_DISPONIBLE;
            case "OCUPADA":       return COLOR_OCUPADA;
            case "RESERVADA":     return COLOR_RESERVADA;
            case "MANTENIMIENTO": return COLOR_MANTENIMIENTO;
            default:              return COLOR_TEXTO;
        }
    }

    public static Color colorEstado(String estado) {
        if (estado == null) return COLOR_TEXTO;
        switch (estado) {
            case "PENDIENTE":  return COLOR_PENDIENTE;
            case "CONFIRMADA": return COLOR_CONFIRMADA;
            case "CHECKIN":    return COLOR_CHECKIN;
            case "CHECKOUT":   return COLOR_CHECKOUT;
            case "CANCELADA":  return COLOR_CANCELADA;
            case "PAGADA":     return COLOR_EXITO;
            case "ANULADA":    return COLOR_ERROR;
            default:           return COLOR_TEXTO;
        }
    }
}
