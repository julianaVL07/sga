package co.edu.uniquindio.sga.infraestructure.notificaciones;

import co.edu.uniquindio.sga.application.usecase.EnviarRecordatoriosUseCase;
import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
import co.edu.uniquindio.sga.infrastructure.notificaciones.MensajeCorreo;
import co.edu.uniquindio.sga.infrastructure.notificaciones.NotificadorTitularLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.cancelar;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.confirmar;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.crearReserva;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.esperarMensajes;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.numero;
import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.texto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notificaciones al titular con el adaptador LOCAL, el de por defecto (10.3.2, 7.8), sin red: crear, confirmar y
 * cancelar una reserva dejan su mensaje con el código, el apartamento, las fechas y el valor; y los recordatorios del
 * día anterior salen una sola vez. Cada prueba usa un correo de titular propio para no mezclar mensajes.
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-notificaciones")
@AutoConfigureMockMvc
class NotificacionesIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NotificadorTitular notificador;

    @Autowired
    private EnviarRecordatoriosUseCase enviarRecordatorios;

    @Test
    @DisplayName("Por defecto el adaptador es el local")
    void elAdaptadorPorDefectoEsElLocal() {
        assertInstanceOf(NotificadorTitularLocal.class, notificador);
    }

    @Test
    @DisplayName("Crear y confirmar una reserva dejan cada uno su mensaje con código, apartamento, fechas y valor")
    void crearYConfirmarDejanSuMensaje() throws Exception {
        String correo = "titular-confirmada@correo.co";
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        ResultActions creada = crearReserva(mvc, "APT-301", entrada, entrada.plusDays(2), correo);
        String codigo = texto(creada, "$.codigo");
        long valorTotal = numero(creada, "$.valorTotal");

        MensajeCorreo alCrear = unico(esperarMensajes(buzon(), correo, "RESERVA_CREADA"));
        assertTrue(alCrear.asunto().contains(codigo));
        assertTrue(alCrear.cuerpo().contains("PENDIENTE"));
        contieneLosDatosDeLaReserva(alCrear, codigo, entrada, valorTotal);

        confirmar(mvc, codigo, valorTotal);

        MensajeCorreo alConfirmar = unico(esperarMensajes(buzon(), correo, "RESERVA_CONFIRMADA"));
        assertTrue(alConfirmar.cuerpo().contains("CONFIRMADA"));
        assertTrue(alConfirmar.cuerpo().contains("16:00"), "Debe llevar la hora estimada de llegada");
        contieneLosDatosDeLaReserva(alConfirmar, codigo, entrada, valorTotal);
    }

    @Test
    @DisplayName("Cancelar deja su mensaje con la retención de la política")
    void cancelarDejaSuMensajeConLaRetencion() throws Exception {
        String correo = "titular-cancelada@correo.co";
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(50);
        ResultActions creada = crearReserva(mvc, "APT-301", entrada, entrada.plusDays(2), correo);
        String codigo = texto(creada, "$.codigo");
        long valorTotal = numero(creada, "$.valorTotal");

        // Con más de 15 días de antelación la política retiene 0 % (RN-13)
        cancelar(mvc, codigo);

        MensajeCorreo alCancelar = unico(esperarMensajes(buzon(), correo, "RESERVA_CANCELADA"));
        assertTrue(alCancelar.asunto().contains("cancelada"));
        assertTrue(alCancelar.cuerpo().contains("Retención: $0"), alCancelar.cuerpo());
        assertTrue(alCancelar.cuerpo().contains("Cambio de planes"), "Debe llevar el motivo");
        contieneLosDatosDeLaReserva(alCancelar, codigo, entrada, valorTotal);
    }

    @Test
    @DisplayName("Si la operación falla no se envía nada: el correo sale solo después de guardar")
    void unaOperacionRechazadaNoNotifica() throws Exception {
        String correo = "titular-rechazada@correo.co";
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(60);
        String codigo = texto(crearReserva(mvc, "APT-301", entrada, entrada.plusDays(2), correo), "$.codigo");

        // Sin anticipo no se confirma (L-11): la transacción se deshace y no debe salir el correo de confirmada
        mvc.perform(MockMvcRequestBuilders.put("/api/reservas/" + codigo + "/confirmar")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());

        esperarMensajes(buzon(), correo, "RESERVA_CREADA");
        assertTrue(buzon().mensajesPara(correo).stream().noneMatch(m -> m.motivo().equals("RESERVA_CONFIRMADA")));
    }

    @Test
    @DisplayName("El recordatorio de llegada sale el día anterior y una sola vez")
    void recordatorioDeLlegadaUnaSolaVez() throws Exception {
        String correo = "titular-recordatorio@correo.co";
        LocalDate manana = LocalDate.now(BOGOTA).plusDays(1);
        ResultActions creada = crearReserva(mvc, "APT-301", manana, manana.plusDays(2), correo);
        String codigo = texto(creada, "$.codigo");
        confirmar(mvc, codigo, numero(creada, "$.valorTotal"));

        enviarRecordatorios.ejecutar();
        enviarRecordatorios.ejecutar();

        List<MensajeCorreo> recordatorios = buzon().mensajesPara(correo).stream()
                .filter(m -> m.motivo().equals("RECORDATORIO_LLEGADA"))
                .toList();
        assertEquals(1, recordatorios.size(), "El mismo recordatorio no se envía dos veces");
        assertTrue(recordatorios.getFirst().cuerpo().contains(codigo));
        assertTrue(recordatorios.getFirst().cuerpo().contains("mañana"));
    }

    private NotificadorTitularLocal buzon() {
        return (NotificadorTitularLocal) notificador;
    }

    private static MensajeCorreo unico(List<MensajeCorreo> mensajes) {
        assertEquals(1, mensajes.size(), "Se esperaba exactamente un mensaje y llegaron " + mensajes.size());
        return mensajes.getFirst();
    }

    private static void contieneLosDatosDeLaReserva(MensajeCorreo mensaje, String codigo, LocalDate entrada,
                                                    long valorTotal) {
        String cuerpo = mensaje.cuerpo();
        assertTrue(cuerpo.contains("Código de reserva: " + codigo), cuerpo);
        assertTrue(cuerpo.contains("Apartamento: APT-301"), cuerpo);
        assertTrue(cuerpo.contains(String.valueOf(entrada.getDayOfMonth())), cuerpo);
        assertTrue(cuerpo.contains(String.valueOf(entrada.getYear())), cuerpo);
        assertTrue(cuerpo.contains("Noches: 2"), cuerpo);
        assertTrue(cuerpo.contains(String.format(Locale.forLanguageTag("es-CO"), "$%,d", valorTotal)),
                cuerpo);
    }
}
