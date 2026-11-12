package co.edu.uniquindio.sga.infrastructure.notificaciones;

import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adaptador local de las notificaciones (10.3.2): guarda en memoria, sin red. */
class NotificadorTitularLocalTest {

    private final NotificadorTitularLocal notificador = new NotificadorTitularLocal(
            new PlantillasCorreo("Apartahotel Coral", LocalTime.of(15, 0), LocalTime.of(11, 0)));

    @Test
    @DisplayName("Guarda el mensaje con el código, la vigencia y la firma")
    void guardaElMensaje() {
        notificador.codigoRecuperacion("laura@correo.co", "Laura", "048213", 15);

        MensajeCorreo mensaje = notificador.mensajesPara("LAURA@correo.co").getFirst();
        assertEquals("CODIGO_RECUPERACION", mensaje.motivo());
        assertTrue(mensaje.cuerpo().contains("048213"));
        assertTrue(mensaje.cuerpo().contains("15 minutos"));
        assertTrue(mensaje.cuerpo().contains("Apartahotel Coral"));
        // El código no se cuela en el toString (ni en el log de errores)
        assertTrue(!mensaje.toString().contains("048213"));
    }

    @Test
    @DisplayName("Sin correo del destinatario no se envía nada")
    void sinCorreoNoEnvia() {
        notificador.codigoRecuperacion("  ", "Laura", "048213", 15);
        notificador.codigoRecuperacion(null, "Laura", "048213", 15);

        assertTrue(notificador.mensajes().isEmpty());
    }

    @Test
    @DisplayName("Conserva solo los últimos mensajes")
    void conservaLosUltimos() {
        for (int i = 0; i < NotificadorTitularLocal.MAXIMO_MENSAJES + 10; i++) {
            notificador.codigoRecuperacion("c" + i + "@correo.co", null, "000000", 15);
        }
        assertEquals(NotificadorTitularLocal.MAXIMO_MENSAJES, notificador.mensajes().size());
        assertEquals("c10@correo.co", notificador.mensajes().getFirst().destinatario());
    }

    @Test
    @DisplayName("Los valores van en pesos colombianos, sin decimales")
    void formatoDePesos() {
        assertEquals("$1.250.000", PlantillasCorreo.pesos(Dinero.de(1_250_000)));
        assertEquals("$0", PlantillasCorreo.pesos(Dinero.cero()));
    }
}
