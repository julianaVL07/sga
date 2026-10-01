package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.service.VencimientoReservasService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * CU-14. Sin endpoint: lo dispara una tarea programada (@Scheduled en infraestructura), sin intervención manual.
 * Cancela las reservas PENDIENTES que superaron el plazo de confirmación (12 horas en Coral) y libera sus
 * noches (RN-21, RN-12). Actor: sistema.
 */
public class VencerReservaPendienteUseCase {

    private final VencimientoReservasService vencimientoService;
    private final Clock reloj;

    public VencerReservaPendienteUseCase(VencimientoReservasService vencimientoService, Clock reloj) {
        this.vencimientoService = Objects.requireNonNull(vencimientoService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** @return cuántas reservas vencieron en esta ejecución. */
    public int ejecutar() {
        return vencimientoService.cancelarReservasExpiradas(LocalDateTime.now(reloj));
    }
}
