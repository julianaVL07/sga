package co.edu.uniquindio.sga.domain.port;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;

/**
 * Puerto de las notificaciones al titular (10.3.2, 7.5 y 7.8): un método por motivo.
 * Cada mensaje lleva el código de la reserva, el apartamento, las fechas y el valor.
 * La integración no es bloqueante: quien llama lo hace después de que la operación de negocio quedó guardada,
 * y si el envío falla (el adaptador puede lanzar una excepción) lo registra en el log y sigue. Si el titular no
 * tiene correo, el adaptador no envía nada y lo registra.
 */
public interface NotificadorTitular {

    /** La reserva se creó y quedó PENDIENTE de confirmación. */
    void reservaCreada(Reserva reserva);

    /** La reserva pasó a CONFIRMADA. */
    void reservaConfirmada(Reserva reserva);

    /** La reserva se canceló; {@code retencion} es lo que se retiene según la política congelada (RN-13). */
    void reservaCancelada(Reserva reserva, Dinero retencion);

    /** Recordatorio del día anterior a la llegada o a la salida. */
    void recordatorio(Reserva reserva, MotivoRecordatorio motivo);

    /**
     * Código para restablecer la contraseña (7.2). Va a la persona de la cuenta, que no siempre es titular de una
     * reserva, por eso recibe sus datos en lugar de una reserva.
     *
     * @param minutosVigencia minutos que dura el código.
     */
    void codigoRecuperacion(String correo, String nombre, String codigo, long minutosVigencia);
}
