package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de apartamentos. No hay borrado físico: la eliminación es lógica
 * (Apartamento.desactivar) y se persiste con guardar (7.3, 12.4).
 */
public interface ApartamentoRepository {

    Apartamento guardar(Apartamento apartamento);

    /** Incluye apartamentos eliminados: las reservas históricas los siguen referenciando (7.3). */
    Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion);

    boolean existe(IdentificacionApartamento identificacion);

    /** Apartamentos activos y no eliminados, los únicos que aparecen en búsquedas (7.3). */
    List<Apartamento> listarVigentesParaVenta();

    /** Todos los no eliminados, para la administración. */
    List<Apartamento> listarNoEliminados();
}
