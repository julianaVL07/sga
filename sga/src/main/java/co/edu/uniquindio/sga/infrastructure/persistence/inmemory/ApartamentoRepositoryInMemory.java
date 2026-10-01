package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por la identificación del apartamento. */
public class ApartamentoRepositoryInMemory implements ApartamentoRepository {

    private static final Comparator<Apartamento> POR_CODIGO =
            Comparator.comparing(a -> a.getIdentificacion().codigo());

    private final Map<IdentificacionApartamento, Apartamento> almacen = new ConcurrentHashMap<>();

    @Override
    public Apartamento guardar(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento a guardar es obligatorio");
        }
        almacen.put(apartamento.getIdentificacion(), apartamento);
        return apartamento;
    }

    @Override
    public Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion) {
        return Optional.ofNullable(identificacion).map(almacen::get);
    }

    @Override
    public boolean existe(IdentificacionApartamento identificacion) {
        return identificacion != null && almacen.containsKey(identificacion);
    }

    @Override
    public List<Apartamento> listarVigentesParaVenta() {
        return almacen.values().stream()
                .filter(Apartamento::estaVigenteParaVenta)
                .sorted(POR_CODIGO)
                .toList();
    }

    @Override
    public List<Apartamento> listarNoEliminados() {
        return almacen.values().stream()
                .filter(a -> !a.isEliminado())
                .sorted(POR_CODIGO)
                .toList();
    }
}
