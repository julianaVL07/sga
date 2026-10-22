package co.edu.uniquindio.sga.infrastructure.scheduler;

import co.edu.uniquindio.sga.application.usecase.VencerReservaPendienteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de CU-14: no hay endpoint, lo dispara el reloj del SGA sin intervención manual.
 * Cada cierto tiempo cancela las reservas PENDIENTES que superaron el plazo de confirmación (12 horas en Coral)
 * y libera sus noches (RN-21). El intervalo se configura en application.properties (15 minutos por defecto).
 * Requiere @EnableScheduling en SgaApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientoReservasScheduler {

    private final VencerReservaPendienteUseCase vencerReservaPendienteUseCase;

    @Scheduled(fixedDelayString = "${sga.tareas.vencimiento-pendientes.intervalo:PT15M}",
            initialDelayString = "${sga.tareas.vencimiento-pendientes.retraso-inicial:PT1M}")
    public void vencerReservasPendientes() {
        int vencidas = vencerReservaPendienteUseCase.ejecutar();
        if (vencidas > 0) {
            log.info("CU-14: {} reserva(s) pendiente(s) vencida(s) por plazo de confirmación (RN-21)", vencidas);
        }
    }
}