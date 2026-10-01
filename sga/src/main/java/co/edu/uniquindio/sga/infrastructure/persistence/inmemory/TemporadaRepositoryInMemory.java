package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por el id de la temporada. */
public class TemporadaRepositoryInMemory implements TemporadaRepository {

    private final Map<String, Temporada> almacen = new ConcurrentHashMap<>();

    @Override
    public synchronized Temporada guardar(Temporada temporada) {
        if (temporada == null) {
            throw new IllegalArgumentException("La temporada a guardar es obligatoria");
        }
        if (temporada.esBase()) {
            buscarBase().filter(base -> !base.equals(temporada)).ifPresent(base -> {
                throw new ReglaDominioException("F-05: ya existe la temporada base " + base.getNombre());
            });
        }
        almacen.put(temporada.getId(), temporada);
        return temporada;
    }

    @Override
    public Optional<Temporada> buscarPorId(String id) {
        return Optional.ofNullable(id).map(almacen::get);
    }

    @Override
    public Optional<Temporada> buscarBase() {
        return almacen.values().stream().filter(Temporada::esBase).findFirst();
    }

    @Override
    public List<Temporada> listarTodas() {
        return almacen.values().stream().sorted(Comparator.comparing(Temporada::getNombre)).toList();
    }
}
