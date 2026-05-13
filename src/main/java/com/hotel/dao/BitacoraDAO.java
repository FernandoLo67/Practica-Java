package com.hotel.dao;

import com.hotel.modelo.Bitacora;
import java.util.List;

/**
 * Interfaz para el acceso a datos de la bitácora de auditoría.
 *
 * @author Fernando
 */
public interface BitacoraDAO {

    /** Registra una nueva entrada en la bitácora. */
    void registrar(Bitacora b);

    /** Devuelve todas las entradas, ordenadas por fecha descendente. */
    List<Bitacora> listarTodas();

    /** Filtra por módulo. */
    List<Bitacora> listarPorModulo(String modulo);

    /** Filtra por usuario. */
    List<Bitacora> listarPorUsuario(int idUsuario);
}
