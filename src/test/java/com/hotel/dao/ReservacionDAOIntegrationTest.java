package com.hotel.dao;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.dao.impl.HabitacionDAOImpl;
import com.hotel.dao.impl.ReservacionDAOImpl;
import com.hotel.dao.impl.TipoHabitacionDAOImpl;
import com.hotel.dao.impl.UsuarioDAOImpl;
import com.hotel.modelo.*;
import com.hotel.util.ConexionDB;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para ReservacionDAO.
 *
 * Crea fixtures (cliente, habitación, usuario) al inicio y los limpia al final.
 * Cada test crea y elimina sus propias reservaciones.
 *
 * @author Fernando
 */
@DisplayName("ReservacionDAO — Integración")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservacionDAOIntegrationTest {

    // DAOs
    private static ReservacionDAOImpl reservacionDAO;
    private static ClienteDAOImpl     clienteDAO;
    private static HabitacionDAOImpl  habitacionDAO;

    // Fixtures reutilizados
    private static Cliente    clienteTest;
    private static Habitacion habitacionTest;
    private static Usuario    usuarioTest;

    private static final String DOC_CLI  = "RES-TEST-CLI-001";
    private static final String NUM_HAB  = "TST-RES-01";

    @BeforeAll
    static void crearFixtures() {
        reservacionDAO = new ReservacionDAOImpl();
        clienteDAO     = new ClienteDAOImpl();
        habitacionDAO  = new HabitacionDAOImpl();

        // Cliente fixture
        clienteTest = new Cliente();
        clienteTest.setNombre("ResTest");
        clienteTest.setApellido("Cliente");
        clienteTest.setTipoDocumento("DPI");
        clienteTest.setDocumento(DOC_CLI);
        clienteTest.setTelefono("00000000");
        clienteDAO.guardar(clienteTest);
        assertTrue(clienteTest.getId() > 0, "Fixture cliente no creado");

        // Habitación fixture
        TipoHabitacionDAOImpl tipoDAO = new TipoHabitacionDAOImpl();
        List<TipoHabitacion> tipos = tipoDAO.listarTodos();
        assertFalse(tipos.isEmpty(), "Necesita TipoHabitacion en BD");

        habitacionTest = new Habitacion();
        habitacionTest.setNumero(NUM_HAB);
        habitacionTest.setPiso(9);
        habitacionTest.setTipo(tipos.get(0));
        habitacionTest.setEstado(Habitacion.ESTADO_DISPONIBLE);
        habitacionDAO.guardar(habitacionTest);
        assertTrue(habitacionTest.getId() > 0, "Fixture habitación no creado");

        // Usuario: usar el primero disponible
        UsuarioDAOImpl usuDAO = new UsuarioDAOImpl();
        List<Usuario> usuarios = usuDAO.listarTodos();
        assertFalse(usuarios.isEmpty(), "Necesita al menos un Usuario en BD");
        usuarioTest = usuarios.get(0);
    }

    @AfterAll
    static void limpiarFixtures() {
        // Limpiar reservaciones de prueba primero (FK)
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM reservaciones WHERE id_habitacion = ?")) {
            ps.setInt(1, habitacionTest.getId());
            ps.executeUpdate();
        } catch (Exception ignored) {}

        // Limpiar habitación y cliente
        try (Connection conn = ConexionDB.getConexion()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM habitaciones WHERE numero = ?")) {
                ps.setString(1, NUM_HAB);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM clientes WHERE documento = ?")) {
                ps.setString(1, DOC_CLI);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    @AfterEach
    void limpiarReservaciones() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM reservaciones WHERE id_habitacion = ?")) {
            ps.setInt(1, habitacionTest.getId());
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // =========================================================
    // GUARDAR
    // =========================================================

    @Test
    @Order(1)
    @DisplayName("guardar() — obtiene ID generado")
    void guardar_obtieneId() {
        Reservacion r = reservacionPrueba(futura(1), futura(3));
        assertTrue(reservacionDAO.guardar(r));
        assertTrue(r.getId() > 0, "ID debe ser asignado por BD");
    }

    // =========================================================
    // BUSCAR
    // =========================================================

    @Test
    @Order(2)
    @DisplayName("buscarPorId() — retorna reservación correcta")
    void buscarPorId_correcto() {
        Reservacion r = reservacionPrueba(futura(5), futura(8));
        reservacionDAO.guardar(r);

        Reservacion found = reservacionDAO.buscarPorId(r.getId());
        assertNotNull(found);
        assertEquals(clienteTest.getId(),    found.getCliente().getId());
        assertEquals(habitacionTest.getId(), found.getHabitacion().getId());
        assertEquals(Reservacion.ESTADO_PENDIENTE, found.getEstado());
    }

    @Test
    @Order(3)
    @DisplayName("buscarPorId() — ID inexistente retorna null")
    void buscarPorId_inexistente_null() {
        assertNull(reservacionDAO.buscarPorId(-9999));
    }

    // =========================================================
    // DISPONIBILIDAD — núcleo del negocio
    // =========================================================

    @Test
    @Order(4)
    @DisplayName("habitacionDisponible() — libre en rango sin reservas")
    void disponible_sinReservas_libre() {
        Date ci = sqlDate(futura(20));
        Date co = sqlDate(futura(23));
        assertTrue(reservacionDAO.habitacionDisponible(
            habitacionTest.getId(), ci, co, 0),
            "Habitación sin reservas debe estar disponible");
    }

    @Test
    @Order(5)
    @DisplayName("habitacionDisponible() — solapamiento total = no disponible")
    void disponible_solapamientoTotal_noDisponible() {
        // Reservar del día 10 al 15
        Reservacion r = reservacionPrueba(futura(10), futura(15));
        reservacionDAO.guardar(r);

        // Intentar reservar del 11 al 14 (dentro del período)
        Date ci = sqlDate(futura(11));
        Date co = sqlDate(futura(14));
        assertFalse(reservacionDAO.habitacionDisponible(
            habitacionTest.getId(), ci, co, 0),
            "Solapamiento total debe bloquear disponibilidad");
    }

    @Test
    @Order(6)
    @DisplayName("habitacionDisponible() — solapamiento parcial inicio = no disponible")
    void disponible_solapamientoParcialInicio_noDisponible() {
        // Reservar del 10 al 15
        reservacionDAO.guardar(reservacionPrueba(futura(10), futura(15)));

        // Intentar del 8 al 12 (se superpone al inicio)
        assertFalse(reservacionDAO.habitacionDisponible(
            habitacionTest.getId(), sqlDate(futura(8)), sqlDate(futura(12)), 0));
    }

    @Test
    @Order(7)
    @DisplayName("habitacionDisponible() — rango adyacente (no se superpone) = disponible")
    void disponible_rangoAdyacente_disponible() {
        // Reservar del 10 al 15
        reservacionDAO.guardar(reservacionPrueba(futura(10), futura(15)));

        // Intentar del 15 al 18 (checkin = checkout anterior → NO se superpone)
        assertTrue(reservacionDAO.habitacionDisponible(
            habitacionTest.getId(), sqlDate(futura(15)), sqlDate(futura(18)), 0),
            "Reserva que empieza en checkout de otra no debe solaparse");
    }

    @Test
    @Order(8)
    @DisplayName("habitacionDisponible() — excluir propia reservación al editar")
    void disponible_excluirPropia_disponible() {
        Reservacion r = reservacionPrueba(futura(20), futura(25));
        reservacionDAO.guardar(r);

        // Al editar la propia reservación, debe considerarse disponible
        assertTrue(reservacionDAO.habitacionDisponible(
            habitacionTest.getId(),
            sqlDate(futura(20)), sqlDate(futura(25)),
            r.getId()),   // ← excluir esta reservación
            "Al editar reservación propia debe estar disponible");
    }

    @Test
    @Order(9)
    @DisplayName("habitacionDisponible() — CANCELADA no bloquea disponibilidad")
    void disponible_canceladaNoBloquea() {
        Reservacion r = reservacionPrueba(futura(30), futura(33));
        reservacionDAO.guardar(r);
        reservacionDAO.cambiarEstado(r.getId(), Reservacion.ESTADO_CANCELADA);

        // Mismo rango → debe estar disponible porque está CANCELADA
        assertTrue(reservacionDAO.habitacionDisponible(
            habitacionTest.getId(), sqlDate(futura(30)), sqlDate(futura(33)), 0),
            "Reservación CANCELADA no debe bloquear disponibilidad");
    }

    // =========================================================
    // ESTADO
    // =========================================================

    @Test
    @Order(10)
    @DisplayName("cambiarEstado() — estado persiste")
    void cambiarEstado_persiste() {
        Reservacion r = reservacionPrueba(futura(40), futura(43));
        reservacionDAO.guardar(r);

        assertTrue(reservacionDAO.cambiarEstado(r.getId(), Reservacion.ESTADO_CONFIRMADA));

        Reservacion reloaded = reservacionDAO.buscarPorId(r.getId());
        assertEquals(Reservacion.ESTADO_CONFIRMADA, reloaded.getEstado());
    }

    @Test
    @Order(11)
    @DisplayName("contarPorEstado() — PENDIENTE incrementa al guardar")
    void contarPorEstado_incrementa() {
        int antes = reservacionDAO.contarPorEstado(Reservacion.ESTADO_PENDIENTE);
        reservacionDAO.guardar(reservacionPrueba(futura(50), futura(52)));
        int despues = reservacionDAO.contarPorEstado(Reservacion.ESTADO_PENDIENTE);
        assertEquals(antes + 1, despues);
    }

    // =========================================================
    // LISTAR EN RANGO
    // =========================================================

    @Test
    @Order(12)
    @DisplayName("listarEnRango() — incluye reservación solapada")
    void listarEnRango_incluyeSolapada() {
        Reservacion r = reservacionPrueba(futura(60), futura(63));
        reservacionDAO.guardar(r);

        List<Reservacion> lista = reservacionDAO.listarEnRango(
            sqlDate(futura(59)), sqlDate(futura(65)));

        assertTrue(lista.stream().anyMatch(res -> res.getId() == r.getId()),
            "Reservación solapada debe aparecer en listarEnRango");
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Reservacion reservacionPrueba(LocalDate ci, LocalDate co) {
        Reservacion r = new Reservacion();
        r.setCliente(clienteTest);
        r.setHabitacion(habitacionTest);
        r.setFechaCheckin(sqlDate(ci));
        r.setFechaCheckout(sqlDate(co));
        r.setEstado(Reservacion.ESTADO_PENDIENTE);
        r.setUsuarioRegistro(usuarioTest);
        return r;
    }

    private static LocalDate futura(int diasDesdeHoy) {
        return LocalDate.now().plusDays(diasDesdeHoy);
    }

    private static Date sqlDate(LocalDate ld) {
        return Date.valueOf(ld);
    }
}
