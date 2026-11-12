package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import co.edu.uniquindio.sga.infrastructure.notificaciones.NotificadorTitularLocal;
import co.edu.uniquindio.sga.infrastructure.notificaciones.NotificadorTitularSmtp;
import co.edu.uniquindio.sga.infrastructure.notificaciones.PlantillasCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Properties;

/**
 * Único lugar donde se elige el adaptador del puerto NotificadorTitular (10.3.2), según
 * sga.notificaciones.proveedor: smtp (correo real) o local (en memoria y en el log, el de por defecto y el de las
 * pruebas). Un valor desconocido se registra en el log y deja el local.
 * Activa @Async para que los correos salgan en otro hilo, después de guardar la operación de negocio.
 */
@Configuration
@EnableAsync
public class NotificacionesConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesConfig.class);

    @Bean
    public PlantillasCorreo plantillasCorreo(
            @Value("${sga.notificaciones.nombre-alojamiento}") String nombreAlojamiento,
            @Value("${sga.alojamiento.hora-entrada}") LocalTime horaEntrada,
            @Value("${sga.alojamiento.hora-salida}") LocalTime horaSalida) {
        return new PlantillasCorreo(nombreAlojamiento, horaEntrada, horaSalida);
    }

    @Bean
    public NotificadorTitular notificadorTitular(
            PlantillasCorreo plantillas,
            @Value("${sga.notificaciones.proveedor:local}") String proveedor,
            @Value("${sga.notificaciones.smtp.host}") String host,
            @Value("${sga.notificaciones.smtp.puerto}") int puerto,
            @Value("${sga.notificaciones.smtp.starttls:true}") boolean starttls,
            @Value("${sga.notificaciones.smtp.tiempo-espera}") Duration tiempoEspera,
            @Value("${sga.notificaciones.smtp.usuario:}") String usuario,
            @Value("${sga.notificaciones.smtp.clave:}") String clave,
            @Value("${sga.notificaciones.remitente:}") String remitente) {
        String elegido = proveedor == null ? "" : proveedor.trim().toLowerCase(Locale.ROOT);
        NotificadorTitular notificador = switch (elegido) {
            case "smtp" -> new NotificadorTitularSmtp(plantillas,
                    servidorSmtp(host, puerto, starttls, tiempoEspera, usuario, clave),
                    remitente.isBlank() ? usuario : remitente);
            case "local" -> new NotificadorTitularLocal(plantillas);
            default -> {
                log.warn("Notificaciones: proveedor desconocido '{}' en sga.notificaciones.proveedor; se usa el local",
                        proveedor);
                yield new NotificadorTitularLocal(plantillas);
            }
        };
        log.info("Notificaciones: correos al titular con {}", notificador.getClass().getSimpleName());
        return notificador;
    }

    /** Cliente SMTP con autenticación, STARTTLS y el mismo tiempo de espera para conectar, leer y escribir. */
    private static JavaMailSenderImpl servidorSmtp(String host, int puerto, boolean starttls, Duration tiempoEspera,
                                                   String usuario, String clave) {
        if (usuario.isBlank() || clave.isBlank()) {
            log.warn("Notificaciones: faltan sga.notificaciones.smtp.usuario o sga.notificaciones.smtp.clave en "
                    + "application-secrets.properties; los correos no se van a poder enviar");
        }
        JavaMailSenderImpl servidor = new JavaMailSenderImpl();
        servidor.setHost(host);
        servidor.setPort(puerto);
        servidor.setUsername(usuario);
        servidor.setPassword(clave);
        servidor.setDefaultEncoding(StandardCharsets.UTF_8.name());
        String milisegundos = String.valueOf(tiempoEspera.toMillis());
        Properties propiedades = servidor.getJavaMailProperties();
        propiedades.put("mail.transport.protocol", "smtp");
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        propiedades.put("mail.smtp.starttls.required", String.valueOf(starttls));
        propiedades.put("mail.smtp.connectiontimeout", milisegundos);
        propiedades.put("mail.smtp.timeout", milisegundos);
        propiedades.put("mail.smtp.writetimeout", milisegundos);
        return servidor;
    }
}
