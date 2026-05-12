package com.hotel.dao.impl;

import com.hotel.dao.HabitacionDAO;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de HabitacionDAO usando JDBC para MySQL.
 *
 * Usa JOINs con tipo_habitacion para traer toda la información
 * en una sola consulta, evitando el problema N+1.
 *
 * @author Fernando
 * @version 1.0
 */
public class HabitacionDAOImpl implements HabitacionDAO {

    // =========================================================
    // CONSULTAS SQL
    // =========================================================

    /** SELECT base con JOIN al tipo para obtener precio y capacidad */
    private static final String SQL_BASE =
        "SELECT h.id, h.numero, h.piso, h.estado, h.descripcion, h.imagen_url, " +
        "       t.id AS tipo_id, t.nombre AS tipo_nombre, t.descripcion AS tipo_desc, " +
        "       t.precio_base, t.capacidad " +
        "FROM habitaciones h " +
        "INNER JOIN tipo_habitacion t ON h.id_tipo = t.id ";

    private static final String SQL_LISTAR_TODAS =
        SQL_BASE + "ORDER BY h.piso, h.numero";

    private static final String SQL_LISTAR_DISPONIBLES =
        SQL_BASE + "WHERE h.estado = 'DISPONIBLE' ORDER BY h.piso, h.numero";

    private static final String SQL_BUSCAR =
        SQL_BASE +
        "WHERE h.numero LIKE ? OR t.nombre LIKE ? OR h.estado LIKE ? OR h.descripcion LIKE ? " +
        "ORDER BY h.piso, h.numero";

    private static final String SQL_BUSCAR_POR_ID =
        SQL_BASE + "WHERE h.id = ?";

    private static final String SQL_GUARDAR =
        "INSERT INTO habitaciones (numero, piso, id_tipo, estado, descripcion) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
        "UPDATE habitaciones SET numero = ?, piso = ?, id_tipo = ?, " +
        "estado = ?, descripcion = ? WHERE id = ?";

    private static final String SQL_CAMBIAR_ESTADO =
        "UPDATE habitaciones SET estado = ? WHERE id = ?";

    private static final String SQL_CONTAR =
        "SELECT COUNT(*) FROM habitaciones";

    private static final String SQL_CONTAR_POR_ESTADO =
        "SELECT COUNT(*) FROM habitaciones WHERE estado = ?";

    // =========================================================
    // IMPLEMENTACIÓN
    // =========================================================

    @Override
    public List<Habitacion> listarTodas() {
        List<Habitacion> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));

        } catch (SQLException e) {
            System.err.println("Error en listarTodas(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public List<Habitacion> listarDisponibles() {
        List<Habitacion> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_DISPONIBLES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));

        } catch (SQLException e) {
            System.err.println("Error en listarDisponibles(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public List<Habitacion> buscar(String texto) {
        List<Habitacion> lista = new ArrayList<>();
        String patron = "%" + texto + "%";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {

            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ps.setString(4, patron);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error en buscar(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public Habitacion buscarPorId(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error en buscarPorId(): " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean guardar(Habitacion h) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, h.getNumero());
            ps.setInt   (2, h.getPiso());
            ps.setInt   (3, h.getTipo().getId());
            ps.setString(4, h.getEstado());
            ps.setString(5, h.getDescripcion());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) h.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en guardar(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean actualizar(Habitacion h) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setString(1, h.getNumero());
            ps.setInt   (2, h.getPiso());
            ps.setInt   (3, h.getTipo().getId());
            ps.setString(4, h.getEstado());
            ps.setString(5, h.getDescripcion());
            ps.setInt   (6, h.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en actualizar(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean cambiarEstado(int id, String nuevoEstado) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CAMBIAR_ESTADO)) {

            ps.setString(1, nuevoEstado);
            ps.setInt   (2, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error en cambiarEstado(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public int contarTodas() {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error en contarTodas(): " + e.getMessage());
        }
        return 0;
    }

    @Override
    public int contarPorEstado(String estado) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR_POR_ESTADO)) {

            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error en contarPorEstado(): " + e.getMessage());
        }
        return 0;
    }

    // =========================================================
    // MAPEO ResultSet → Habitacion
    // =========================================================

    private Habitacion mapear(ResultSet rs) throws SQLException {
        // Mapear el tipo de habitación
        TipoHabitacion tipo = new TipoHabitacion();
        tipo.setId         (rs.getInt   ("tipo_id"));
        tipo.setNombre     (rs.getString("tipo_nombre"));
        tipo.setDescripcion(rs.getString("tipo_desc"));
        tipo.setPrecioBase (rs.getDouble("precio_base"));
        tipo.setCapacidad  (rs.getInt   ("capacidad"));

        // Mapear la habitación
        Habitacion h = new Habitacion();
        h.setId         (rs.getInt   ("id"));
        h.setNumero     (rs.getString("numero"));
        h.setPiso       (rs.getInt   ("piso"));
        h.setEstado     (rs.getString("estado"));
        h.setDescripcion(rs.getString("descripcion"));
        h.setImagenUrl  (rs.getString("imagen_url"));
        h.setTipo       (tipo);

        return h;
    }
}
