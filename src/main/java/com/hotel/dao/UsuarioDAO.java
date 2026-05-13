package com.hotel.dao;

import com.hotel.modelo.Usuario;
import java.util.List;

/**
 * Interfaz DAO (Data Access Object) para la entidad Usuario.
 *
 * ¿Qué es un DAO?
 *   Es una clase que se encarga de hablar con la base de datos.
 *   Separa la lógica de acceso a datos del resto de la aplicación.
 *   La interfaz define QUÉ operaciones se pueden hacer.
 *   La implementación define CÓMO se hacen (con JDBC, en este caso).
 *
 * Patrón: Interfaz → Implementación
 *   UsuarioDAO (interfaz)  → UsuarioDAOImpl (implementación)
 *
 * @author Fernando
 * @version 1.0
 */
public interface UsuarioDAO {

    /**
     * Verifica las credenciales de un usuario para el login.
     *
     * @param usuario  Nombre de usuario
     * @param password Contraseña
     * @return El objeto Usuario si las credenciales son correctas, null si no
     */
    Usuario autenticar(String usuario, String password);

    /**
     * Busca un usuario por su ID.
     *
     * @param id El ID del usuario en la base de datos
     * @return El usuario encontrado, o null si no existe
     */
    Usuario buscarPorId(int id);

    /**
     * Obtiene la lista de todos los usuarios del sistema.
     *
     * @return Lista de usuarios (vacía si no hay ninguno)
     */
    List<Usuario> listarTodos();

    /**
     * Guarda un nuevo usuario en la base de datos.
     *
     * @param usuario El objeto con los datos a guardar
     * @return true si se guardó correctamente, false si hubo error
     */
    boolean guardar(Usuario usuario);

    /**
     * Actualiza los datos de un usuario existente.
     *
     * @param usuario El objeto con los datos actualizados (debe tener ID)
     * @return true si se actualizó correctamente
     */
    boolean actualizar(Usuario usuario);

    /**
     * Verifica si un nombre de usuario ya está registrado.
     * Útil para evitar duplicados al crear usuarios.
     *
     * @param nombreUsuario El nombre de usuario a verificar
     * @return true si ya existe
     */
    boolean existeUsuario(String nombreUsuario);

    /**
     * Actualiza solo la contraseña de un usuario (ya hasheada con BCrypt).
     *
     * @param idUsuario ID del usuario
     * @param nuevoHashBcrypt Hash BCrypt de la nueva contraseña
     * @return true si se actualizó correctamente
     */
    boolean actualizarPassword(int idUsuario, String nuevoHashBcrypt);
}
