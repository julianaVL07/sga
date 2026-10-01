package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Responde si un apartamento puede venderse para una estancia (3.3). Es el único lugar donde se cruzan
 * las reservas de todos los canales, porque ninguna entidad por sí sola conoce las demás reservas.
 *
 * Reglas: RN-01 (sin reservas activas solapadas, sin importar el canal), RN-02 (capacidad),
 * RN-07 (bloqueos), RN-20 (tiempo de preparación entre una salida y una entrada el mismo día),
 * RP-01 (estancia mínima de fin de semana).
 */
public class DisponibilidadApartamentoService {

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final BloqueoRepository bloqueoRepository;
    private final AlojamientoRepository alojamientoRepository;

    public DisponibilidadApartamentoService(ReservaRepository reservaRepository,
                                            ApartamentoRepository apartamentoRepository,
                                            BloqueoRepository bloqueoRepository,
                                            AlojamientoRepository alojamientoRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.bloqueoRepository = Objects.requireNonNull(bloqueoRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
    }

    /** F-06: apartamentos vendibles para la estancia y el tamaño del grupo. */
    public List<Apartamento> buscarDisponibles(Estancia estancia, int totalOcupantes) {
        requerido(estancia, "La estancia es obligatoria");
        obtenerAlojamiento().validarEstanciaMinima(estancia);
        return apartamentoRepository.listarVigentesParaVenta().stream()
                .filter(apartamento -> estaDisponible(apartamento, estancia, totalOcupantes, null))
                .toList();
    }

    /**
     * @param reservaExcluida reserva que no se cuenta como ocupación (la que se está modificando); puede ser null.
     */
    public boolean estaDisponible(Apartamento apartamento, Estancia estancia, int totalOcupantes,
                                  CodigoReserva reservaExcluida) {
        requerido(apartamento, "El apartamento es obligatorio");
        requerido(estancia, "La estancia es obligatoria");
        return apartamento.estaVigenteParaVenta()
                && apartamento.admite(totalOcupantes)
                && conflictoDeInventario(apartamento, estancia, reservaExcluida).isEmpty();
    }

    /**
     * Igual que {@link #estaDisponible} pero lanza la excepción con la regla que falla.
     * Lo usan CreacionReservaService y ModificacionReservaService antes de tocar la reserva.
     */
    public void validarDisponibilidad(Apartamento apartamento, Estancia estancia, int totalOcupantes,
                                      CodigoReserva reservaExcluida) {
        requerido(apartamento, "El apartamento es obligatorio");
        requerido(estancia, "La estancia es obligatoria");
        if (!apartamento.estaVigenteParaVenta()) {
            throw new ReglaDominioException("3.3: El apartamento " + apartamento.getIdentificacion().codigo()
                    + " no está activo para la venta");
        }
        apartamento.validarCapacidadPara(totalOcupantes);
        obtenerAlojamiento().validarEstanciaMinima(estancia);
        conflictoDeInventario(apartamento, estancia, reservaExcluida).ifPresent(motivo -> {
            throw new ReglaDominioException(motivo);
        });
    }

    /**
     * Motivo por el que las noches ya no están libres (RN-07, RN-01 o RN-20), o vacío si están libres.
     * ConciliacionCanalExternoService lo usa para distinguir un conflicto de inventario (RN-18)
     * de un mensaje con datos inválidos.
     */
    public Optional<String> conflictoDeInventario(Apartamento apartamento, Estancia estancia,
                                                  CodigoReserva reservaExcluida) {
        String codigo = apartamento.getIdentificacion().codigo();

        boolean bloqueado = apartamento.estaBloqueadoDurante(estancia)
                || !bloqueoRepository.listarVigentesQueSeSolapan(apartamento.getIdentificacion(), estancia).isEmpty();
        if (bloqueado) {
            return Optional.of("RN-07: El apartamento " + codigo + " tiene un bloqueo en esas noches");
        }

        List<Reserva> otrasActivas = reservaRepository.listarActivasPorApartamento(apartamento.getIdentificacion())
                .stream()
                .filter(reserva -> !reserva.getCodigo().equals(reservaExcluida))
                .toList();

        Optional<Reserva> solapada = otrasActivas.stream().filter(r -> r.ocupaNochesDe(estancia)).findFirst();
        if (solapada.isPresent()) {
            return Optional.of("RN-01: El apartamento " + codigo + " ya tiene la reserva "
                    + solapada.get().getCodigo().valor() + " en esas noches");
        }

        if (!obtenerAlojamiento().permiteEntradaElMismoDiaDeUnaSalida()) {
            boolean pegadaAOtra = otrasActivas.stream().anyMatch(r ->
                    r.getEstancia().fechaSalida().equals(estancia.fechaEntrada())
                            || r.getEstancia().fechaEntrada().equals(estancia.fechaSalida()));
            if (pegadaAOtra) {
                return Optional.of("RN-20: El apartamento " + codigo + " no alcanza a prepararse entre una salida "
                        + "y una entrada el mismo día");
            }
        }
        return Optional.empty();
    }

    private Alojamiento obtenerAlojamiento() {
        return alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
    }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }
}
