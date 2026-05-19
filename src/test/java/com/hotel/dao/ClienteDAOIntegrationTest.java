package com.hotel.dao;

import com.hotel.dao.impl.ClienteDAOImpl;
import com.hotel.modelo.Cliente;
import com.hotel.util.ConexionDB;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para ClienteDAO.
 *
 * REQUIERE BD REAL: database.properties configurado y MySQL corriendo.
 * Limpia los datos de prueba en @AfterEach para no contaminar la BD.
 *
 * Para ejecutar solo estos tests desde NetBeans:
 *   clic derecho → Test File
 *
 * @author Fernando
 */
@DisplayName("ClienteDAO — Integración")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClienteDAOIntegrationTest {

    private static ClienteDAOImpl dao;
    private static final String DOC_PRUEBA = "TEST-99999999";

    @BeforeAll
    static void iniciarDAO() {
        dao = new ClienteDAOImpl();
    }

    /** Limpia cualquier cliente de prueba antes y después de cada test */
    @BeforeEach
    @AfterEach
    void limpiarDatosPrueba() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM clientes WHERE documento = ?")) {
            ps.setString(1, DOC_PRUEBA);
            ps.executeUpdate();
        } catch (Exception e) {
            // Si la BD no está disponible, los tests fallarán con el mensaje correcto
        }
    }

    // =========================================================
    // GUARDAR
    // =========================================================

    @Test
    @Order(1)
    @DisplayName("guardar() — cliente nuevo obtiene ID generado")
    void guardar_clienteNuevo_obtieneId() {
        Cliente c = clientePrueba();
        boolean ok = dao.guardar(c);

        assertTrue(ok, "guardar() debe retornar true");
        assertTrue(c.getId() > 0, "ID debe ser asignado por la BD");
    }

    @Test
    @Order(2)
    @DisplayName("guardar() — documento duplicado retorna false")
    void guardar_documentoDuplicado_retornaFalse() {
        dao.guardar(clientePrueba()); // primer insert
        Cliente duplicado = clientePrueba();
        boolean ok = dao.guardar(duplicado);

        assertFalse(ok, "Insertar documento duplicado debe fallar");
    }

    // =========================================================
    // BUSCAR
    // =========================================================

    @Test
    @Order(3)
    @DisplayName("buscarPorId() — retorna cliente correcto")
    void buscarPorId_retornaClienteCorrecto() {
        Cliente c = clientePrueba();
        dao.guardar(c);

        Cliente encontrado = dao.buscarPorId(c.getId());

        assertNotNull(encontrado, "Debe encontrar el cliente");
        assertEquals("TEST_NOMBRE",   encontrado.getNombre());
        assertEquals("TEST_APELLIDO", encontrado.getApellido());
        assertEquals(DOC_PRUEBA,      encontrado.getDocumento());
    }

    @Test
    @Order(4)
    @DisplayName("buscarPorId() — ID inexistente retorna null")
    void buscarPorId_idInexistente_retornaNull() {
        Cliente c = dao.buscarPorId(-9999);
        assertNull(c, "ID inexistente debe retornar null");
    }

    @Test
    @Order(5)
    @DisplayName("buscar() — texto en nombre retorna resultados")
    void buscar_textoParcial_retornaResultados() {
        dao.guardar(clientePrueba());

        List<Cliente> resultado = dao.buscar("TEST_NOMBRE");

        assertFalse(resultado.isEmpty(), "Debe encontrar al menos el cliente de prueba");
        assertTrue(resultado.stream()
            .anyMatch(c -> DOC_PRUEBA.equals(c.getDocumento())));
    }

    @Test
    @Order(6)
    @DisplayName("buscar() — texto sin coincidencia retorna lista vacía")
    void buscar_sinCoincidencia_listaVacia() {
        List<Cliente> resultado = dao.buscar("XXXXXXXXXXX_NO_EXISTE_XXXXXXXXXXX");
        assertTrue(resultado.isEmpty(), "Sin coincidencia debe retornar lista vacía");
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @Test
    @Order(7)
    @DisplayName("actualizar() — cambios persisten en BD")
    void actualizar_cambiosPersisten() {
        Cliente c = clientePrueba();
        dao.guardar(c);

        c.setNombre("NOMBRE_MODIFICADO");
        c.setTelefono("55551234");
        boolean ok = dao.actualizar(c);

        assertTrue(ok, "actualizar() debe retornar true");

        Cliente recargado = dao.buscarPorId(c.getId());
        assertEquals("NOMBRE_MODIFICADO", recargado.getNombre());
        assertEquals("55551234",          recargado.getTelefono());
    }

    // =========================================================
    // LISTAR
    // =========================================================

    @Test
    @Order(8)
    @DisplayName("listarTodos() — lista no nula")
    void listarTodos_noNula() {
        List<Cliente> todos = dao.listarTodos();
        assertNotNull(todos, "listarTodos() nunca debe retornar null");
    }

    @Test
    @Order(9)
    @DisplayName("contarTodos() — incrementa al guardar")
    void contarTodos_incrementaAlGuardar() {
        int antes = dao.contarTodos();
        dao.guardar(clientePrueba());
        int despues = dao.contarTodos();

        assertEquals(antes + 1, despues, "Contador debe incrementar en 1");
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static Cliente clientePrueba() {
        Cliente c = new Cliente();
        c.setNombre("TEST_NOMBRE");
        c.setApellido("TEST_APELLIDO");
        c.setTipoDocumento("DPI");
        c.setDocumento(DOC_PRUEBA);
        c.setTelefono("12345678");
        c.setEmail("test@hotel.local");
        return c;
    }
}
