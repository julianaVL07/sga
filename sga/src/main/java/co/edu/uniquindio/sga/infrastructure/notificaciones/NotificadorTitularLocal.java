package co.edu.uniquindio.sga.infrastructure.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Adaptador LOCAL del puerto NotificadorTitular (10.3.2): no envía nada por la red. Guarda los mensajes en memoria y
 * los escribe en el log. Es el que corre por defecto y en las pruebas (sga.notificaciones.proveedor=local).
 * Como el log muestra el texto completo, en desarrollo el código de recuperación de contraseña se lee ahí.
 * Conserva solo los últimos {@value #MAXIMO_MENSAJES} mensajes para no crecer sin límite.
 */
public class NotificadorTitularLocal extends NotificadorTitularConPlantillas {

    private static final Logger log = LoggerFactory.getLogger(NotificadorTitularLocal.class);

    static final int MAXIMO_MENSAJES = 500;

    private final Deque<MensajeCorreo> buzon = new ConcurrentLinkedDeque<>();

    public NotificadorTitularLocal(PlantillasCorreo plantillas) {
        super(plantillas);
    }

    @Override
    protected void enviar(MensajeCorreo mensaje) {
        buzon.addLast(mensaje);
        while (buzon.size() > MAXIMO_MENSAJES) {
            buzon.pollFirst();
        }
        log.info("Notificaciones (local): correo a {} · {}\n{}", mensaje.destinatario(), mensaje.asunto(),
                mensaje.cuerpo());
    }

    /** Los mensajes guardados, del más antiguo al más reciente. */
    public List<MensajeCorreo> mensajes() {
        return List.copyOf(new ArrayList<>(buzon));
    }

    /** Los mensajes de un destinatario, sin distinguir mayúsculas. */
    public List<MensajeCorreo> mensajesPara(String correo) {
        return mensajes().stream().filter(m -> m.destinatario().equalsIgnoreCase(correo)).toList();
    }

    public void limpiar() {
        buzon.clear();
    }
}
