package co.edu.uniquindio.sga.infrastructure.notificaciones;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Parte común de los adaptadores del puerto NotificadorTitular: arma el mensaje con PlantillasCorreo y, si hay a
 * quién enviarlo, se lo pasa al adaptador concreto. Sin correo no se envía nada y se registra en el log (7.8).
 */
public abstract class NotificadorTitularConPlantillas implements NotificadorTitular {

    private static final Logger log = LoggerFactory.getLogger(NotificadorTitularConPlantillas.class);

    private final PlantillasCorreo plantillas;

    protected NotificadorTitularConPlantillas(PlantillasCorreo plantillas) {
        this.plantillas = Objects.requireNonNull(plantillas);
    }

    /** Envía el mensaje. Si falla puede lanzar una excepción: quien llama la registra y sigue. */
    protected abstract void enviar(MensajeCorreo mensaje);

    @Override
    public void reservaCreada(Reserva reserva) {
        enviarSiHayCorreo(plantillas.reservaCreada(reserva));
    }

    @Override
    public void reservaConfirmada(Reserva reserva) {
        enviarSiHayCorreo(plantillas.reservaConfirmada(reserva));
    }

    @Override
    public void reservaCancelada(Reserva reserva, Dinero retencion) {
        enviarSiHayCorreo(plantillas.reservaCancelada(reserva, retencion));
    }

    @Override
    public void recordatorio(Reserva reserva, MotivoRecordatorio motivo) {
        enviarSiHayCorreo(plantillas.recordatorio(reserva, motivo));
    }

    @Override
    public void codigoRecuperacion(String correo, String nombre, String codigo, long minutosVigencia) {
        enviarSiHayCorreo(plantillas.codigoRecuperacion(correo, nombre, codigo, minutosVigencia));
    }

    private void enviarSiHayCorreo(MensajeCorreo mensaje) {
        if (mensaje.destinatario() == null || mensaje.destinatario().isBlank()) {
            log.info("Notificaciones: '{}' no se envía porque no hay correo del destinatario", mensaje.asunto());
            return;
        }
        enviar(mensaje);
    }
}
