package co.edu.uniquindio.sga.infraestructure.rest;

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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * integración del canal externo (CU-08): disponibilidad, reserva y cancelación desde una plataforma.
 * Todo mensaje queda en la bitácora (7.8), así que un rechazo no es una excepción sino un resultado:
 * ACEPTADO 201, DUPLICADO_IGNORADO 200 (RN-19), RECHAZADO_CONFLICTO 409 (RN-18), RECHAZADO_INVALIDO 422.
 * Por eso este controlador no tiene 404: una reserva del canal que no existe es RECHAZADO_INVALIDO.
 * Usa APT-101 lejos en el calendario para no chocar con las demás pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CanalExternoControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Reserva del canal: aceptada, duplicada, cancelada y las noches vuelven a la venta (RN-18, RN-19)")
    void cicloDeUnaReservaDelCanal() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(120);
        String disponibilidad = "/api/canales/booking/disponibilidad?entrada=" + entrada + "&salida="
                + entrada.plusDays(2) + "&ocupantes=1";
        get(disponibilidad).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].identificacion", hasItem("APT-101")));

        post("/api/canales/booking/reservas", reserva("BK-F5-1", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"))
                .andExpect(jsonPath("$.tipoEvento").value("CREACION_RESERVA"));
        get(disponibilidad).andExpect(jsonPath("$[*].identificacion", not(hasItem("APT-101"))));

        // RN-19: el canal reenvía el mismo mensaje y no se crea otra reserva
        post("/api/canales/booking/reservas", reserva("BK-F5-1", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO_IGNORADO"));

        put("/api/canales/booking/reservas/BK-F5-1/cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"))
                .andExpect(jsonPath("$.tipoEvento").value("CANCELACION_RESERVA"));
        put("/api/canales/booking/reservas/BK-F5-1/cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO_IGNORADO"));
        get(disponibilidad).andExpect(jsonPath("$[*].identificacion", hasItem("APT-101")));
    }

    @Test
    @DisplayName("409: el canal pide noches que ya están vendidas (RN-18)")
    void nochesVendidasResponden409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(125);
        post("/api/canales/booking/reservas", reserva("BK-F5-2", "APT-101", entrada, entrada.plusDays(3)))
                .andExpect(status().isCreated());
        post("/api/canales/airbnb/reservas", reserva("AB-F5-2", "APT-101", entrada.plusDays(1), entrada.plusDays(3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO_CONFLICTO"));
    }

    @Test
    @DisplayName("422: datos que no cumplen las reglas o cancelación de una reserva que el canal no creó")
    void mensajesInvalidosResponden422() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(125);
        post("/api/canales/booking/reservas", reserva("BK-F5-3", "APT-999", entrada, entrada.plusDays(2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO_INVALIDO"));
        put("/api/canales/booking/reservas/BK-NO-EXISTE/cancelar")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO_INVALIDO"));
    }

    @Test
    @DisplayName("400: falta el identificador externo o un parámetro de la disponibilidad")
    void datosMalEscritosResponden400() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(125);
        post("/api/canales/booking/reservas", reserva("", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.identificadorExterno").exists());
        get("/api/canales/booking/disponibilidad?entrada=" + entrada).andExpect(status().isBadRequest());
    }

    private static String reserva(String idExterno, String apartamento, LocalDate entrada, LocalDate salida) {
        return """
                {"identificadorExterno":"%s","identificacionApartamento":"%s","fechaEntrada":"%s","fechaSalida":"%s",
                 "titular":{"nombre":"Carlos Ruiz","tipoDocumento":"PASAPORTE","numeroDocumento":"X1234567",
                            "email":"carlos@correo.com","telefono":"3109876543"},
                 "ocupantes":[{"nombre":"Carlos Ruiz","tipoDocumento":"PASAPORTE","numeroDocumento":"X1234567",
                               "fechaNacimiento":"1985-02-11"}]}
                """.formatted(idExterno, apartamento, entrada, salida);
    }

    private ResultActions get(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url));
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(url));
    }
}