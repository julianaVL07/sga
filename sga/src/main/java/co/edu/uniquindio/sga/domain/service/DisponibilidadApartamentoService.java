package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.CriterioBusqueda;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;

import java.time.LocalDate;
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
    private final CotizadorEstanciaService cotizador;

    public DisponibilidadApartamentoService(ReservaRepository reservaRepository,
                                            ApartamentoRepository apartamentoRepository,
                                            BloqueoRepository bloqueoRepository,
                                            AlojamientoRepository alojamientoRepository,
                                            CotizadorEstanciaService cotizador) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.bloqueoRepository = Objects.requireNonNull(bloqueoRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.cotizador = Objects.requireNonNull(cotizador);
    }

    /** Arma su propio cotizador con los mismos repositorios; lo usan las pruebas que no lo necesitan aparte. */
    public DisponibilidadApartamentoService(ReservaRepository reservaRepository,
                                            ApartamentoRepository apartamentoRepository,
                                            BloqueoRepository bloqueoRepository,
                                            AlojamientoRepository alojamientoRepository) {
        this(reservaRepository, apartamentoRepository, bloqueoRepository, alojamientoRepository,
                new CotizadorEstanciaService(apartamentoRepository, alojamientoRepository));
    }

    /** F-06: apartamentos vendibles para la estancia y el tamaño del grupo. Lo usa también el canal externo. */
    public List<Apartamento> buscarDisponibles(Estancia estancia, int totalOcupantes) {
        requerido(estancia, "La estancia es obligatoria");
        obtenerAlojamiento().validarEstanciaMinima(estancia);
        return apartamentoRepository.listarVigentesParaVenta().stream()
                .filter(apartamento -> estaDisponible(apartamento, estancia, totalOcupantes, null))
                .toList();
    }

    /**
     * CU-01 con filtros (7.5). Primero decide la disponibilidad igual que {@link #buscarDisponibles(Estancia, int)}
     * (RN-01, RN-02, RN-07, RN-20, RP-01); sobre ese resultado aplica los filtros del criterio: capacidad mínima,
     * característica de la dotación (sin distinguir mayúsculas) y rango de precio del valor estimado de la estancia.
     * El valor estimado lo calcula CotizadorEstanciaService (RN-05, RN-06); si no llegan fechas de nacimiento,
     * todos los ocupantes cuentan como facturables (ver CotizadorEstanciaService.estimar).
     *
     * @param fechasNacimiento opcional (null o vacía); si trae datos debe haber una por ocupante.
     * @param criterio         opcional; null equivale a {@link CriterioBusqueda#sinFiltros()}.
     * @return disponibles ordenados por identificación, con su valor estimado.
     */
    public List<ApartamentoDisponible> buscarDisponibles(Estancia estancia, int totalOcupantes,
                                                         List<LocalDate> fechasNacimiento,
                                                         CriterioBusqueda criterio) {
        CriterioBusqueda filtros = criterio == null ? CriterioBusqueda.sinFiltros() : criterio;
        Coordenada ubicacion = obtenerAlojamiento().getUbicacion();
        return buscarDisponibles(estancia, totalOcupantes).stream()
                .filter(apartamento -> filtros.cumpleCapacidad(apartamento.getCapacidad()))
                .filter(apartamento -> filtros.cumpleCaracteristica(apartamento.getDotacion()))
                .map(apartamento -> new ApartamentoDisponible(apartamento,
                        cotizador.estimar(apartamento, estancia, totalOcupantes, fechasNacimiento).valorTotal(),
                        ubicacion))
                .filter(disponible -> filtros.cumplePrecio(disponible.valorEstimado()))
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
