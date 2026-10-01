package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Temporada;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de temporadas (F-05, 7.4). */
public interface TemporadaRepository {

    Temporada guardar(Temporada temporada);

    Optional<Temporada> buscarPorId(String id);

    /** F-05: la temporada base obligatoria. */
    Optional<Temporada> buscarBase();

    List<Temporada> listarTodas();
}
