package com.hotel.dao.impl;

import com.hotel.dao.ReservacionDAO;
import com.hotel.modelo.*;
import com.hotel.util.ConexionDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ReservacionDAO usando JDBC.
 * Usa JOINs para traer cliente, habitación, tipo y usuario en una sola consulta.
 *
 * @author Fernando
 * @version 1.0
 */
public class ReservacionDAOImpl implements ReservacionDAO {

    private static final Logger log = LoggerFactory.getLogger(ReservacionDAOImpl.class);

    // =========================================================
    // CONSULTAS SQL
    // =========================================================

    private static final String SQL_BASE =
        "SELECT r.id, r.fecha_checkin, r.fecha_checkout, r.estado, r.observaciones, r.fecha_reserva, " +
        "       c.id AS cli_id, c.nombre AS cli_nombre, c.apellido AS cli_apellido, " +
        "       c.tipo_documento, c.documento, c.telefono, c.email, " +
        "       h.id AS hab_id, h.numero, h.piso, h.estado AS hab_estado, h.descripcion AS hab_desc, " +
        "       t.id AS tipo_id, t.nombre AS tipo_nombre, t.precio_base, t.capacidad, " +
        "       u.id AS usu_id, u.nombre AS usu_nombre, u.rol " +
        "FROM reservaciones r " +
        "INNER JOIN clientes      c ON r.id_cliente    = c.id " +
        "INNER JOIN habitaciones  h ON r.id_habitacion = h.id " +
        "INNER JOIN tipo_habitacion t ON h.id_tipo     = t.id " +
        "INNER JOIN usuarios      u ON r.id_usuario    = u.id ";

    private static final String SQL_LISTAR_TODAS =
        SQL_BASE + "ORDER BY r.fecha_checkin DESC";

    private static final String SQL_LISTAR_ACTIVAS =
        SQL_BASE + "WHERE r.estado IN ('PENDIENTE','CONFIRMADA','CHECKIN') " +
        "ORDER BY r.fecha_checkin ASC";

    private static final String SQL_BUSCAR =
        SQL_BASE +
        "WHERE c.nombre LIKE ? OR c.apellido LIKE ? OR h.numero LIKE ? OR r.estado LIKE ? " +
        "ORDER BY r.fecha_checkin DESC";

    private static final String SQL_BUSCAR_POR_ID =
        SQL_BASE + "WHERE r.id = ?";

    private static final String SQL_GUARDAR =
        "INSERT INTO reservaciones (id_cliente, id_habitacion, fecha_checkin, fecha_checkout, " +
        "estado, observaciones, id_usuario) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
        "UPDATE reservaciones SET id_cliente = ?, id_habitacion = ?, fecha_checkin = ?, " +
        "fecha_checkout = ?, estado = ?, observaciones = ? WHERE id = ?";

    private static final String SQL_CAMBIAR_ESTADO =
        "UPDATE reservaciones SET estado = ? WHERE id = ?";

    private static final String SQL_ELIMINAR =
        "DELETE FROM reservaciones WHERE id = ?";

    private static final String SQL_DISPONIBILIDAD =
        "SELECT COUNT(*) FROM reservaciones " +
        "WHERE id_habitacion = ? " +
        "AND id != ? " +
        "AND estado NOT IN ('CANCELADA','CHECKOUT') " +
        "AND NOT (fecha_checkout <= ? OR fecha_checkin >= ?)";

    private static final String SQL_CONTAR_ESTADO =
        "SELECT COUNT(*) FROM reservaciones WHERE estado = ?";

    // =========================================================
    // IMPLEMENTACIÓN
    // =========================================================

    @Override
    public List<Reservacion> listarTodas() {
        List<Reservacion> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_TODAS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error en listarTodas(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public List<Reservacion> listarActivas() {
        List<Reservacion> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_ACTIVAS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error en listarActivas(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public List<Reservacion> buscar(String texto) {
        List<Reservacion> lista = new ArrayList<>();
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
            log.error("Error en buscar(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public Reservacion buscarPorId(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            log.error("Error en buscarPorId(): " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean guardar(Reservacion r) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, r.getCliente().getId());
            ps.setInt   (2, r.getHabitacion().getId());
            ps.setDate  (3, r.getFechaCheckin());
            ps.setDate  (4, r.getFechaCheckout());
            ps.setString(5, r.getEstado());
            ps.setString(6, r.getObservaciones());
            ps.setInt   (7, r.getUsuarioRegistro().getId());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) r.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("Error en guardar(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean actualizar(Reservacion r) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setInt   (1, r.getCliente().getId());
            ps.setInt   (2, r.getHabitacion().getId());
            ps.setDate  (3, r.getFechaCheckin());
            ps.setDate  (4, r.getFechaCheckout());
            ps.setString(5, r.getEstado());
            ps.setString(6, r.getObservaciones());
            ps.setInt   (7, r.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en actualizar(): " + e.getMessage());
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
            log.error("Error en cambiarEstado(): " + e.getMessage());
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
            log.error("Error en eliminar(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean habitacionDisponible(int idHabitacion, java.sql.Date checkin,
                                        java.sql.Date checkout, int idExcluir) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_DISPONIBILIDAD)) {
            ps.setInt (1, idHabitacion);
            ps.setInt (2, idExcluir);
            ps.setDate(3, checkin);
            ps.setDate(4, checkout);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            log.error("Error en habitacionDisponible(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public int contarPorEstado(String estado) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR_ESTADO)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error en contarPorEstado(): " + e.getMessage());
        }
        return 0;
    }

    // =========================================================
    // MAPEO ResultSet → Reservacion
    // =========================================================

    private Reservacion mapear(ResultSet rs) throws SQLException {
        // Cliente
        Cliente c = new Cliente();
        c.setId           (rs.getInt   ("cli_id"));
        c.setNombre       (rs.getString("cli_nombre"));
        c.setApellido     (rs.getString("cli_apellido"));
        c.setTipoDocumento(rs.getString("tipo_documento"));
        c.setDocumento    (rs.getString("documento"));
        c.setTelefono     (rs.getString("telefono"));
        c.setEmail        (rs.getString("email"));

        // Tipo habitación
        TipoHabitacion tipo = new TipoHabitacion();
        tipo.setId        (rs.getInt   ("tipo_id"));
        tipo.setNombre    (rs.getString("tipo_nombre"));
        tipo.setPrecioBase(rs.getDouble("precio_base"));
        tipo.setCapacidad (rs.getInt   ("capacidad"));

        // Habitación
        Habitacion h = new Habitacion();
        h.setId         (rs.getInt   ("hab_id"));
        h.setNumero     (rs.getString("numero"));
        h.setPiso       (rs.getInt   ("piso"));
        h.setEstado     (rs.getString("hab_estado"));
        h.setDescripcion(rs.getString("hab_desc"));
        h.setTipo       (tipo);

        // Usuario
        Usuario u = new Usuario();
        u.setId    (rs.getInt   ("usu_id"));
        u.setNombre(rs.getString("usu_nombre"));
        u.setRol   (rs.getString("rol"));

        // Reservación
        Reservacion r = new Reservacion();
        r.setId             (rs.getInt      ("id"));
        r.setCliente        (c);
        r.setHabitacion     (h);
        r.setFechaCheckin   (rs.getDate     ("fecha_checkin"));
        r.setFechaCheckout  (rs.getDate     ("fecha_checkout"));
        r.setEstado         (rs.getString   ("estado"));
        r.setObservaciones  (rs.getString   ("observaciones"));
        r.setFechaReserva   (rs.getTimestamp("fecha_reserva"));
        r.setUsuarioRegistro(u);

        return r;
    }
}
