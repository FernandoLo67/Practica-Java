package com.hotel.util;

import java.awt.Color;
import java.awt.Font;

/**
 * Clase centralizada del tema visual del sistema.
 *
 * Todos los colores, fuentes y tamaños están aquí.
 * Para cambiar el tema basta modificar esta clase.
 *
 * Uso:
 *   panel.setBackground(Tema.COLOR_PRIMARIO);
 *   label.setFont(Tema.FUENTE_TITULO);
 *
 * @author Fernando
 * @version 1.0
 */
public final class Tema {

    // Constructor privado: no se instancia
    private Tema() {}

    // =========================================================
    // COLORES PRINCIPALES
    // =========================================================

    /** Azul índigo — color principal del sistema */
    public static final Color COLOR_PRIMARIO        = new Color(26, 35, 126);

    /** Azul un tono más claro — hover del sidebar */
    public static final Color COLOR_PRIMARIO_HOVER  = new Color(40, 53, 147);

    /** Dorado — color de acento y destacados */
    public static final Color COLOR_ACENTO          = new Color(255, 160, 0);

    /** Azul muy claro — fondo general de la app */
    public static final Color COLOR_FONDO           = new Color(243, 246, 253);

    /** Azul claro — encabezados de tablas */
    public static final Color COLOR_HEADER_TABLA    = new Color(232, 236, 255);

    /** Blanco puro */
    public static final Color COLOR_BLANCO          = Color.WHITE;

    /** Negro suave — texto principal */
    public static final Color COLOR_TEXTO           = new Color(33, 33, 33);

    /** Gris medio — texto secundario */
    public static final Color COLOR_TEXTO_SECUNDARIO = new Color(90, 95, 120);

    /** Gris muy claro — borde de tarjetas */
    public static final Color COLOR_BORDE           = new Color(215, 220, 240);

    // =========================================================
    // COLORES DE ESTADO
    // =========================================================

    public static final Color COLOR_EXITO           = new Color(46, 125, 50);
    public static final Color COLOR_ERROR           = new Color(198, 40, 40);
    public static final Color COLOR_ADVERTENCIA     = new Color(230, 81, 0);
    public static final Color COLOR_INFO            = new Color(21, 101, 192);
    public static final Color COLOR_GRIS            = new Color(100, 100, 100);

    // =========================================================
    // COLORES DE FILAS DE TABLA
    // =========================================================

    public static final Color COLOR_FILA_PAR        = Color.WHITE;
    public static final Color COLOR_FILA_IMPAR      = new Color(248, 250, 255);
    public static final Color COLOR_FILA_SELECCION  = new Color(197, 210, 255);

    // =========================================================
    // COLORES DE ESTADOS DE ENTIDADES
    // =========================================================

    /** Estados de habitación */
    public static final Color COLOR_DISPONIBLE      = new Color(46, 125, 50);
    public static final Color COLOR_OCUPADA         = new Color(198, 40, 40);
    public static final Color COLOR_RESERVADA       = new Color(230, 81, 0);
    public static final Color COLOR_MANTENIMIENTO   = new Color(100, 100, 100);

    /** Estados de reservación */
    public static final Color COLOR_PENDIENTE       = new Color(230, 81, 0);
    public static final Color COLOR_CONFIRMADA      = new Color(46, 125, 50);
    public static final Color COLOR_CHECKIN         = new Color(21, 101, 192);
    public static final Color COLOR_CHECKOUT        = new Color(100, 100, 100);
    public static final Color COLOR_CANCELADA       = new Color(198, 40, 40);

    // =========================================================
    // FUENTES
    // =========================================================

    public static final Font FUENTE_TITULO          = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FUENTE_SUBTITULO       = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FUENTE_NORMAL          = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_SMALL           = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FUENTE_BOLD            = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FUENTE_CAMPO           = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FUENTE_BOTON           = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_KPI             = new Font("Segoe UI", Font.BOLD, 26);

    // =========================================================
    // UTILIDADES
    // =========================================================

    /**
     * Devuelve el color correspondiente al estado de una habitación.
     */
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

    /**
     * Devuelve el color correspondiente al estado de una reservación o factura.
     */
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
