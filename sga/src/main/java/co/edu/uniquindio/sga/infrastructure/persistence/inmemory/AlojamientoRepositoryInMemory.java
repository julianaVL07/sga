package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02). El mapa está indexado por el id del alojamiento. */
public class AlojamientoRepositoryInMemory implements AlojamientoRepository {

    private final Map<String, Alojamiento> almacen = new ConcurrentHashMap<>();

    @Override
    public Alojamiento guardar(Alojamiento alojamiento) {
        if (alojamiento == null) {
            throw new IllegalArgumentException("El alojamiento a guardar es obligatorio");
        }
        boolean esOtroAlojamiento = !almacen.isEmpty() && !almacen.containsKey(alojamiento.getId());
        if (esOtroAlojamiento) {
            throw new ReglaDominioException("F-13: el sistema administra un único alojamiento");
        }
        almacen.put(alojamiento.getId(), alojamiento);
        return alojamiento;
    }

    @Override
    public Optional<Alojamiento> buscarPorId(String id) {
        return Optional.ofNullable(id).map(almacen::get);
    }

    @Override
    public Optional<Alojamiento> obtener() {
        return almacen.values().stream().findFirst();
    }
}
