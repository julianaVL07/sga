package co.edu.uniquindio.sga.infraestructure.notificaciones;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
import co.edu.uniquindio.sga.infrastructure.scheduler.RecordatoriosScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.cancelar;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.confirmar;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.crearReserva;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.numero;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.texto;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La integración no es bloqueante (10.3.3): con un adaptador que siempre falla, crear, confirmar y cancelar
 * responden igual y la reserva queda guardada; el fallo solo se registra en el log.
 * También comprueba que con sga.recordatorios.activo=false la tarea de recordatorios no existe.
 */
@Import({SeguridadPruebasConfig.class, NotificacionesFallidasIntegracionTest.AdaptadorQueFalla.class})
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sga-notificaciones-fallidas",
        "sga.recordatorios.activo=false"})
@AutoConfigureMockMvc
class NotificacionesFallidasIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Cuántas veces se intentó notificar. */
    static final AtomicInteger INTENTOS = new AtomicInteger();

    /** Reemplaza al adaptador local por uno que lanza una excepción en cada envío, como un SMTP caído. */
    @TestConfiguration
    static class AdaptadorQueFalla {

        @Bean
        @Primary
        NotificadorTitular notificadorQueFalla() {
            return new NotificadorTitular() {
                @Override
                public void reservaCreada(Reserva reserva) {
                    fallar();
                }

                @Override
                public void reservaConfirmada(Reserva reserva) {
                    fallar();
                }

                @Override
                public void reservaCancelada(Reserva reserva, Dinero retencion) {
                    fallar();
                }

                @Override
                public void recordatorio(Reserva reserva, MotivoRecordatorio motivo) {
                    fallar();
                }

                @Override
                public void codigoRecuperacion(String correo, String nombre, String codigo, long minutosVigencia) {
                    fallar();
                }

                private void fallar() {
                    INTENTOS.incrementAndGet();
                    throw new IllegalStateException("servidor de correo caído (prueba)");
                }
            };
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("Un adaptador que falla no impide crear, confirmar ni cancelar")
    void unAdaptadorQueFallaNoImpideReservar() throws Exception {
        int antes = INTENTOS.get();
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);

        ResultActions creada = crearReserva(mvc, "APT-301", entrada, entrada.plusDays(2), "titular@correo.co")
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        String codigo = texto(creada, "$.codigo");
        confirmar(mvc, codigo, numero(creada, "$.valorTotal"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        String otra = texto(crearReserva(mvc, "APT-301", entrada.plusDays(5), entrada.plusDays(7),
                "titular@correo.co"), "$.codigo");
        cancelar(mvc, otra).andExpect(jsonPath("$.estado").value("CANCELADA"));

        // Las reservas quedaron guardadas pese al fallo del correo
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas/" + otra))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        // Y sí se intentó notificar (4 eventos: 2 creadas, 1 confirmada, 1 cancelada), en otro hilo
        long limite = System.currentTimeMillis() + 5_000;
        while (INTENTOS.get() - antes < 4 && System.currentTimeMillis() < limite) {
            Thread.sleep(25);
        }
        assertTrue(INTENTOS.get() - antes >= 4, "Se esperaban 4 intentos de notificación");
    }

    @Test
    @DisplayName("Un adaptador que falla tampoco cambia la respuesta de la recuperación de contraseña")
    void recuperacionResponde202AunqueFalleElCorreo() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/recuperacion/solicitar")
                        .contentType("application/json").content("{\"correo\":\"huesped1@sga.local\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Con sga.recordatorios.activo=false la tarea de recordatorios está apagada")
    void recordatoriosApagados() {
        assertNull(contexto.getBeanProvider(RecordatoriosScheduler.class).getIfAvailable());
    }
}
