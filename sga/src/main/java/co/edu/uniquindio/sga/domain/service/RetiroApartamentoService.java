package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-21: retira un apartamento de la venta (7.3). El agregado no conoce las reservas, por eso este servicio
 * las consulta y le entrega la respuesta a Apartamento.retirarDeLaVenta, que es quien aplica la regla.
 * La eliminación es lógica (12.4): el apartamento se guarda marcado como eliminado, nunca se borra.
 */
public class RetiroApartamentoService {

    private final ApartamentoRepository apartamentoRepository;
    private final ReservaRepository reservaRepository;

    public RetiroApartamentoService(ApartamentoRepository apartamentoRepository, ReservaRepository reservaRepository) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
    }

    /** @param hoy fecha desde la que una reserva cuenta como activa o futura. */
    public Apartamento retirar(IdentificacionApartamento aptoId, LocalDate hoy) {
        if (aptoId == null) throw new ReglaDominioException("El apartamento es obligatorio");
        if (hoy == null) throw new ReglaDominioException("La fecha de referencia es obligatoria");
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(aptoId)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + aptoId.codigo()));

        boolean tieneReservasActivasOFuturas =
                !reservaRepository.listarActivasOFuturasPorApartamento(aptoId, hoy).isEmpty();
        apartamento.retirarDeLaVenta(tieneReservasActivasOFuturas);
        return apartamentoRepository.guardar(apartamento);
    }
}
