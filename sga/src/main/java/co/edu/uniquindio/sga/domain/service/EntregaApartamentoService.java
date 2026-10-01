package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Registro de llegada (7.6): la reserva pasa a EN_CURSO y el apartamento a OCUPADO.
 *
 * Reglas: RN-10 (reserva CONFIRMADA y no antes de la fecha de entrada) y RN-11 (apartamento PREPARADO).
 */
public class EntregaApartamentoService {

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;

    public EntregaApartamentoService(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
    }

    public void autorizarEntrega(CodigoReserva codigo, LocalDate fechaActual) {
        Reserva reserva = buscarReserva(codigo);
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(reserva.getApartamento())
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento "
                        + reserva.getApartamento().codigo()));

        // Reserva valida RN-10 y RN-11 antes de cambiar; así el apartamento no queda OCUPADO si la reserva falla.
        reserva.registrarLlegada(fechaActual, apartamento.getEstadoOperativo());
        apartamento.recibirGrupo();

        reservaRepository.guardar(reserva);
        apartamentoRepository.guardar(apartamento);
    }

    private Reserva buscarReserva(CodigoReserva codigo) {
        if (codigo == null) throw new ReglaDominioException("El código de la reserva es obligatorio");
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaDominioException("No existe la reserva " + codigo.valor()));
    }
}
