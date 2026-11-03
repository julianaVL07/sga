package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoActualizacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Administración de apartamentos (7.3) y de sus tarifas (7.4): CU-22 a CU-25.
 * Coordina el agregado Apartamento con el Alojamiento (temporadas, L-03, L-04) y con las reservas (advertencia de
 * capacidad de 7.3). Las reglas propias del apartamento las aplica el agregado; este servicio solo le entrega lo
 * que no conoce.
 */
public class AdministracionApartamentoService {

    private final ApartamentoRepository apartamentoRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final ReservaRepository reservaRepository;

    public AdministracionApartamentoService(ApartamentoRepository apartamentoRepository,
                                            AlojamientoRepository alojamientoRepository,
                                            ReservaRepository reservaRepository) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
    }

    /**
     * CU-22: registra un apartamento recién creado con Apartamento.crear (nace inactivo, 7.4) y, si se indican,
     * sus tarifas por temporada desde hoy. L-04: la identificación es única, aunque el otro esté retirado de la
     * venta; L-03: el alojamiento admite un máximo de apartamentos.
     */
    public Apartamento registrar(Apartamento nuevo, List<Tarifa> tarifas, LocalDate hoy) {
        if (nuevo == null) throw new ReglaDominioException("El apartamento es obligatorio");
        requerirFecha(hoy);
        if (apartamentoRepository.existe(nuevo.getIdentificacion())) {
            throw new ReglaDominioException("L-04: Ya existe un apartamento con la identificación "
                    + nuevo.getIdentificacion().codigo());
        }
        Alojamiento alojamiento = obtenerAlojamiento();
        if (tarifas != null && !tarifas.isEmpty()) {
            nuevo.definirTarifas(tarifas, alojamiento.getTemporadas(), hoy);
        }
        alojamiento.agregarApartamento(nuevo);
        Apartamento guardado = apartamentoRepository.guardar(nuevo);
        alojamientoRepository.guardar(alojamiento);
        return guardado;
    }

    /**
     * CU-23 (7.3): actualiza datos descriptivos y capacidad. Las reservas ya creadas no cambian; el resultado
     * lista las activas o futuras cuyo grupo supera la nueva capacidad para que el administrador las revise.
     */
    public ResultadoActualizacionApartamento actualizar(IdentificacionApartamento aptoId, String nombre,
                                                        String descripcion, int dormitorios, Dotacion dotacion,
                                                        Capacidad nuevaCapacidad, LocalDate hoy) {
        requerirFecha(hoy);
        Apartamento apartamento = buscarApartamento(aptoId);
        apartamento.actualizarDatos(nombre, descripcion, dormitorios, dotacion);
        apartamento.cambiarCapacidad(nuevaCapacidad);
        List<CodigoReserva> afectadas = reservasQueExcedenCapacidad(aptoId, nuevaCapacidad, hoy);
        return new ResultadoActualizacionApartamento(apartamentoRepository.guardar(apartamento), afectadas);
    }

    /** 7.3: reservas activas o futuras del apartamento cuyo total de ocupantes supera la capacidad dada. */
    public List<CodigoReserva> reservasQueExcedenCapacidad(IdentificacionApartamento aptoId, Capacidad capacidad,
                                                           LocalDate hoy) {
        if (aptoId == null) throw new ReglaDominioException("El apartamento es obligatorio");
        if (capacidad == null) throw new ReglaDominioException("La capacidad es obligatoria");
        requerirFecha(hoy);
        return reservaRepository.listarActivasOFuturasPorApartamento(aptoId, hoy).stream()
                .filter(reserva -> capacidad.excede(reserva.totalOcupantes()))
                .map(Reserva::getCodigo)
                .toList();
    }

    /** CU-24 (7.4): define una o varias tarifas desde hoy; las anteriores quedan en el histórico. */
    public Apartamento definirTarifas(IdentificacionApartamento aptoId, List<Tarifa> tarifas, LocalDate hoy) {
        requerirFecha(hoy);
        Apartamento apartamento = buscarApartamento(aptoId);
        apartamento.definirTarifas(tarifas, obtenerAlojamiento().getTemporadas(), hoy);
        return apartamentoRepository.guardar(apartamento);
    }

    /** CU-25 (7.4): activa el apartamento solo si tiene tarifa en todas las temporadas del alojamiento. */
    public Apartamento activar(IdentificacionApartamento aptoId) {
        Apartamento apartamento = buscarApartamento(aptoId);
        apartamento.activar(obtenerAlojamiento().getTemporadas());
        return apartamentoRepository.guardar(apartamento);
    }

    private Apartamento buscarApartamento(IdentificacionApartamento aptoId) {
        if (aptoId == null) throw new ReglaDominioException("El apartamento es obligatorio");
        return apartamentoRepository.buscarPorIdentificacion(aptoId)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + aptoId.codigo()));
    }

    private Alojamiento obtenerAlojamiento() {
        return alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
    }

    private static void requerirFecha(LocalDate hoy) {
        if (hoy == null) throw new ReglaDominioException("La fecha de referencia es obligatoria");
    }
}
