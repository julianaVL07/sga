package co.edu.uniquindio.sga.infrastructure.notificaciones;

import co.edu.uniquindio.sga.application.event.CodigoRecuperacionGeneradoEvento;
import co.edu.uniquindio.sga.application.event.ReservaCanceladaEvento;
import co.edu.uniquindio.sga.application.event.ReservaConfirmadaEvento;
import co.edu.uniquindio.sga.application.event.ReservaCreadaEvento;
import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

/**
 * Adaptador de entrada de las notificaciones (10.3.2, 7.8): escucha los eventos de los casos de uso y llama al
 * puerto NotificadorTitular DESPUÉS de que la transacción termina bien (AFTER_COMMIT) y en otro hilo (@Async).
 * Así un correo lento o caído nunca deshace la reserva ni le devuelve un error al usuario: el fallo se registra en
 * el log y ya. Si la transacción se deshace, no se envía nada.
 */
@Component
public class NotificacionesListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesListener.class);

    private final NotificadorTitular notificador;

    public NotificacionesListener(NotificadorTitular notificador) {
        this.notificador = Objects.requireNonNull(notificador);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearReserva(ReservaCreadaEvento evento) {
        notificar("reserva creada", evento.reserva().getCodigo().valor(),
                () -> notificador.reservaCreada(evento.reserva()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarReserva(ReservaConfirmadaEvento evento) {
        notificar("reserva confirmada", evento.reserva().getCodigo().valor(),
                () -> notificador.reservaConfirmada(evento.reserva()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelarReserva(ReservaCanceladaEvento evento) {
        notificar("reserva cancelada", evento.reserva().getCodigo().valor(),
                () -> notificador.reservaCancelada(evento.reserva(), evento.retencion()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alGenerarCodigoRecuperacion(CodigoRecuperacionGeneradoEvento evento) {
        // El código no va al log
        notificar("código de recuperación", "de una cuenta",
                () -> notificador.codigoRecuperacion(evento.correo(), evento.nombre(), evento.codigo(),
                        evento.minutosVigencia()));
    }

    private static void notificar(String motivo, String referencia, Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.warn("Notificaciones: no se pudo enviar '{}' ({}); la operación ya quedó guardada. {}: {}",
                    motivo, referencia, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
