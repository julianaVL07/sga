package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de novedades (7.6). */
public interface NovedadRepository {

    Novedad guardar(Novedad novedad);

    Optional<Novedad> buscarPorId(String id);

    /** 7.6: historial de novedades de un apartamento, de la más reciente a la más antigua. */
    List<Novedad> listarPorApartamento(IdentificacionApartamento apartamento);

    /**
     * 7.6 y 12.4: una página del historial de novedades de un apartamento, de la más reciente a la más antigua.
     * La numeración de páginas empieza en 0.
     */
    Pagina<Novedad> listarPorApartamento(IdentificacionApartamento apartamento, int pagina, int tamano);

    /** Novedades de gravedad ALTA sin atender. */
    List<Novedad> listarQueRequierenAtencionInmediata();
}
