package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos.IngresoPorMedio;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion.OcupacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal.ReservasDeCanal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Reportes del administrador (7.9): ocupación, ingresos y reservas por canal de un periodo.
 * El periodo va de desde a hasta, ambas fechas incluidas; en la ocupación cada fecha es una noche (3.1).
 * Solo lee: los datos llegan por los puertos de reservas, folios y apartamentos.
 *
 * Decisiones del equipo:
 * - Una noche está vendida si pertenece a una reserva CONFIRMADA, EN_CURSO o FINALIZADA. Las PENDIENTE aún no
 *   están confirmadas y las CANCELADA y NO_SHOW liberaron sus noches (RN-12).
 * - Los apartamentos activos son los que hoy están a la venta (ApartamentoRepository.listarVigentesParaVenta).
 * - Las reservas por canal se cuentan por fecha de entrada, igual que el filtro del listado de reservas (7.5).
 */
public class ReportesService {

    /** Estados cuyas noches cuentan como vendidas. */
    private static final Set<EstadoReserva> VENDEN_NOCHES =
            Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final ApartamentoRepository apartamentos;

    public ReportesService(ReservaRepository reservas, FolioRepository folios, ApartamentoRepository apartamentos) {
        this.reservas = Objects.requireNonNull(reservas);
        this.folios = Objects.requireNonNull(folios);
        this.apartamentos = Objects.requireNonNull(apartamentos);
    }

    /** 7.9: noches vendidas y porcentaje = noches vendidas / (apartamentos activos × noches del periodo). */
    public ReporteOcupacion ocupacion(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDate finExcluido = hasta.plusDays(1);
        int nochesPeriodo = (int) ChronoUnit.DAYS.between(desde, finExcluido);

        Set<IdentificacionApartamento> activos = apartamentos.listarVigentesParaVenta().stream()
                .map(Apartamento::getIdentificacion)
                .collect(Collectors.toSet());

        Map<String, Integer> vendidasPorApartamento = new TreeMap<>();
        activos.forEach(a -> vendidasPorApartamento.put(a.codigo(), 0));
        for (Reserva reserva : reservas.listarTodas()) {
            if (!VENDEN_NOCHES.contains(reserva.getEstado())) {
                continue;
            }
            int noches = nochesDentroDelPeriodo(reserva.getEstancia(), desde, finExcluido);
            if (noches > 0) {
                vendidasPorApartamento.merge(reserva.getApartamento().codigo(), noches, Integer::sum);
            }
        }

        List<OcupacionApartamento> detalle = new ArrayList<>();
        int nochesVendidas = 0;
        for (Map.Entry<String, Integer> fila : vendidasPorApartamento.entrySet()) {
            IdentificacionApartamento id = new IdentificacionApartamento(fila.getKey());
            int vendidas = fila.getValue();
            nochesVendidas += vendidas;
            detalle.add(new OcupacionApartamento(id, vendidas, porcentaje(vendidas, nochesPeriodo),
                    activos.contains(id)));
        }
        int nochesDisponibles = activos.size() * nochesPeriodo;
        return new ReporteOcupacion(desde, hasta, nochesPeriodo, activos.size(), nochesDisponibles, nochesVendidas,
                porcentaje(nochesVendidas, nochesDisponibles), detalle);
    }

    /** 7.9: suma de los pagos del periodo por medio de pago. Los cargos no cuentan. */
    public ReporteIngresos ingresos(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        Map<MedioPago, Dinero> totales = new EnumMap<>(MedioPago.class);
        Map<MedioPago, Integer> cantidades = new EnumMap<>(MedioPago.class);
        for (MedioPago medio : MedioPago.values()) {
            totales.put(medio, Dinero.cero());
            cantidades.put(medio, 0);
        }

        for (Folio folio : folios.listarConPagosEntre(desde, hasta)) {
            for (Pago pago : folio.getPagos()) {
                if (dentroDelPeriodo(pago.fecha(), desde, hasta)) {
                    totales.merge(pago.medio(), pago.valor(), Dinero::sumar);
                    cantidades.merge(pago.medio(), 1, Integer::sum);
                }
            }
        }

        Dinero total = totales.values().stream().reduce(Dinero.cero(), Dinero::sumar);
        int cantidadPagos = cantidades.values().stream().mapToInt(Integer::intValue).sum();
        List<IngresoPorMedio> medios = new ArrayList<>();
        for (MedioPago medio : MedioPago.values()) {
            Dinero delMedio = totales.get(medio);
            medios.add(new IngresoPorMedio(medio, cantidades.get(medio), delMedio,
                    porcentaje(delMedio.monto(), total.monto())));
        }
        return new ReporteIngresos(desde, hasta, total, cantidadPagos, medios);
    }

    /** 7.9: reservas por canal de origen con fecha de entrada en el periodo, incluidas canceladas y no-show. */
    public ReporteReservasPorCanal reservasPorCanal(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        Map<CanalOrigen, List<Reserva>> porCanal = new EnumMap<>(CanalOrigen.class);
        for (CanalOrigen canal : CanalOrigen.values()) {
            porCanal.put(canal, new ArrayList<>());
        }
        reservas.listarTodas().stream()
                .filter(r -> dentroDelPeriodo(r.getEstancia().fechaEntrada(), desde, hasta))
                .forEach(r -> porCanal.get(r.getCanalOrigen()).add(r));

        int total = porCanal.values().stream().mapToInt(List::size).sum();
        List<ReservasDeCanal> canales = porCanal.entrySet().stream()           // EnumMap: orden del enum
                .map(e -> {
                    List<Reserva> delCanal = e.getValue();
                    return new ReservasDeCanal(e.getKey(), delCanal.size(),
                            contar(delCanal, EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO),
                            contar(delCanal, EstadoReserva.FINALIZADA),
                            contar(delCanal, EstadoReserva.CANCELADA),
                            contar(delCanal, EstadoReserva.NO_SHOW),
                            porcentaje(delCanal.size(), total));
                })
                .toList();
        return new ReporteReservasPorCanal(desde, hasta, total, canales);
    }

    // ---------------------------------------------------------------- Apoyo

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaDominioException("7.9: el reporte necesita la fecha desde y la fecha hasta");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaDominioException("7.9: la fecha desde (" + desde + ") no puede ser posterior a la fecha "
                    + "hasta (" + hasta + ")");
        }
    }

    /** Noches de la estancia [entrada, salida) que caen en el periodo [desde, finExcluido). */
    static int nochesDentroDelPeriodo(Estancia estancia, LocalDate desde, LocalDate finExcluido) {
        LocalDate inicio = estancia.fechaEntrada().isAfter(desde) ? estancia.fechaEntrada() : desde;
        LocalDate fin = estancia.fechaSalida().isBefore(finExcluido) ? estancia.fechaSalida() : finExcluido;
        return inicio.isBefore(fin) ? (int) ChronoUnit.DAYS.between(inicio, fin) : 0;
    }

    private static boolean dentroDelPeriodo(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private static int contar(List<Reserva> reservas, EstadoReserva... estados) {
        Set<EstadoReserva> buscados = Set.of(estados);
        return (int) reservas.stream().filter(r -> buscados.contains(r.getEstado())).count();
    }

    private static BigDecimal porcentaje(long parte, long total) {
        return porcentaje(BigDecimal.valueOf(parte), BigDecimal.valueOf(total));
    }

    /** parte / total × 100 con dos decimales; 0 si el total es cero. */
    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }
}
