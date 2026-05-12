package com.hotel.dao.impl;

import com.hotel.dao.UsuarioDAO;
import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de UsuarioDAO usando JDBC para MySQL.
 *
 * Aquí es donde se escriben las consultas SQL y se
 * mapean los resultados a objetos Java.
 *
 * CONCEPTOS CLAVE USADOS:
 *   - PreparedStatement: evita inyección SQL y mejora rendimiento
 *   - ResultSet: el resultado de una consulta SELECT
 *   - try-with-resources: cierra automáticamente los recursos
 *
 * @author Fernando
 * @version 1.0
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    // =========================================================
    // CONSULTAS SQL (constantes para fácil mantenimiento)
    // =========================================================

    private static final String SQL_AUTENTICAR =
        "SELECT * FROM usuarios WHERE usuario = ? AND password = ? AND activo = TRUE";

    private static final String SQL_BUSCAR_POR_ID =
        "SELECT * FROM usuarios WHERE id = ?";

    private static final String SQL_LISTAR_TODOS =
        "SELECT * FROM usuarios ORDER BY nombre";

    private static final String SQL_GUARDAR =
        "INSERT INTO usuarios (nombre, usuario, password, rol, activo) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
        "UPDATE usuarios SET nombre = ?, usuario = ?, rol = ?, activo = ? WHERE id = ?";

    private static final String SQL_EXISTE_USUARIO =
        "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";

    // =========================================================
    // IMPLEMENTACIÓN DE LOS MÉTODOS
    // =========================================================

    /**
     * Autentica un usuario verificando usuario y contraseña.
     *
     * Nota de seguridad: En producción se debe comparar el hash
     * de la contraseña, no la contraseña en texto plano.
     */
    @Override
    public Usuario autenticar(String usuario, String password) {
        // try-with-resources: cierra conn y ps automáticamente
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_AUTENTICAR)) {

            // El '?' de la posición 1 recibe el usuario
            ps.setString(1, usuario);
            // El '?' de la posición 2 recibe la contraseña
            ps.setString(2, password);

            // Ejecutamos la consulta y obtenemos los resultados
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Si encontró un resultado, mapear a objeto Usuario
                    return mapearUsuario(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error en autenticar(): " + e.getMessage());
        }

        return null; // null = credenciales incorrectas o error
    }

    /**
     * Busca un usuario por su ID numérico.
     */
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
            System.err.println("Error en buscarPorId(): " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene todos los usuarios ordenados por nombre.
     */
    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();

        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = ps.executeQuery()) {

            // Iterar sobre cada fila del resultado
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error en listarTodos(): " + e.getMessage());
        }

        return lista;
    }

    /**
     * Inserta un nuevo usuario en la base de datos.
     */
    @Override
    public boolean guardar(Usuario usuario) {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     SQL_GUARDAR, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, usuario.getPassword());
            ps.setString(4, usuario.getRol());
            ps.setBoolean(5, usuario.isActivo());

            int filasAfectadas = ps.executeUpdate();

            // Obtener el ID generado automáticamente por MySQL
            if (filasAfectadas > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        usuario.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error en guardar(): " + e.getMessage());
        }
        return false;
    }

    /**
     * Actualiza los datos de un usuario existente.
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
            System.err.println("Error en actualizar(): " + e.getMessage());
        }
        return false;
    }

    /**
     * Verifica si un nombre de usuario ya existe en la BD.
     */
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
            System.err.println("Error en existeUsuario(): " + e.getMessage());
        }
        return false;
    }

    // =========================================================
    // MÉTODO PRIVADO: Mapear ResultSet → Objeto Usuario
    // =========================================================

    /**
     * Convierte una fila del ResultSet en un objeto Usuario.
     * Este método se reutiliza en todos los métodos de consulta.
     *
     * @param rs ResultSet posicionado en la fila a convertir
     * @return Objeto Usuario con los datos de esa fila
     * @throws SQLException si hay error al leer las columnas
     */
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
