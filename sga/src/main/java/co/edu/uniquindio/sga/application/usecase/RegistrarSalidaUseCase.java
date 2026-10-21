package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.LiquidacionSalidaService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-05. PUT /api/reservas/{codigo}/registrar-salida
 * Salida del grupo: la reserva pasa a FINALIZADA (RN-12, libera sus noches) y el apartamento a
 * PENDIENTE_PREPARACION. Incluye CU-06: el folio debe quedar cerrado (RN-17). Si el saldo es cero se cierra
 * aquí; si no, primero hay que pagar o el administrador debe autorizar el cierre (CU-07).
 * Actor: recepcionista.
 */
public class RegistrarSalidaUseCase {

    private final ReservaRepository reservaRepository;
    private final LiquidacionSalidaService liquidacionSalidaService;
    private final Clock reloj;

    public RegistrarSalidaUseCase(ReservaRepository reservaRepository, LiquidacionSalidaService liquidacionSalidaService,
                                  Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.liquidacionSalidaService = Objects.requireNonNull(liquidacionSalidaService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public void ejecutar(CodigoReserva codigo) {
        verificarQueExiste(codigo);
        liquidacionSalidaService.registrarSalida(codigo, LocalDate.now(reloj));
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
