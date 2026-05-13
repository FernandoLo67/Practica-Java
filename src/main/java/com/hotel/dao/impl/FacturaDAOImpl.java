package com.hotel.dao.impl;

import com.hotel.dao.FacturaDAO;
import com.hotel.modelo.*;
import com.hotel.util.ConexionDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de FacturaDAO usando JDBC.
 *
 * @author Fernando
 * @version 1.0
 */
public class FacturaDAOImpl implements FacturaDAO {

    private static final Logger log = LoggerFactory.getLogger(FacturaDAOImpl.class);

    private static final String SQL_BASE =
        "SELECT f.id, f.fecha_emision, f.subtotal, f.impuesto, f.total, " +
        "       f.estado, f.metodo_pago, f.observaciones, " +
        "       r.id AS res_id, r.fecha_checkin, r.fecha_checkout, r.estado AS res_estado, " +
        "       c.id AS cli_id, c.nombre AS cli_nombre, c.apellido AS cli_apellido, c.documento, " +
        "       h.id AS hab_id, h.numero, h.piso, " +
        "       t.nombre AS tipo_nombre, t.precio_base " +
        "FROM facturas f " +
        "INNER JOIN reservaciones r  ON f.id_reservacion = r.id " +
        "INNER JOIN clientes c       ON r.id_cliente     = c.id " +
        "INNER JOIN habitaciones h   ON r.id_habitacion  = h.id " +
        "INNER JOIN tipo_habitacion t ON h.id_tipo       = t.id ";

    private static final String SQL_LISTAR =
        SQL_BASE + "ORDER BY f.fecha_emision DESC";

    private static final String SQL_POR_ID =
        SQL_BASE + "WHERE f.id = ?";

    private static final String SQL_POR_RESERVACION =
        SQL_BASE + "WHERE f.id_reservacion = ?";

    private static final String SQL_GUARDAR =
        "INSERT INTO facturas (id_reservacion, subtotal, impuesto, total, estado, metodo_pago, observaciones) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR_ESTADO =
        "UPDATE facturas SET estado = ? WHERE id = ?";

    private static final String SQL_CONTAR_ESTADO =
        "SELECT COUNT(*) FROM facturas WHERE estado = ?";

    private static final String SQL_SUMAR_ESTADO =
        "SELECT COALESCE(SUM(total), 0) FROM facturas WHERE estado = ?";

    @Override
    public List<Factura> listarTodas() {
        List<Factura> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Error en listarTodas(): " + e.getMessage());
        }
        return lista;
    }

    @Override
    public Factura buscarPorId(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_ID)) {
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
    public Factura buscarPorReservacion(int idReservacion) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_RESERVACION)) {
            ps.setInt(1, idReservacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            log.error("Error en buscarPorReservacion(): " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean guardar(Factura f) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, f.getReservacion().getId());
            ps.setDouble(2, f.getSubtotal());
            ps.setDouble(3, f.getImpuesto());
            ps.setDouble(4, f.getTotal());
            ps.setString(5, f.getEstado() != null ? f.getEstado() : Factura.ESTADO_PENDIENTE);
            ps.setString(6, f.getMetodoPago());
            ps.setString(7, f.getObservaciones());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) f.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("Error en guardar(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean actualizarEstado(int id, String nuevoEstado) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
            ps.setString(1, nuevoEstado);
            ps.setInt   (2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error en actualizarEstado(): " + e.getMessage());
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

    @Override
    public double sumarTotalPorEstado(String estado) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_SUMAR_ESTADO)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            log.error("Error en sumarTotalPorEstado(): " + e.getMessage());
        }
        return 0;
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        TipoHabitacion tipo = new TipoHabitacion();
        tipo.setNombre    (rs.getString("tipo_nombre"));
        tipo.setPrecioBase(rs.getDouble("precio_base"));

        Habitacion h = new Habitacion();
        h.setId    (rs.getInt   ("hab_id"));
        h.setNumero(rs.getString("numero"));
        h.setPiso  (rs.getInt   ("piso"));
        h.setTipo  (tipo);

        Cliente c = new Cliente();
        c.setId      (rs.getInt   ("cli_id"));
        c.setNombre  (rs.getString("cli_nombre"));
        c.setApellido(rs.getString("cli_apellido"));
        c.setDocumento(rs.getString("documento"));

        Reservacion r = new Reservacion();
        r.setId           (rs.getInt ("res_id"));
        r.setCliente      (c);
        r.setHabitacion   (h);
        r.setFechaCheckin (rs.getDate("fecha_checkin"));
        r.setFechaCheckout(rs.getDate("fecha_checkout"));
        r.setEstado       (rs.getString("res_estado"));

        Factura f = new Factura();
        f.setId           (rs.getInt      ("id"));
        f.setReservacion  (r);
        f.setFechaEmision (rs.getTimestamp("fecha_emision"));
        f.setSubtotal     (rs.getDouble   ("subtotal"));
        f.setImpuesto     (rs.getDouble   ("impuesto"));
        f.setTotal        (rs.getDouble   ("total"));
        f.setEstado       (rs.getString   ("estado"));
        f.setMetodoPago   (rs.getString   ("metodo_pago"));
        f.setObservaciones(rs.getString   ("observaciones"));

        return f;
    }
}
