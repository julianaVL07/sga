package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Implementación en memoria (I-02), indexada por el código de la reserva.
 * Usa ConcurrentHashMap porque el vencimiento automático (RN-21) corre en otro hilo (scheduler).
 */
public class ReservaRepositoryInMemory implements ReservaRepository {

    private static final Comparator<Reserva> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(Reserva::getFechaCreacion).reversed();

    private final Map<CodigoReserva, Reserva> almacen = new ConcurrentHashMap<>();

    @Override
    public synchronized Reserva guardar(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva a guardar es obligatoria");
        }
        if (reserva.getIdentificadorCanalExterno() != null) {
            buscarPorCanalEIdentificadorExterno(reserva.getCanalOrigen(), reserva.getIdentificadorCanalExterno())
                    .filter(existente -> !existente.equals(reserva))
                    .ifPresent(existente -> {
                        throw new ReglaDominioException("RN-19: ya existe la reserva " + existente.getCodigo().valor()
                                + " para " + reserva.getCanalOrigen() + " / " + reserva.getIdentificadorCanalExterno());
                    });
        }
        almacen.put(reserva.getCodigo(), reserva);
        return reserva;
    }

    @Override
    public Optional<Reserva> buscarPorCodigo(CodigoReserva codigo) {
        return Optional.ofNullable(codigo).map(almacen::get);
    }

    @Override
    public Optional<Reserva> buscarPorCanalEIdentificadorExterno(CanalOrigen canal, String identificadorExterno) {
        return almacen.values().stream()
                .filter(r -> r.correspondeA(canal, identificadorExterno))
                .findFirst();
    }

    @Override
    public List<Reserva> listarActivasQueSeSolapan(IdentificacionApartamento apartamento, Estancia estancia) {
        return delApartamento(apartamento)
                .filter(r -> r.ocupaNochesDe(estancia))
                .toList();
    }

    @Override
    public List<Reserva> listarActivasPorApartamento(IdentificacionApartamento apartamento) {
        return delApartamento(apartamento)
                .filter(Reserva::estaActiva)
                .sorted(Comparator.comparing(r -> r.getEstancia().fechaEntrada()))
                .toList();
    }

    @Override
    public List<Reserva> listarActivasOFuturasPorApartamento(IdentificacionApartamento apartamento, LocalDate desde) {
        return delApartamento(apartamento)
                .filter(Reserva::estaActiva)
                .filter(r -> !r.getEstancia().fechaSalida().isBefore(desde))
                .toList();
    }

    @Override
    public List<Reserva> listarPorEstado(EstadoReserva estado) {
        return almacen.values().stream()
                .filter(r -> r.getEstado() == estado)
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }

    @Override
    public List<Reserva> listarLlegadasPrevistas(LocalDate fecha) {
        return almacen.values().stream()
                .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA)
                .filter(r -> r.getEstancia().fechaEntrada().equals(fecha))
                .toList();
    }

    @Override
    public List<Reserva> listarSalidasPrevistas(LocalDate fecha) {
        return almacen.values().stream()
                .filter(r -> r.getEstado() == EstadoReserva.EN_CURSO)
                .filter(r -> r.getEstancia().fechaSalida().equals(fecha))
                .toList();
    }

    @Override
    public List<Reserva> listarTodas() {
        return almacen.values().stream().sorted(MAS_RECIENTE_PRIMERO).toList();
    }

    private Stream<Reserva> delApartamento(IdentificacionApartamento apartamento) {
        return almacen.values().stream().filter(r -> r.esDelApartamento(apartamento));
    }
}
