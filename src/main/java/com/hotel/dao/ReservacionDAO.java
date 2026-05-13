package com.hotel.dao;

import com.hotel.modelo.Reservacion;
import java.sql.Date;
import java.util.List;

/**
 * Interfaz DAO para la gestión de reservaciones.
 *
 * @author Fernando
 * @version 1.0
 */
public interface ReservacionDAO {

    /** Retorna todas las reservaciones con cliente, habitación y usuario */
    List<Reservacion> listarTodas();

    /** Retorna reservaciones activas (PENDIENTE, CONFIRMADA, CHECKIN) */
    List<Reservacion> listarActivas();

    /** Busca reservaciones por nombre de cliente, número de habitación o estado */
    List<Reservacion> buscar(String texto);

    /** Busca una reservación por ID */
    Reservacion buscarPorId(int id);

    /** Guarda una nueva reservación */
    boolean guardar(Reservacion reservacion);

    /** Actualiza una reservación existente */
    boolean actualizar(Reservacion reservacion);

    /** Cambia únicamente el estado de una reservación */
    boolean cambiarEstado(int id, String nuevoEstado);

    /** Elimina una reservación (solo si está PENDIENTE o CANCELADA) */
    boolean eliminar(int id);

    /** Verifica si una habitación está disponible en un rango de fechas */
    boolean habitacionDisponible(int idHabitacion, Date fechaCheckin, Date fechaCheckout, int idReservacionExcluir);

    /** Cuenta reservaciones por estado */
    int contarPorEstado(String estado);
}
