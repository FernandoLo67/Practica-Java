package com.hotel.dao.impl;

import com.hotel.dao.UsuarioDAO;
import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionDB;
import com.hotel.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de UsuarioDAO usando JDBC para MySQL.
 *
 * Cambios respecto a v1.0:
 *   - autenticar(): ya NO compara password en SQL; la compara en Java
 *     con BCrypt (compatible con passwords antiguos en texto plano).
 *   - guardar(): hashea la contraseña con BCrypt antes de persistirla.
 *   - actualizarPassword(): método dedicado para cambiar contraseña.
 *
 * ¿Por qué no comparar en SQL?
 *   BCrypt genera un hash diferente cada vez para el mismo password.
 *   Por eso "admin123" != "$2a$10$xyz..." en SQL, aunque sean el mismo
 *   password. La verificación debe hacerse con BCrypt.checkpw() en Java.
 *
 * @author Fernando
 * @version 2.0 - BCrypt password hashing
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDAOImpl.class);

    // =========================================================
    // CONSULTAS SQL
    // =========================================================

    /** Busca usuario solo por nombre (la password se verifica en Java) */
    private static final String SQL_BUSCAR_POR_USUARIO =
        "SELECT * FROM usuarios WHERE usuario = ? AND activo = TRUE";

    private static final String SQL_BUSCAR_POR_ID =
        "SELECT * FROM usuarios WHERE id = ?";

    private static final String SQL_LISTAR_TODOS =
        "SELECT * FROM usuarios ORDER BY nombre";

    private static final String SQL_GUARDAR =
        "INSERT INTO usuarios (nombre, usuario, password, rol, activo) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
        "UPDATE usuarios SET nombre = ?, usuario = ?, rol = ?, activo = ? WHERE id = ?";

    private static final String SQL_ACTUALIZAR_PASSWORD =
        "UPDATE usuarios SET password = ? WHERE id = ?";

    private static final String SQL_EXISTE_USUARIO =
        "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";

    // =========================================================
    // AUTENTICACIÓN CON BCRYPT
    // =========================================================

    /**
     * Autentica un usuario comparando la contraseña con BCrypt.
     *
     * Flujo:
     *   1. Busca el usuario por nombre en la BD (sin filtrar por password)
     *   2. Si existe, usa PasswordUtil.verificar() para comparar
     *      (soporta hashes BCrypt y contraseñas antiguas en texto plano)
     *   3. Si el password era texto plano y es correcto, lo migra a BCrypt
     *
     * @return Usuario autenticado, o null si las credenciales son incorrectas
     */
    @Override
    public Usuario autenticar(String usuario, String password) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_USUARIO)) {

            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = mapearUsuario(rs);
                    String hashAlmacenado = u.getPassword();

                    // Verificar con BCrypt (o texto plano por backward compat)
                    if (PasswordUtil.verificar(password, hashAlmacenado)) {

                        // Migración automática: si el password era texto plano,
                        // aprovechar este login para hashearlo con BCrypt
                        if (!PasswordUtil.estaHasheada(hashAlmacenado)) {
                            String nuevoHash = PasswordUtil.hashear(password);
                            actualizarPassword(u.getId(), nuevoHash);
                            u.setPassword(nuevoHash);
                            System.out.println("✓ Password del usuario '" + usuario +
                                "' migrado a BCrypt automáticamente.");
                        }

                        return u;
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error en autenticar(): " + e.getMessage());
        }

        return null; // null = credenciales incorrectas o error
    }

    // =========================================================
    // CRUD
    // =========================================================

    @Override
    public Usuario buscarPorId(int id) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }

        } catch (SQLException e) {
            log.error("Error en buscarPorId(): " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }

        } catch (SQLException e) {
            log.error("Error en listarTodos(): " + e.getMessage());
        }

        return lista;
    }

    /**
     * Guarda un nuevo usuario hasheando su contraseña con BCrypt.
     * Si la contraseña ya está hasheada (edición posterior), no la vuelve a hashear.
     */
    @Override
    public boolean guardar(Usuario usuario) {
        // Hashear la contraseña si no lo está ya
        String passwordFinal = PasswordUtil.estaHasheada(usuario.getPassword())
            ? usuario.getPassword()
            : PasswordUtil.hashear(usuario.getPassword());

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, passwordFinal);
            ps.setString(4, usuario.getRol());
            ps.setBoolean(5, usuario.isActivo());

            int filas = ps.executeUpdate();

            if (filas > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        usuario.setId(keys.getInt(1));
                    }
                }
                usuario.setPassword(passwordFinal); // actualizar el objeto en memoria
                return true;
            }

        } catch (SQLException e) {
            log.error("Error en guardar(): " + e.getMessage());
        }
        return false;
    }

    /**
     * Actualiza nombre, usuario, rol y estado (NO la contraseña).
     * Para cambiar la contraseña, usar actualizarPassword().
     */
    @Override
    public boolean actualizar(Usuario usuario) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, usuario.getRol());
            ps.setBoolean(4, usuario.isActivo());
            ps.setInt(5, usuario.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error en actualizar(): " + e.getMessage());
        }
        return false;
    }

    /**
     * Actualiza SOLO la contraseña de un usuario (ya hasheada con BCrypt).
     * Se usa al cambiar contraseña desde el formulario de usuario.
     */
    @Override
    public boolean actualizarPassword(int idUsuario, String nuevoHashBcrypt) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR_PASSWORD)) {

            ps.setString(1, nuevoHashBcrypt);
            ps.setInt(2, idUsuario);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error en actualizarPassword(): " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean existeUsuario(String nombreUsuario) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTE_USUARIO)) {

            ps.setString(1, nombreUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            log.error("Error en existeUsuario(): " + e.getMessage());
        }
        return false;
    }

    // =========================================================
    // MAPEO ResultSet → Usuario
    // =========================================================

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setPassword(rs.getString("password"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        u.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        return u;
    }
}
