package com.hotel.dao.impl;

import com.hotel.dao.BitacoraDAO;
import com.hotel.modelo.Bitacora;
import com.hotel.util.ConexionDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de BitacoraDAO.
 *
 * Las escrituras nunca lanzan excepciones al llamador — si falla,
 * solo se registra en el log para no interrumpir el flujo normal.
 *
 * @author Fernando
 * @version 1.0
 */
public class BitacoraDAOImpl implements BitacoraDAO {

    private static final Logger log = LoggerFactory.getLogger(BitacoraDAOImpl.class);

    private static final String SQL_INSERT =
        "INSERT INTO bitacora (id_usuario, usuario_nombre, accion, modulo, descripcion) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_LISTAR =
        "SELECT id, id_usuario, usuario_nombre, accion, modulo, descripcion, fecha " +
        "FROM bitacora ORDER BY fecha DESC LIMIT 500";

    private static final String SQL_POR_MODULO =
        "SELECT id, id_usuario, usuario_nombre, accion, modulo, descripcion, fecha " +
        "FROM bitacora WHERE modulo = ? ORDER BY fecha DESC LIMIT 300";

    private static final String SQL_POR_USUARIO =
        "SELECT id, id_usuario, usuario_nombre, accion, modulo, descripcion, fecha " +
        "FROM bitacora WHERE id_usuario = ? ORDER BY fecha DESC LIMIT 300";

    // =========================================================
    // registrar()
    // =========================================================

    @Override
    public void registrar(Bitacora b) {
        // Se ejecuta en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try (Connection conn = ConexionDB.getConexion();
                 PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

                if (b.getIdUsuario() != null) {
                    ps.setInt(1, b.getIdUsuario());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setString(2, b.getUsuarioNombre() != null ? b.getUsuarioNombre() : "Sistema");
                ps.setString(3, b.getAccion());
                ps.setString(4, b.getModulo());
                ps.setString(5, b.getDescripcion());
                ps.executeUpdate();

                log.debug("Bitácora: [{}] {} — {}", b.getModulo(), b.getAccion(), b.getDescripcion());

            } catch (Exception e) {
                log.error("No se pudo registrar en bitácora: {}", b, e);
            }
        }, "bitacora-insert").start();
    }

    // =========================================================
    // listarTodas()
    // =========================================================

    @Override
    public List<Bitacora> listarTodas() {
        List<Bitacora> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) {
            log.error("Error en listarTodas()", e);
        }
        return lista;
    }

    // =========================================================
    // listarPorModulo()
    // =========================================================

    @Override
    public List<Bitacora> listarPorModulo(String modulo) {
        List<Bitacora> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_MODULO)) {
            ps.setString(1, modulo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (Exception e) {
            log.error("Error en listarPorModulo({})", modulo, e);
        }
        return lista;
    }

    // =========================================================
    // listarPorUsuario()
    // =========================================================

    @Override
    public List<Bitacora> listarPorUsuario(int idUsuario) {
        List<Bitacora> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_USUARIO)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (Exception e) {
            log.error("Error en listarPorUsuario({})", idUsuario, e);
        }
        return lista;
    }

    // =========================================================
    // MAPEO
    // =========================================================

    private Bitacora mapear(ResultSet rs) throws SQLException {
        Bitacora b = new Bitacora();
        b.setId(rs.getInt("id"));
        int uid = rs.getInt("id_usuario");
        if (!rs.wasNull()) b.setIdUsuario(uid);
        b.setUsuarioNombre(rs.getString("usuario_nombre"));
        b.setAccion(rs.getString("accion"));
        b.setModulo(rs.getString("modulo"));
        b.setDescripcion(rs.getString("descripcion"));
        b.setFecha(rs.getTimestamp("fecha"));
        return b;
    }
}
