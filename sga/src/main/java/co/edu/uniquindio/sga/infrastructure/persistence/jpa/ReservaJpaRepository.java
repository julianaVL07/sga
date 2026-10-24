package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA del puerto ReservaRepository (Guía 09, 6.5). Los casos de uso no saben que existe: llaman al
 * puerto, este convierte con el mapper, delega en Spring Data y vuelve a convertir.
 * @Transactional en la clase: dentro de un caso de uso se une a su transacción; fuera (datos iniciales,
 * tarea programada) abre la suya y así las listas LAZY se leen sin LazyInitializationException.
 */
@Transactional
public class ReservaJpaRepository implements ReservaRepository {

    /** La regla de qué estados retienen noches sigue en el dominio (RN-12). */
    private static final List<EstadoReserva> ACTIVOS = Arrays.stream(EstadoReserva.values())
            .filter(EstadoReserva::retieneDisponibilidad)
            .toList();

    private final ReservaJpaDataRepository dataRepository;
    private final ReservaPersistenceMapper mapper;

    public ReservaJpaRepository(ReservaJpaDataRepository dataRepository, ReservaPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Reserva guardar(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva a guardar es obligatoria");
        }
        if (reserva.getIdentificadorCanalExterno() != null) {
            dataRepository.findByCanalOrigenAndIdentificadorCanalExterno(reserva.getCanalOrigen(),
                            reserva.getIdentificadorCanalExterno())
                    .filter(existente -> !existente.getCodigo().equals(reserva.getCodigo().valor()))
                    .ifPresent(existente -> {
                        throw new ReglaDominioException("RN-19: ya existe la reserva " + existente.getCodigo()
                                + " para " + reserva.getCanalOrigen() + " / " + reserva.getIdentificadorCanalExterno());
                    });
        }
        ReservaEntity entidad = mapper.aEntidad(reserva);
        dataRepository.findByCodigo(reserva.getCodigo().valor())
                .ifPresent(existente -> entidad.setId(existente.getId()));   // actualizar, no insertar otra fila
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorCodigo(CodigoReserva codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return dataRepository.findByCodigo(codigo.valor()).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorCanalEIdentificadorExterno(CanalOrigen canal, String identificadorExterno) {
        if (canal == null || identificadorExterno == null) {
            return Optional.empty();
        }
        return dataRepository.findByCanalOrigenAndIdentificadorCanalExterno(canal, identificadorExterno.trim())
                .map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarActivasQueSeSolapan(IdentificacionApartamento apartamento, Estancia estancia) {
        return aDominio(dataRepository.buscarQueSeSolapan(apartamento.codigo(), ACTIVOS,
                estancia.fechaEntrada(), estancia.fechaSalida()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarActivasPorApartamento(IdentificacionApartamento apartamento) {
        return aDominio(dataRepository.findByApartamentoIdAndEstadoInOrderByEstanciaFechaEntradaAsc(
                apartamento.codigo(), ACTIVOS));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarActivasOFuturasPorApartamento(IdentificacionApartamento apartamento, LocalDate desde) {
        return aDominio(dataRepository.findByApartamentoIdAndEstadoInAndEstanciaFechaSalidaGreaterThanEqual(
                apartamento.codigo(), ACTIVOS, desde));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarPorEstado(EstadoReserva estado) {
        return aDominio(dataRepository.findByEstadoOrderByFechaCreacionDesc(estado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarLlegadasPrevistas(LocalDate fecha) {
        return aDominio(dataRepository.findByEstadoAndEstanciaFechaEntrada(EstadoReserva.CONFIRMADA, fecha));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarSalidasPrevistas(LocalDate fecha) {
        return aDominio(dataRepository.findByEstadoAndEstanciaFechaSalida(EstadoReserva.EN_CURSO, fecha));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {
        return aDominio(dataRepository.findAllByOrderByFechaCreacionDesc());
    }

    private List<Reserva> aDominio(List<ReservaEntity> entidades) {
        return entidades.stream().map(mapper::aDominio).toList();
    }
}
