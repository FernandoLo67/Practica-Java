package com.hotel.dao;

import com.hotel.modelo.Cliente;
import java.util.List;

/**
 * Interfaz DAO para la entidad Cliente.
 *
 * Define todas las operaciones que se pueden hacer con clientes
 * en la base de datos (CRUD = Create, Read, Update, Delete).
 *
 * @author Fernando
 * @version 1.0
 */
public interface ClienteDAO {

    /**
     * Obtiene la lista completa de todos los clientes registrados.
     *
     * @return Lista de clientes ordenada por apellido
     */
    List<Cliente> listarTodos();

    /**
     * Busca clientes cuyo nombre, apellido o documento contengan el texto dado.
     * Se usa para el buscador en tiempo real del panel de clientes.
     *
     * @param texto Texto a buscar (puede ser parcial)
     * @return Lista de clientes que coinciden con la búsqueda
     */
    List<Cliente> buscar(String texto);

    /**
     * Busca un cliente por su ID.
     *
     * @param id ID del cliente
     * @return El cliente encontrado, o null si no existe
     */
    Cliente buscarPorId(int id);

    /**
     * Busca un cliente por su número de documento.
     * Útil para evitar registrar el mismo cliente dos veces.
     *
     * @param documento Número de documento (DNI, pasaporte, etc.)
     * @return El cliente encontrado, o null si no existe
     */
    Cliente buscarPorDocumento(String documento);

    /**
     * Registra un nuevo cliente en la base de datos.
     *
     * @param cliente Objeto con los datos del nuevo cliente
     * @return true si se guardó correctamente
     */
    boolean guardar(Cliente cliente);

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param cliente Objeto con los datos actualizados (debe tener ID válido)
     * @return true si se actualizó correctamente
     */
    boolean actualizar(Cliente cliente);

    /**
     * Elimina un cliente de la base de datos.
     * Solo se puede eliminar si no tiene reservaciones asociadas.
     *
     * @param id ID del cliente a eliminar
     * @return true si se eliminó correctamente
     */
    boolean eliminar(int id);

    /**
     * Cuenta el total de clientes registrados.
     *
     * @return Número total de clientes
     */
    int contarTodos();
}
