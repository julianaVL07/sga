package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02) de los canales de venta, indexada por el nombre. */
public class CanalVentaRepositoryInMemory implements CanalVentaRepository {

    private final Map<String, CanalVenta> almacen = new ConcurrentHashMap<>();

    @Override
    public CanalVenta guardar(CanalVenta canal) {
        if (canal == null) {
            throw new IllegalArgumentException("El canal a guardar es obligatorio");
        }
        almacen.put(canal.getNombre(), canal);
        return canal;
    }

    @Override
    public Optional<CanalVenta> buscarPorNombre(String nombre) {
        return Optional.ofNullable(nombre).map(almacen::get);
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return nombre != null && almacen.containsKey(nombre);
    }

    @Override
    public Pagina<CanalVenta> listar(int pagina, int tamano) {
        return Pagina.de(almacen.values().stream().sorted(Comparator.comparing(CanalVenta::getNombre)).toList(),
                pagina, tamano);
    }
}
