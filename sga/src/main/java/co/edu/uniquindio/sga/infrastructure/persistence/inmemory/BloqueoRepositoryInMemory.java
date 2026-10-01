package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por el id del bloqueo. */
public class BloqueoRepositoryInMemory implements BloqueoRepository {

    private static final Comparator<Bloqueo> POR_INICIO =
            Comparator.comparing(b -> b.getRango().fechaInicio());

    private final Map<String, Bloqueo> almacen = new ConcurrentHashMap<>();

    @Override
    public Bloqueo guardar(Bloqueo bloqueo) {
        if (bloqueo == null) {
            throw new IllegalArgumentException("El bloqueo a guardar es obligatorio");
        }
        almacen.put(bloqueo.getId(), bloqueo);
        return bloqueo;
    }

    @Override
    public Optional<Bloqueo> buscarPorId(String id) {
        return Optional.ofNullable(id).map(almacen::get);
    }

    @Override
    public List<Bloqueo> listarVigentesPorApartamento(IdentificacionApartamento apartamento) {
        return almacen.values().stream()
                .filter(Bloqueo::isVigente)
                .filter(b -> b.perteneceA(apartamento))
                .sorted(POR_INICIO)
                .toList();
    }

    @Override
    public List<Bloqueo> listarVigentesQueSeSolapan(IdentificacionApartamento apartamento, Estancia estancia) {
        return listarVigentesPorApartamento(apartamento).stream()
                .filter(b -> b.seSolapaCon(estancia))
                .toList();
    }
}
