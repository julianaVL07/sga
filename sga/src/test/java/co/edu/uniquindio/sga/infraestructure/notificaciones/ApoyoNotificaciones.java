package co.edu.uniquindio.sga.infraestructure.notificaciones;

import co.edu.uniquindio.sga.infrastructure.notificaciones.MensajeCorreo;
import co.edu.uniquindio.sga.infrastructure.notificaciones.NotificadorTitularLocal;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ayudas de las pruebas de integración de las notificaciones: reservas por HTTP y espera de los correos. */
final class ApoyoNotificaciones {

    /** Los correos salen en otro hilo después del commit: se espera hasta este tiempo a que lleguen. */
    private static final long ESPERA_MAXIMA_MS = 5_000;

    private ApoyoNotificaciones() {
    }

    /** POST /api/reservas por el canal DIRECTO con un titular de ese correo; devuelve la respuesta 201. */
    static ResultActions crearReserva(MockMvc mvc, String apartamento, LocalDate entrada, LocalDate salida,
                                      String correoTitular) throws Exception {
        String cuerpo = """
                {"identificacionApartamento":"%s","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"16:00","canalOrigen":"DIRECTO",
                 "titular":{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                            "email":"%s","telefono":"3001234567"},
                 "ocupantes":[{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                               "fechaNacimiento":"1990-05-20"}]}
                """.formatted(apartamento, entrada, salida, correoTitular);
        return mvc.perform(MockMvcRequestBuilders.post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }

    /** Paga el anticipo del 50 % (L-11) y confirma la reserva (CU-10). */
    static ResultActions confirmar(MockMvc mvc, String codigo, long valorTotal) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/reservas/" + codigo + "/folio/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":" + valorTotal / 2 + ",\"medio\":\"TRANSFERENCIA_BANCARIA\"}"))
                .andExpect(status().isCreated());
        return mvc.perform(MockMvcRequestBuilders.put("/api/reservas/" + codigo + "/confirmar")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    static ResultActions cancelar(MockMvc mvc, String codigo) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put("/api/reservas/" + codigo + "/cancelar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Cambio de planes\"}"))
                .andExpect(status().isOk());
    }

    static String texto(ResultActions respuesta, String ruta) throws Exception {
        return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta);
    }

    static long numero(ResultActions respuesta, String ruta) throws Exception {
        Number valor = JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta);
        return valor.longValue();
    }

    /** Espera a que el adaptador local tenga un mensaje de ese motivo para ese correo; vacía si no llega a tiempo. */
    static List<MensajeCorreo> esperarMensajes(NotificadorTitularLocal buzon, String correo, String motivo)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (true) {
            List<MensajeCorreo> encontrados = buzon.mensajesPara(correo).stream()
                    .filter(m -> m.motivo().equals(motivo))
                    .toList();
            if (!encontrados.isEmpty() || System.currentTimeMillis() > limite) {
                return encontrados;
            }
            Thread.sleep(25);
        }
    }
}
