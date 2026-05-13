package com.hotel.modelo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el modelo Reservacion.
 *
 * Verifica el cálculo de noches y totales sin necesitar base de datos.
 *
 * @author Fernando
 */
@DisplayName("Reservacion — Cálculo de noches y totales")
class ReservacionTest {

    private Reservacion reservacion;
    private Habitacion  habitacion;
    private TipoHabitacion tipo;

    @BeforeEach
    void setUp() {
        tipo = new TipoHabitacion();
        tipo.setNombre("Doble");
        tipo.setPrecioBase(500.00); // Q500 por noche

        habitacion = new Habitacion();
        habitacion.setNumero("201");
        habitacion.setPiso(2);
        habitacion.setTipo(tipo);
        habitacion.setEstado("DISPONIBLE");

        reservacion = new Reservacion();
        reservacion.setHabitacion(habitacion);
    }

    // =========================================================
    // getNoches()
    // =========================================================

    @Test
    @DisplayName("getNoches() calcula correctamente 3 noches")
    void getNoches_tresNoches() {
        reservacion.setFechaCheckin(Date.valueOf(LocalDate.of(2024, 6, 1)));
        reservacion.setFechaCheckout(Date.valueOf(LocalDate.of(2024, 6, 4)));

        assertEquals(3, reservacion.getNoches());
    }

    @Test
    @DisplayName("getNoches() calcula correctamente 1 noche")
    void getNoches_unaNoche() {
        reservacion.setFechaCheckin(Date.valueOf(LocalDate.of(2024, 1, 15)));
        reservacion.setFechaCheckout(Date.valueOf(LocalDate.of(2024, 1, 16)));

        assertEquals(1, reservacion.getNoches());
    }

    @Test
    @DisplayName("getNoches() devuelve 0 si las fechas son null")
    void getNoches_fechasNulas_devuelveCero() {
        reservacion.setFechaCheckin(null);
        reservacion.setFechaCheckout(null);

        assertEquals(0, reservacion.getNoches());
    }

    @Test
    @DisplayName("getNoches() calcula correctamente cruzando mes")
    void getNoches_cruzaMes() {
        reservacion.setFechaCheckin(Date.valueOf(LocalDate.of(2024, 1, 29)));
        reservacion.setFechaCheckout(Date.valueOf(LocalDate.of(2024, 2, 2)));

        assertEquals(4, reservacion.getNoches());
    }

    // =========================================================
    // getTotalSinImpuesto()
    // =========================================================

    @Test
    @DisplayName("getTotalSinImpuesto() es noches × precio")
    void getTotalSinImpuesto_calculaCorrecto() {
        reservacion.setFechaCheckin(Date.valueOf(LocalDate.of(2024, 6, 1)));
        reservacion.setFechaCheckout(Date.valueOf(LocalDate.of(2024, 6, 4))); // 3 noches

        // 3 noches × Q500 = Q1500
        assertEquals(1500.00, reservacion.getTotalSinImpuesto(), 0.001);
    }

    @Test
    @DisplayName("getTotalSinImpuesto() devuelve 0 si habitacion es null")
    void getTotalSinImpuesto_habitacionNula_devuelveCero() {
        reservacion.setHabitacion(null);
        reservacion.setFechaCheckin(Date.valueOf(LocalDate.of(2024, 6, 1)));
        reservacion.setFechaCheckout(Date.valueOf(LocalDate.of(2024, 6, 4)));

        assertEquals(0.0, reservacion.getTotalSinImpuesto(), 0.001);
    }

    // =========================================================
    // getTotalConImpuesto()
    // =========================================================

    @Test
    @DisplayName("getTotalConImpuesto() aplica 18% correctamente")
    void getTotalConImpuesto_aplica18Porciento() {
        reservacion.setFechaCheckin(Date.valueOf(LocalDate.of(2024, 6, 1)));
        reservacion.setFechaCheckout(Date.valueOf(LocalDate.of(2024, 6, 3))); // 2 noches

        // 2 × Q500 = Q1000 + 18% = Q1180
        assertEquals(1180.00, reservacion.getTotalConImpuesto(), 0.001);
    }

    // =========================================================
    // Estados
    // =========================================================

    @Test
    @DisplayName("Estado inicial al construir con constructor completo es PENDIENTE")
    void constructor_estadoInicial_esPendiente() {
        Cliente c = new Cliente();
        Usuario u = new Usuario();
        Date ci = Date.valueOf(LocalDate.now());
        Date co = Date.valueOf(LocalDate.now().plusDays(2));

        Reservacion r = new Reservacion(c, habitacion, ci, co, u);
        assertEquals(Reservacion.ESTADO_PENDIENTE, r.getEstado());
    }

    @Test
    @DisplayName("Las constantes de estado tienen los valores esperados")
    void constantes_estadosTienenValoresCorrectos() {
        assertEquals("PENDIENTE",  Reservacion.ESTADO_PENDIENTE);
        assertEquals("CONFIRMADA", Reservacion.ESTADO_CONFIRMADA);
        assertEquals("CHECKIN",    Reservacion.ESTADO_CHECKIN);
        assertEquals("CHECKOUT",   Reservacion.ESTADO_CHECKOUT);
        assertEquals("CANCELADA",  Reservacion.ESTADO_CANCELADA);
    }
}
