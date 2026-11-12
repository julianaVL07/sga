package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;

import java.util.Optional;

/** Puerto de persistencia de los canales de venta externos (10.3.1). Nunca se borra un canal: se desactiva. */
public interface CanalVentaRepository {

    /** Inserta el canal o actualiza el que tiene el mismo nombre. */
    CanalVenta guardar(CanalVenta canal);

    /** Busca por el nombre ya normalizado (CanalVenta.normalizarNombre). */
    Optional<CanalVenta> buscarPorNombre(String nombre);

    boolean existePorNombre(String nombre);

    /** Todos los canales, activos e inactivos, ordenados por nombre (12.4: paginado). */
    Pagina<CanalVenta> listar(int pagina, int tamano);
}
