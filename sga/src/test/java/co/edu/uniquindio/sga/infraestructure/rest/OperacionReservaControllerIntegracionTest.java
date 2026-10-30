package co.edu.uniquindio.sga.infraestructure.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de la operación: llegada y salida del grupo y cómo arrastran el estado operativo del
 * apartamento (PREPARADO --> OCUPADO --> PENDIENTE_PREPARACION --> EN_PREPARACION --> PREPARADO).
 * Usa APT-202 hoy y APT-301 en el futuro para no chocar con las demás pruebas que comparten la base H2.
 * Estos endpoints no reciben cuerpo ni parámetros, por eso no tienen un caso 400: solo 404 y 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OperacionReservaControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Llegada y salida arrastran el estado del apartamento y exigen que esté PREPARADO (RN-11, RN-12)")
    void llegadaYSalidaMuevenElEstadoDelApartamento() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        Confirmada reserva = reservaConfirmada("APT-202", hoy, hoy.plusDays(2));
        String codigo = reserva.codigo();

        // RN-11: si el apartamento no está PREPARADO no recibe al grupo. Los datos iniciales (12.3) lo dejan
        // FUERA_DE_SERVICIO, así que el personal debe prepararlo antes de la llegada.
        llegada(codigo).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", startsWith("RN-11")));
        cambiarEstado("APT-202", "PENDIENTE_PREPARACION").andExpect(status().isOk());
        cambiarEstado("APT-202", "EN_PREPARACION").andExpect(status().isOk());
        cambiarEstado("APT-202", "PREPARADO").andExpect(status().isOk());

        llegada(codigo).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_CURSO"));
        // El apartamento quedó OCUPADO: no se puede sacar de servicio con huéspedes adentro (7.6)
        cambiarEstado("APT-202", "FUERA_DE_SERVICIO").andExpect(status().isConflict());

        post("/api/reservas/" + codigo + "/folio/pagos", pago(reserva.saldo())).andExpect(status().isCreated());
        salida(codigo).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FINALIZADA"));

        // Tras la salida el apartamento pasa a PENDIENTE_PREPARACION, y el personal lo deja listo otra vez
        cambiarEstado("APT-202", "EN_PREPARACION").andExpect(status().isOk());
        cambiarEstado("APT-202", "PREPARADO").andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
    }

    @Test
    @DisplayName("409: no se registra la llegada antes de la fecha de entrada ni de una reserva sin confirmar (RN-10)")
    void llegadaFueraDeTiempoOSinConfirmarResponde409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(100);
        String pendiente = codigo(post("/api/reservas", reserva("APT-301", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated()));
        llegada(pendiente).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", startsWith("RN-10")));

        String confirmada = reservaConfirmada("APT-301", entrada.plusDays(5), entrada.plusDays(7)).codigo();
        llegada(confirmada).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", startsWith("RN-10")));
        // Una reserva que no está EN_CURSO tampoco puede registrar salida
        salida(confirmada).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("404: la reserva no existe")
    void reservaInexistenteResponde404() throws Exception {
        llegada("RES-2026-99999").andExpect(status().isNotFound());
        salida("RES-2026-99999").andExpect(status().isNotFound());
    }

    /** Código de la reserva y lo que queda por pagar en su folio después del anticipo. */
    private record Confirmada(String codigo, long saldo) {
    }

    /** Crea la reserva, paga el anticipo del 50 % (L-11) y la confirma. */
    private Confirmada reservaConfirmada(String apartamento, LocalDate entrada, LocalDate salida) throws Exception {
        ResultActions creada = post("/api/reservas", reserva(apartamento, entrada, salida))
                .andExpect(status().isCreated());
        String codigo = codigo(creada);
        ResultActions anticipo = post("/api/reservas/" + codigo + "/folio/pagos",
                pago(numero(creada, "$.valorTotal") / 2)).andExpect(status().isCreated());
        put("/api/reservas/" + codigo + "/confirmar", "{}").andExpect(status().isOk());
        return new Confirmada(codigo, numero(anticipo, "$.saldo"));
    }

    private ResultActions llegada(String codigo) throws Exception {
        return put("/api/reservas/" + codigo + "/registrar-llegada", null);
    }

    private ResultActions salida(String codigo) throws Exception {
        return put("/api/reservas/" + codigo + "/registrar-salida", null);
    }

    private ResultActions cambiarEstado(String apartamento, String estado) throws Exception {
        return put("/api/apartamentos/" + apartamento + "/estado-operativo", "{\"nuevoEstado\":\"" + estado + "\"}");
    }

    private static String reserva(String apartamento, LocalDate entrada, LocalDate salida) {
        return """
                {"identificacionApartamento":"%s","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"16:00","canalOrigen":"DIRECTO",
                 "titular":{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                            "email":"ana@correo.co","telefono":"3001234567"},
                 "ocupantes":[{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                               "fechaNacimiento":"1990-05-20"}]}
                """.formatted(apartamento, entrada, salida);
    }

    private static String pago(long valor) {
        return "{\"valor\":" + valor + ",\"medio\":\"TRANSFERENCIA_BANCARIA\"}";
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(String url, String json) throws Exception {
        var peticion = MockMvcRequestBuilders.put(url);
        if (json != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(peticion);
    }

    private static String codigo(ResultActions creada) throws Exception {
        return JsonPath.read(creada.andReturn().getResponse().getContentAsString(), "$.codigo");
    }

    private static long numero(ResultActions respuesta, String ruta) throws Exception {
        Number valor = JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta);
        return valor.longValue();
    }
}
