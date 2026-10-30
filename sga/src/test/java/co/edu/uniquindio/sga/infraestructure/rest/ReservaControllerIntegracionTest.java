// src/test/java/co/edu/uniquindio/sga/infrastructure/rest/ReservaControllerIntegracionTest.java
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del agregado Reserva: levanta la aplicación completa (controladores, casos de uso, dominio,
 * JPA sobre H2 y los datos iniciales del Coral) y la recorre por HTTP como lo haría Postman.
 * Cubre el ciclo completo, la cancelación, la modificación, el listado (CU-19) y un 400, un 404 y un 409 de
 * ReservaController.
 * Las pruebas comparten la base H2, por eso cada una usa su propio apartamento o sus propias fechas.
 * Las fechas se calculan con el reloj de Colombia, el mismo que usa el SGA (L-13, RN-10).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Ciclo completo: crear, anticipo, confirmar, llegada, saldo y salida con el folio cerrado")
    void cicloCompletoDeUnaReserva() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        ResultActions creada = crear("APT-101", hoy, hoy.plusDays(2), "16:00", "DIRECTO")
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        String codigo = codigo(creada);
        long valorTotal = numero(creada, "$.valorTotal");

        // L-11: el anticipo del 50 % debe estar pagado antes de confirmar
        put("/api/reservas/" + codigo + "/confirmar", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("L-11")));
        ResultActions anticipo = post("/api/reservas/" + codigo + "/folio/pagos", pago(valorTotal / 2))
                .andExpect(status().isCreated());
        // El folio carga alojamiento + depósito de garantía, así que queda saldo por pagar al salir
        long saldo = numero(anticipo, "$.saldo");
        put("/api/reservas/" + codigo + "/confirmar", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        put("/api/reservas/" + codigo + "/registrar-llegada", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        // RN-17: con saldo pendiente no se registra la salida
        put("/api/reservas/" + codigo + "/registrar-salida", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-17")));

        post("/api/reservas/" + codigo + "/folio/pagos", pago(saldo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.cerrado").value(false));

        put("/api/reservas/" + codigo + "/registrar-salida", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"));
        // RN-16: el folio quedó cerrado y ya no admite pagos
        post("/api/reservas/" + codigo + "/folio/pagos", pago(1000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-16")));
    }

    @Test
    @DisplayName("Cancelación con más de 15 días: retiene 0 % y libera las noches (RN-12, RN-13)")
    void cancelarConAntelacionLiberaLasNoches() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(30);
        String codigo = codigo(crear("APT-102", entrada, entrada.plusDays(2), "16:00", "PORTAL"));

        put("/api/reservas/" + codigo + "/cancelar", "{\"motivo\":\"Cambio de planes\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.retencion").value(0));
        get("/api/reservas/" + codigo)
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelacion").value("Cambio de planes"));

        // RN-12: las mismas noches quedan libres para otra reserva
        crear("APT-102", entrada, entrada.plusDays(2), "16:00", "PORTAL");

        // RN-08: una reserva cancelada no se vuelve a cancelar
        put("/api/reservas/" + codigo + "/cancelar", "{\"motivo\":\"Otra vez\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-08")));
    }

    @Test
    @DisplayName("Modificación: una noche más se cobra como diferencia en el folio (RN-14)")
    void modificarFechasAsientaLaDiferencia() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        ResultActions creada = crear("APT-201", entrada, entrada.plusDays(2), "16:00", "PORTAL");
        String codigo = codigo(creada);
        long valorInicial = numero(creada, "$.valorTotal");

        String cuerpo = "{\"fechaEntrada\":\"" + entrada + "\",\"fechaSalida\":\"" + entrada.plusDays(3) + "\"}";
        ResultActions modificada = put("/api/reservas/" + codigo, cuerpo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserva.noches").value(3));
        long valorFinal = numero(modificada, "$.reserva.valorTotal");
        modificada.andExpect(jsonPath("$.diferencia").value(valorFinal - valorInicial));
    }

    @Test
    @DisplayName("400: falta el titular")
    void crearSinTitularResponde400() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(60);
        String cuerpo = "{\"identificacionApartamento\":\"APT-301\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + entrada.plusDays(2) + "\",\"canalOrigen\":\"PORTAL\","
                + "\"ocupantes\":[{\"nombre\":\"Ana Gomez\",\"fechaNacimiento\":\"1990-05-20\"}]}";
        post("/api/reservas", cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titular").exists());
    }

    @Test
    @DisplayName("404: la reserva no existe")
    void consultarReservaInexistenteResponde404() throws Exception {
        get("/api/reservas/RES-2026-99999").andExpect(status().isNotFound());
        put("/api/reservas/RES-2026-99999/cancelar", "{\"motivo\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("409: dos reservas no pueden ocupar las mismas noches (RN-01)")
    void reservaSolapadaResponde409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(50);
        crear("APT-202", entrada, entrada.plusDays(3), "16:00", "PORTAL");
        post("/api/reservas", reserva("APT-202", entrada.plusDays(1), entrada.plusDays(3), "16:00", "PORTAL"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-01")));
    }


    // ==================== CU-19: listado ====================

    @Test
    @DisplayName("CU-19: el listado muestra primero la reserva más reciente")
    void listarOrdenaDeLaMasRecienteALaMasAntigua() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(200);
        String primera = codigo(crear("APT-102", entrada, entrada.plusDays(2), "16:00", "PORTAL"));
        String segunda = codigo(crear("APT-102", entrada.plusDays(5), entrada.plusDays(7), "16:00", "PORTAL"));

        get("/api/reservas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.contenido[0].codigo").value(segunda))
                .andExpect(jsonPath("$.contenido[1].codigo").value(primera))
                .andExpect(jsonPath("$.contenido[0].apartamento").value("APT-102"))
                .andExpect(jsonPath("$.contenido[0].nombreTitular").value("Ana Gomez"))
                .andExpect(jsonPath("$.contenido[0].totalOcupantes").value(1));
    }

    @Test
    @DisplayName("CU-19: el filtro por estado solo devuelve reservas en ese estado")
    void listarFiltraPorEstado() throws Exception {
        get("/api/reservas?estado=CANCELADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", not(empty())))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(is("CANCELADA"))));
    }

    @Test
    @DisplayName("CU-19: el filtro por canal solo devuelve reservas de ese canal")
    void listarFiltraPorCanal() throws Exception {
        get("/api/reservas?canal=EXTERNO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", not(empty())))
                .andExpect(jsonPath("$.contenido[*].canalOrigen", everyItem(is("EXTERNO"))));
    }

    @Test
    @DisplayName("CU-19: cada página trae como máximo 10 reservas (12.4)")
    void listarPaginaDeAMaximoDiez() throws Exception {
        // Con los datos de prueba (7 reservas) y estas 4 hay al menos 11: no caben en una sola página
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(300);
        for (int i = 0; i < 4; i++) {
            crear("APT-101", entrada.plusDays(3L * i), entrada.plusDays(3L * i + 2), "16:00", "DIRECTO");
        }

        get("/api/reservas?pagina=0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido", hasSize(10)))
                .andExpect(jsonPath("$.totalElementos", greaterThan(10)))
                .andExpect(jsonPath("$.totalPaginas", greaterThan(1)));
        get("/api/reservas?pagina=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.contenido", hasSize(lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.contenido", not(empty())));
    }

    @Test
    @DisplayName("CU-19, 400: la página no puede ser negativa")
    void listarConPaginaNegativaResponde400() throws Exception {
        get("/api/reservas?pagina=-1").andExpect(status().isBadRequest());
    }


    /** POST /api/reservas que debe responder 201 con el header Location. */
    private ResultActions crear(String apartamento, LocalDate entrada, LocalDate salida, String hora, String canal)
            throws Exception {
        return post("/api/reservas", reserva(apartamento, entrada, salida, hora, canal))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    private static String reserva(String apartamento, LocalDate entrada, LocalDate salida, String hora, String canal) {
        return """
                {"identificacionApartamento":"%s","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"%s","canalOrigen":"%s",
                 "titular":{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                            "email":"ana@correo.co","telefono":"3001234567"},
                 "ocupantes":[{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                               "fechaNacimiento":"1990-05-20"}]}
                """.formatted(apartamento, entrada, salida, hora, canal);
    }

    private static String pago(long valor) {
        return "{\"valor\":" + valor + ",\"medio\":\"TRANSFERENCIA_BANCARIA\"}";
    }

    private ResultActions get(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url));
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json));
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
