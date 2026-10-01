package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por el id del conflicto. */
public class ConflictoCanalRepositoryInMemory implements ConflictoCanalRepository {

    private static final Comparator<ConflictoCanal> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(ConflictoCanal::getFecha).reversed();

    private final Map<String, ConflictoCanal> almacen = new ConcurrentHashMap<>();

    @Override
    public ConflictoCanal guardar(ConflictoCanal conflicto) {
        if (conflicto == null) {
            throw new IllegalArgumentException("El conflicto a guardar es obligatorio");
        }
        almacen.put(conflicto.getId(), conflicto);
        return conflicto;
    }

    @Override
    public Optional<ConflictoCanal> buscarPorId(String id) {
        return Optional.ofNullable(id).map(almacen::get);
    }

    @Override
    public List<ConflictoCanal> listarPendientes() {
        return almacen.values().stream()
                .filter(ConflictoCanal::estaPendiente)
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }

    @Override
    public List<ConflictoCanal> listarTodos() {
        return almacen.values().stream().sorted(MAS_RECIENTE_PRIMERO).toList();
    }
}
