package com.hotel.dao;

import com.hotel.modelo.Factura;
import java.util.List;

/**
 * Interfaz DAO para la gestión de facturas.
 *
 * @author Fernando
 * @version 1.0
 */
public interface FacturaDAO {

    List<Factura> listarTodas();
    Factura buscarPorId(int id);
    Factura buscarPorReservacion(int idReservacion);
    boolean guardar(Factura factura);
    boolean actualizarEstado(int id, String nuevoEstado);
    int contarPorEstado(String estado);
    double sumarTotalPorEstado(String estado);
}
