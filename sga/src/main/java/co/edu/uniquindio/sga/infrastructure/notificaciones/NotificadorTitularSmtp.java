package co.edu.uniquindio.sga.infrastructure.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

/**
 * Adaptador real del puerto NotificadorTitular (10.3.2): envía el correo por SMTP con JavaMailSender
 * (sga.notificaciones.proveedor=smtp). El host, el puerto y el tiempo de espera vienen de application.properties;
 * el usuario y la clave solo de application-secrets.properties.
 * Si el servidor no responde a tiempo, rechaza las credenciales o faltan, lanza la excepción: quien llama la
 * registra en el log y la operación de negocio sigue igual.
 */
public class NotificadorTitularSmtp extends NotificadorTitularConPlantillas {

    private static final Logger log = LoggerFactory.getLogger(NotificadorTitularSmtp.class);

    private final JavaMailSender correo;
    private final String remitente;

    /** @param remitente dirección del campo "De"; con Gmail debe ser la misma cuenta del usuario SMTP. */
    public NotificadorTitularSmtp(PlantillasCorreo plantillas, JavaMailSender correo, String remitente) {
        super(plantillas);
        this.correo = Objects.requireNonNull(correo);
        this.remitente = remitente == null ? "" : remitente.trim();
    }

    @Override
    protected void enviar(MensajeCorreo mensaje) {
        if (remitente.isEmpty()) {
            throw new IllegalStateException("faltan el usuario y la clave SMTP (sga.notificaciones.smtp.usuario y "
                    + "sga.notificaciones.smtp.clave en application-secrets.properties)");
        }
        SimpleMailMessage correoSimple = new SimpleMailMessage();
        correoSimple.setFrom(remitente);
        correoSimple.setTo(mensaje.destinatario());
        correoSimple.setSubject(mensaje.asunto());
        correoSimple.setText(mensaje.cuerpo());
        correo.send(correoSimple);
        log.info("Notificaciones (smtp): enviado '{}' a {}", mensaje.asunto(), mensaje.destinatario());
    }
}
