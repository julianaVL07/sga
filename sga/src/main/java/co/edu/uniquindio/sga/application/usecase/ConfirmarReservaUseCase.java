package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * CU-10. PUT /api/reservas/{codigo}/confirmar
 * PENDIENTE → CONFIRMADA. Exige hora estimada de llegada (RN-09), que no haya vencido el plazo (RN-21) y,
 * como el alojamiento Coral exige anticipo del 50 %, que los pagos del folio lo cubran (L-11).
 * Actores: recepcionista o administrador.
 */
public class ConfirmarReservaUseCase {

    private final ReservaRepository reservaRepository;
    private final ConfirmacionReservaService confirmacionService;
    private final Clock reloj;

    public ConfirmarReservaUseCase(ReservaRepository reservaRepository, ConfirmacionReservaService confirmacionService,
                                   Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.confirmacionService = Objects.requireNonNull(confirmacionService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /**
     * @param horaEstimadaLlegada opcional: si la reserva aún no la tiene (por ejemplo, llegó por el canal externo),
     *                            se registra antes de confirmar (RN-09).
     */
    public Reserva ejecutar(CodigoReserva codigo, LocalTime horaEstimadaLlegada) {
        verificarQueExiste(codigo);
        if (horaEstimadaLlegada != null) {
            Reserva reserva = reservaRepository.buscarPorCodigo(codigo).orElseThrow();
            reserva.registrarHoraEstimadaLlegada(horaEstimadaLlegada);
            reservaRepository.guardar(reserva);
        }
        confirmacionService.confirmar(codigo, LocalDateTime.now(reloj));
        return reservaRepository.buscarPorCodigo(codigo).orElseThrow();
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
