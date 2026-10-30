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
 * integración del agregado Folio: pagos, cierre con saldo en cero y cierre autorizado con saldo
 * (RN-15, RN-16, RN-17), por HTTP contra la aplicación completa sobre H2.
 * Cada prueba crea su propia reserva en APT-102 lejos en el calendario para no chocar con las demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FolioControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final long DEPOSITO_GARANTIA = 250_000;

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("El folio nace con alojamiento + depósito y se cierra solo cuando el saldo es cero (RN-17)")
    void pagarTodoYCerrar() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(130);
        ResultActions creada = crearReserva(entrada);
        String folio = "/api/reservas/" + codigo(creada) + "/folio";
        long valorTotal = numero(creada, "$.valorTotal");

        ResultActions abono = post(folio + "/pagos", pago(100_000))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCargos").value(valorTotal + DEPOSITO_GARANTIA))
                .andExpect(jsonPath("$.totalPagado").value(100_000))
                .andExpect(jsonPath("$.pagos.length()").value(1))
                .andExpect(jsonPath("$.cerrado").value(false));
        long saldo = numero(abono, "$.saldo");

        // RN-17: con saldo pendiente el folio no se cierra
        put(folio + "/cerrar").andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", startsWith("RN-17")));

        post(folio + "/pagos", pago(saldo)).andExpect(status().isCreated()).andExpect(jsonPath("$.saldo").value(0));
        put(folio + "/cerrar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado").value(true))
                .andExpect(jsonPath("$.fechaCierre").value(LocalDate.now(BOGOTA).toString()));

        // RN-16: un folio cerrado no admite más movimientos ni se vuelve a cerrar
        post(folio + "/pagos", pago(1_000)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-16")));
        put(folio + "/cerrar").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("El administrador cierra con saldo y queda registrado quién y por qué (RN-17)")
    void cierreAutorizadoConSaldo() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(140);
        String folio = "/api/reservas/" + codigo(crearReserva(entrada)) + "/folio";

        post(folio + "/autorizacion-cierre", "{\"autorizadoPor\":\"Administradora Marta\","
                + "\"motivo\":\"El huésped deja el depósito pendiente por acuerdo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado").value(true))
                .andExpect(jsonPath("$.autorizadoPor").value("Administradora Marta"))
                .andExpect(jsonPath("$.motivoAutorizacion").value("El huésped deja el depósito pendiente por acuerdo"));

        post(folio + "/autorizacion-cierre", "{\"autorizadoPor\":\"Marta\",\"motivo\":\"Otra vez\"}")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("400: el pago necesita valor positivo y medio; la autorización necesita motivo")
    void datosInvalidosResponden400() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(150);
        String folio = "/api/reservas/" + codigo(crearReserva(entrada)) + "/folio";

        post(folio + "/pagos", "{\"valor\":-5000,\"medio\":\"TRANSFERENCIA_BANCARIA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.valor").exists());
        post(folio + "/pagos", "{\"valor\":5000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.medio").exists());
        post(folio + "/pagos", "{\"valor\":5000,\"medio\":\"EFECTIVO\"}").andExpect(status().isBadRequest());
        post(folio + "/autorizacion-cierre", "{\"autorizadoPor\":\"Marta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    @DisplayName("404: no hay folio para una reserva que no existe")
    void folioDeReservaInexistenteResponde404() throws Exception {
        String folio = "/api/reservas/RES-2026-99999/folio";
        post(folio + "/pagos", pago(5_000)).andExpect(status().isNotFound());
        put(folio + "/cerrar").andExpect(status().isNotFound());
    }


    private ResultActions crearReserva(LocalDate entrada) throws Exception {
        return post("/api/reservas", """
                {"identificacionApartamento":"APT-102","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"16:00","canalOrigen":"DIRECTO",
                 "titular":{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                            "email":"ana@correo.co","telefono":"3001234567"},
                 "ocupantes":[{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                               "fechaNacimiento":"1990-05-20"}]}
                """.formatted(entrada, entrada.plusDays(2))).andExpect(status().isCreated());
    }

    private static String pago(long valor) {
        return "{\"valor\":" + valor + ",\"medio\":\"TARJETA_DEBITO_CREDITO\"}";
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(url));
    }

    private static String codigo(ResultActions creada) throws Exception {
        return JsonPath.read(creada.andReturn().getResponse().getContentAsString(), "$.codigo");
    }

    private static long numero(ResultActions respuesta, String ruta) throws Exception {
        Number valor = JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta);
        return valor.longValue();
    }
}