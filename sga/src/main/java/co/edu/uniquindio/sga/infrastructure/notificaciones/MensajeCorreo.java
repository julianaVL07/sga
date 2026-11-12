package co.edu.uniquindio.sga.infrastructure.notificaciones;

/**
 * Un correo listo para enviar: a quién, el asunto y el texto plano.
 *
 * @param motivo de qué se trata (RESERVA_CREADA, RECORDATORIO_LLEGADA...); sirve para el log y las pruebas.
 */
public record MensajeCorreo(String motivo, String destinatario, String asunto, String cuerpo) {

    /** El cuerpo no va al toString: puede llevar el código de recuperación. */
    @Override
    public String toString() {
        return "MensajeCorreo[motivo=" + motivo + ", destinatario=" + destinatario + ", asunto=" + asunto + "]";
    }
}
