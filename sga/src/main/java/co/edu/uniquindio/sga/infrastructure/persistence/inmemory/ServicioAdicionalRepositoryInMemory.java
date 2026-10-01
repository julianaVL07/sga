package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por el id del servicio. */
public class ServicioAdicionalRepositoryInMemory implements ServicioAdicionalRepository {

    private static final Comparator<ServicioAdicional> POR_NOMBRE = Comparator.comparing(ServicioAdicional::getNombre);

    private final Map<String, ServicioAdicional> almacen = new ConcurrentHashMap<>();

    @Override
    public ServicioAdicional guardar(ServicioAdicional servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio a guardar es obligatorio");
        }
        almacen.put(servicio.getId(), servicio);
        return servicio;
    }

    @Override
    public Optional<ServicioAdicional> buscarPorId(String id) {
        return Optional.ofNullable(id).map(almacen::get);
    }

    @Override
    public List<ServicioAdicional> listarActivos() {
        return almacen.values().stream().filter(ServicioAdicional::isActivo).sorted(POR_NOMBRE).toList();
    }

    @Override
    public List<ServicioAdicional> listarTodos() {
        return almacen.values().stream().sorted(POR_NOMBRE).toList();
    }
}
