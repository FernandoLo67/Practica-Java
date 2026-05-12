package com.hotel.dao;

import com.hotel.modelo.Habitacion;
import java.util.List;

/**
 * Interfaz DAO para la gestión de habitaciones.
 * Define todas las operaciones posibles sobre la tabla 'habitaciones'.
 *
 * @author Fernando
 * @version 1.0
 */
public interface HabitacionDAO {

    /** Retorna todas las habitaciones con su tipo asociado */
    List<Habitacion> listarTodas();

    /** Retorna solo las habitaciones disponibles */
    List<Habitacion> listarDisponibles();

    /** Busca habitaciones por número, tipo o estado */
    List<Habitacion> buscar(String texto);

    /** Busca una habitación por su ID */
    Habitacion buscarPorId(int id);

    /** Guarda una nueva habitación en la BD */
    boolean guardar(Habitacion habitacion);

    /** Actualiza los datos de una habitación existente */
    boolean actualizar(Habitacion habitacion);

    /** Cambia únicamente el estado de una habitación */
    boolean cambiarEstado(int id, String nuevoEstado);

    /** Cuenta el total de habitaciones */
    int contarTodas();

    /** Cuenta habitaciones por estado */
    int contarPorEstado(String estado);
}
