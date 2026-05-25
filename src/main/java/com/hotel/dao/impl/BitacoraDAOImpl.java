package com.hotel.dao.impl;

import com.hotel.dao.BitacoraDAO;
import com.hotel.modelo.Bitacora;
import com.hotel.util.ConexionDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Implementación JDBC de BitacoraDAO.
 *
 * Las escrituras son asíncronas y nunca bloquean la UI.
 * Se usa un ExecutorService con pool fijo (2 hilos) para evitar
 * la creación de hilos ilimitados ante carga alta.
 *
 * [A-03] Reemplazado new Thread(...).start() por ExecutorService.
 * Los hilos son daemon para no bloquear el shutdown de la JVM.
 *
 * @author Fernando
 * @version 1.1
 */
public class BitacoraDAOImpl implements BitacoraDAO {

    private static final Logger log = LoggerFactory.getLogger(BitacoraDAOImpl.class);

    /**
     * Pool de 2 hilos daemon para insertar en bitácora sin bloquear la UI.
     * Fijo en 2 porque las inserciones son rápidas y la BD es la misma.
     * Al ser daemon, no impide el cierre normal de la aplicación.
     */
    private static final ExecutorService BITACORA_EXECUTOR =
        Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "bitacora-worker-" + ++count);
                t.setDaemon(true);  // no bloquea el shutdown de la JVM
                return t;
            }
        });

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
        // [A-03] ExecutorService con pool fijo en lugar de new Thread ilimitado
        BITACORA_EXECUTOR.submit(() -> {
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
