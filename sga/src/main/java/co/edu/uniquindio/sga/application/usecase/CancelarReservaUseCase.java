package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CancelarReservaService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-09. PUT /api/reservas/{codigo}/cancelar
 * Cancela una reserva antes del registro, calcula la retención con la política congelada (RN-13), la asienta
 * en el folio (RN-16) y libera las noches de inmediato (RN-12).
 * Actores: titular (huésped, solo sus reservas), recepcionista o administrador.
 *
 * @return la retención; lo pagado por encima de ella queda como saldo a favor para devolver fuera del sistema.
 */
public class CancelarReservaUseCase {

    private final ReservaRepository reservaRepository;
    private final CancelarReservaService cancelarReservaService;
    private final Clock reloj;

    public CancelarReservaUseCase(ReservaRepository reservaRepository, CancelarReservaService cancelarReservaService,
                                  Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.cancelarReservaService = Objects.requireNonNull(cancelarReservaService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Dinero ejecutar(CodigoReserva codigo, String motivo) {
        verificarQueExiste(codigo);
        return cancelarReservaService.cancelar(codigo, LocalDate.now(reloj), motivo);
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
