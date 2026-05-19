package com.hotel.dao;

import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.TipoHabitacionDAOImpl;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.ConexionDB;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para HabitacionDAO.
 *
 * Requiere BD real. Usa número "TST-99" para identificar habitaciones de prueba.
 * Limpia en @BeforeEach/@AfterEach.
 *
 * @author Fernando
 */
@DisplayName("HabitacionDAO — Integración")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HabitacionDAOIntegrationTest {

    private static HabitacionDAOImpl dao;
    private static TipoHabitacion    tipoPrueba;

    private static final String NUM_PRUEBA = "TST-99";

    @BeforeAll
    static void setup() {
        dao = new HabitacionDAOImpl();
        // Obtener primer tipo disponible para usarlo en pruebas
        TipoHabitacionDAOImpl tipoDAO = new TipoHabitacionDAOImpl();
        List<TipoHabitacion> tipos = tipoDAO.listarTodos();
        assertFalse(tipos.isEmpty(), "Debe haber al menos un TipoHabitacion en BD para correr tests");
        tipoPrueba = tipos.get(0);
    }

    @BeforeEach
    @AfterEach
    void limpiar() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM habitaciones WHERE numero = ?")) {
            ps.setString(1, NUM_PRUEBA);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // =========================================================
    // GUARDAR / BUSCAR
    // =========================================================

    @Test
    @Order(1)
    @DisplayName("guardar() — obtiene ID generado")
    void guardar_obtieneId() {
        Habitacion h = habPrueba();
        assertTrue(dao.guardar(h));
        assertTrue(h.getId() > 0, "ID debe ser asignado por BD");
    }

    @Test
    @Order(2)
    @DisplayName("buscarPorId() — retorna habitación correcta")
    void buscarPorId_correcto() {
        Habitacion h = habPrueba();
        dao.guardar(h);

        Habitacion found = dao.buscarPorId(h.getId());
        assertNotNull(found);
        assertEquals(NUM_PRUEBA, found.getNumero());
        assertEquals(9,          found.getPiso());
        assertNotNull(found.getTipo());
        assertEquals(tipoPrueba.getId(), found.getTipo().getId());
    }

    @Test
    @Order(3)
    @DisplayName("buscarPorId() — ID inexistente retorna null")
    void buscarPorId_inexistente_null() {
        assertNull(dao.buscarPorId(-9999));
    }

    // =========================================================
    // ACTUALIZAR / ESTADO
    // =========================================================

    @Test
    @Order(4)
    @DisplayName("actualizar() — cambios persisten")
    void actualizar_cambiosPersisten() {
        Habitacion h = habPrueba();
        dao.guardar(h);

        h.setDescripcion("Descripcion modificada test");
        h.setPrecioEspecial(750.0);
        assertTrue(dao.actualizar(h));

        Habitacion r = dao.buscarPorId(h.getId());
        assertEquals("Descripcion modificada test", r.getDescripcion());
        assertEquals(750.0, r.getPrecioEspecial(), 0.01);
    }

    @Test
    @Order(5)
    @DisplayName("cambiarEstado() — estado actualizado")
    void cambiarEstado_actualizado() {
        Habitacion h = habPrueba();
        dao.guardar(h);

        assertTrue(dao.cambiarEstado(h.getId(), Habitacion.ESTADO_MANTENIMIENTO));

        Habitacion r = dao.buscarPorId(h.getId());
        assertEquals(Habitacion.ESTADO_MANTENIMIENTO, r.getEstado());
    }

    // =========================================================
    // PRECIO EFECTIVO
    // =========================================================

    @Test
    @Order(6)
    @DisplayName("getPrecioNoche() — usa precio especial si está seteado")
    void getPrecioNoche_usaPrecioEspecial() {
        Habitacion h = habPrueba();
        h.setPrecioEspecial(999.99);
        dao.guardar(h);

        Habitacion r = dao.buscarPorId(h.getId());
        assertEquals(999.99, r.getPrecioNoche(), 0.01);
    }

    @Test
    @Order(7)
    @DisplayName("getPrecioNoche() — usa precio base si especial es null")
    void getPrecioNoche_usaPrecioBase() {
        Habitacion h = habPrueba(); // sin precio especial
        dao.guardar(h);

        Habitacion r = dao.buscarPorId(h.getId());
        assertNull(r.getPrecioEspecial());
        assertEquals(tipoPrueba.getPrecioBase(), r.getPrecioNoche(), 0.01);
    }

    // =========================================================
    // DISPONIBILIDAD EN RANGO
    // =========================================================

    @Test
    @Order(8)
    @DisplayName("listarDisponiblesEnRango() — retorna lista no nula")
    void disponiblesEnRango_noNula() {
        Date ci = Date.valueOf(LocalDate.now().plusDays(30));
        Date co = Date.valueOf(LocalDate.now().plusDays(33));
        List<Habitacion> lista = dao.listarDisponiblesEnRango(ci, co);
        assertNotNull(lista);
    }

    @Test
    @Order(9)
    @DisplayName("listarDisponiblesEnRango() — habitación MANTENIMIENTO excluida")
    void disponiblesEnRango_excluyeMantenimiento() {
        Habitacion h = habPrueba();
        h.setEstado(Habitacion.ESTADO_MANTENIMIENTO);
        dao.guardar(h);

        Date ci = Date.valueOf(LocalDate.now().plusDays(30));
        Date co = Date.valueOf(LocalDate.now().plusDays(33));
        List<Habitacion> lista = dao.listarDisponiblesEnRango(ci, co);

        boolean aparece = lista.stream()
            .anyMatch(hab -> NUM_PRUEBA.equals(hab.getNumero()));
        assertFalse(aparece, "Habitación en MANTENIMIENTO no debe aparecer en disponibles");
    }

    // =========================================================
    // CONTEO
    // =========================================================

    @Test
    @Order(10)
    @DisplayName("contarTodas() / contarPorEstado() — coherentes")
    void conteo_coherente() {
        int total = dao.contarTodas();
        assertTrue(total >= 0);

        int disponibles  = dao.contarPorEstado(Habitacion.ESTADO_DISPONIBLE);
        int ocupadas     = dao.contarPorEstado(Habitacion.ESTADO_OCUPADA);
        int reservadas   = dao.contarPorEstado(Habitacion.ESTADO_RESERVADA);
        int mantenimiento = dao.contarPorEstado(Habitacion.ESTADO_MANTENIMIENTO);

        // La suma de estados conocidos no debe superar el total
        assertTrue(disponibles + ocupadas + reservadas + mantenimiento <= total + 1,
            "Suma de estados no debe superar total");
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static Habitacion habPrueba() {
        Habitacion h = new Habitacion();
        h.setNumero(NUM_PRUEBA);
        h.setPiso(9);
        h.setTipo(tipoPrueba);
        h.setEstado(Habitacion.ESTADO_DISPONIBLE);
        h.setDescripcion("Habitacion de prueba — no borrar si falla test");
        return h;
    }
}
