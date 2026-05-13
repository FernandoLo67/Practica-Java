package com.hotel.dao.impl;

import com.hotel.dao.ClienteDAO;
import com.hotel.modelo.Cliente;
import com.hotel.util.ConexionDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ClienteDAO usando JDBC para MySQL.
 *
 * Contiene todas las consultas SQL para gestionar clientes:
 * listar, buscar, guardar, actualizar y eliminar.
 *
 * @author Fernando
 * @version 1.0
 */
public class ClienteDAOImpl implements ClienteDAO {

    private static final Logger log = LoggerFactory.getLogger(ClienteDAOImpl.class);

    // =========================================================
    // CONSULTAS SQL
    // =========================================================

    private static final String SQL_LISTAR =
        "SELECT * FROM clientes ORDER BY apellido, nombre";

    private static final String SQL_BUSCAR =
        "SELECT * FROM clientes " +
        "WHERE nombre LIKE ? OR apellido LIKE ? OR documento LIKE ? OR email LIKE ? " +
        "ORDER BY apellido, nombre";

    private static final String SQL_BUSCAR_POR_ID =
        "SELECT * FROM clientes WHERE id = ?";

    private static final String SQL_BUSCAR_POR_DOCUMENTO =
        "SELECT * FROM clientes WHERE documento = ?";

    private static final String SQL_GUARDAR =
        "INSERT INTO clientes (nombre, apellido, tipo_documento, documento, " +
        "telefono, email, direccion, nacionalidad, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
        "UPDATE clientes SET nombre = ?, apellido = ?, tipo_documento = ?, " +
        "documento = ?, telefono = ?, email = ?, direccion = ?, nacionalidad = ?, activo = ? " +
        "WHERE id = ?";

    private static final String SQL_ELIMINAR =
        "DELETE FROM clientes WHERE id = ?";

    private static final String SQL_CONTAR =
        "SELECT COUNT(*) FROM clientes";

    private static final String SQL_CONTAR_ACTIVOS =
        "SELECT COUNT(*) FROM clientes WHERE activo = TRUE";

    private static final String SQL_CONTAR_NUEVOS_MES =
        "SELECT COUNT(*) FROM clientes " +
        "WHERE MONTH(fecha_registro) = MONTH(CURDATE()) " +
        "AND YEAR(fecha_registro) = YEAR(CURDATE())";

    private static final String SQL_CAMBIAR_ACTIVO =
        "UPDATE clientes SET activo = ? WHERE id = ?";

    // =========================================================
    // IMPLEMENTACIÓN
    // =========================================================

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearCliente(rs));
            }
        } catch (SQLException e) {
            log.error("Error en listarTodos()", e);
        }
        return lista;
    }

    @Override
    public List<Cliente> buscar(String texto) {
        List<Cliente> lista = new ArrayList<>();
        // El % permite buscar en cualquier posición del texto
        String patron = "%" + texto + "%";

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {

            // Se pasa el mismo patrón a los 4 campos de búsqueda
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ps.setString(4, patron);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCliente(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error en buscar()", e);
        }
        return lista;
    }

    @Override
    public Cliente buscarPorId(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearCliente(rs);
            }
        } catch (SQLException e) {
            log.error("Error en buscarPorId()", e);
        }
        return null;
    }

    @Override
    public Cliente buscarPorDocumento(String documento) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_DOCUMENTO)) {

            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearCliente(rs);
            }
        } catch (SQLException e) {
            log.error("Error en buscarPorDocumento()", e);
        }
        return null;
    }

    @Override
    public boolean guardar(Cliente cliente) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getTipoDocumento());
            ps.setString(4, cliente.getDocumento());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getEmail());
            ps.setString(7, cliente.getDireccion());
            ps.setString(8, cliente.getNacionalidad());
            ps.setBoolean(9, cliente.isActivo());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) cliente.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("Error en guardar()", e);
        }
        return false;
    }

    @Override
    public boolean actualizar(Cliente cliente) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getTipoDocumento());
            ps.setString(4, cliente.getDocumento());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getEmail());
            ps.setString(7, cliente.getDireccion());
            ps.setString(8, cliente.getNacionalidad());
            ps.setBoolean(9, cliente.isActivo());
            ps.setInt(10, cliente.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en actualizar()", e);
        }
        return false;
    }

    @Override
    public boolean eliminar(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ELIMINAR)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            // Si tiene reservaciones asociadas, MySQL lanzará error de FK
            log.error("Error en eliminar()", e);
        }
        return false;
    }

    @Override
    public int contarTodos() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            log.error("Error en contarTodos()", e);
        }
        return 0;
    }

    public int contarActivos() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR_ACTIVOS);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error en contarActivos()", e);
        }
        return 0;
    }

    public int contarNuevosEsteMes() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR_NUEVOS_MES);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error en contarNuevosEsteMes()", e);
        }
        return 0;
    }

    public boolean cambiarActivo(int id, boolean activo) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CAMBIAR_ACTIVO)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en cambiarActivo()", e);
        }
        return false;
    }

    // =========================================================
    // MAPEO ResultSet → Cliente
    // =========================================================

    /**
     * Convierte una fila del ResultSet en un objeto Cliente.
     */
    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setApellido(rs.getString("apellido"));
        c.setTipoDocumento(rs.getString("tipo_documento"));
        c.setDocumento(rs.getString("documento"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setDireccion(rs.getString("direccion"));
        c.setNacionalidad(rs.getString("nacionalidad"));
        c.setActivo(rs.getBoolean("activo"));
        c.setFechaRegistro(rs.getTimestamp("fecha_registro"));
        return c;
    }
}
