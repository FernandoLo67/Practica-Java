package com.hotel.dao.impl;

import com.hotel.dao.TipoHabitacionDAO;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.ConexionDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de TipoHabitacionDAO.
 * Gestiona el CRUD de la tabla tipo_habitacion.
 *
 * @author Fernando
 * @version 1.0
 */
public class TipoHabitacionDAOImpl implements TipoHabitacionDAO {

    private static final Logger log = LoggerFactory.getLogger(TipoHabitacionDAOImpl.class);

    private static final String SQL_LISTAR =
        "SELECT id, nombre, descripcion, precio_base, capacidad " +
        "FROM tipo_habitacion ORDER BY nombre";

    private static final String SQL_POR_ID =
        "SELECT id, nombre, descripcion, precio_base, capacidad " +
        "FROM tipo_habitacion WHERE id = ?";

    private static final String SQL_GUARDAR =
        "INSERT INTO tipo_habitacion (nombre, descripcion, precio_base, capacidad) " +
        "VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
        "UPDATE tipo_habitacion SET nombre = ?, descripcion = ?, " +
        "precio_base = ?, capacidad = ? WHERE id = ?";

    private static final String SQL_ELIMINAR =
        "DELETE FROM tipo_habitacion WHERE id = ?";

    // =========================================================
    // IMPLEMENTACIÓN
    // =========================================================

    @Override
    public List<TipoHabitacion> listarTodos() {
        List<TipoHabitacion> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));

        } catch (SQLException e) {
            log.error("Error en listarTodos()", e);
        }
        return lista;
    }

    @Override
    public TipoHabitacion buscarPorId(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            log.error("Error en buscarPorId()", e);
        }
        return null;
    }

    @Override
    public boolean guardar(TipoHabitacion tipo) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setDouble(3, tipo.getPrecioBase());
            ps.setInt   (4, tipo.getCapacidad());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) tipo.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("Error en guardar()", e);
        }
        return false;
    }

    @Override
    public boolean actualizar(TipoHabitacion tipo) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setDouble(3, tipo.getPrecioBase());
            ps.setInt   (4, tipo.getCapacidad());
            ps.setInt   (5, tipo.getId());

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
            // FK: el tipo tiene habitaciones asociadas
            log.error("Error en eliminar() — posiblemente tiene habitaciones asignadas", e);
        }
        return false;
    }

    // =========================================================
    // MAPEO
    // =========================================================

    private TipoHabitacion mapear(ResultSet rs) throws SQLException {
        TipoHabitacion t = new TipoHabitacion();
        t.setId         (rs.getInt   ("id"));
        t.setNombre     (rs.getString("nombre"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setPrecioBase (rs.getDouble("precio_base"));
        t.setCapacidad  (rs.getInt   ("capacidad"));
        return t;
    }
}
