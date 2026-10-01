package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por el id de la novedad. */
public class NovedadRepositoryInMemory implements NovedadRepository {

    private static final Comparator<Novedad> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(Novedad::getFecha).reversed();

    private final Map<String, Novedad> almacen = new ConcurrentHashMap<>();

    @Override
    public Novedad guardar(Novedad novedad) {
        if (novedad == null) {
            throw new IllegalArgumentException("La novedad a guardar es obligatoria");
        }
        almacen.put(novedad.getId(), novedad);
        return novedad;
    }

    @Override
    public Optional<Novedad> buscarPorId(String id) {
        return Optional.ofNullable(id).map(almacen::get);
    }

    @Override
    public List<Novedad> listarPorApartamento(IdentificacionApartamento apartamento) {
        return almacen.values().stream()
                .filter(n -> n.perteneceA(apartamento))
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }

    @Override
    public List<Novedad> listarQueRequierenAtencionInmediata() {
        return almacen.values().stream()
                .filter(Novedad::requiereAtencionInmediata)
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }
}
