package com.hotel.dao;

import com.hotel.modelo.TipoHabitacion;
import java.util.List;

/**
 * Interfaz DAO para la tabla tipo_habitacion.
 *
 * @author Fernando
 * @version 1.0
 */
public interface TipoHabitacionDAO {

    /** @return todos los tipos de habitación ordenados por nombre */
    List<TipoHabitacion> listarTodos();

    /** @return tipo por su ID, o null si no existe */
    TipoHabitacion buscarPorId(int id);

    /**
     * Inserta un nuevo tipo de habitación.
     * Si tiene éxito, asigna el ID generado al objeto.
     */
    boolean guardar(TipoHabitacion tipo);

    /** Actualiza nombre, descripción, precio_base y capacidad. */
    boolean actualizar(TipoHabitacion tipo);

    /**
     * Elimina el tipo si no tiene habitaciones asociadas.
     * MySQL lanzará FK error en caso contrario.
     */
    boolean eliminar(int id);
}
