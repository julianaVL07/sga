package co.edu.uniquindio.sga.infrastructure.rest;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de la cotización (CU-02): tarifa por noche según temporada y ocupantes facturables,
 * calculada con las temporadas y tarifas cargadas del Anexo A. Solo consulta, no reserva nada.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CotizacionControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String ADULTO = "{\"nombre\":\"Ana Gomez\",\"fechaNacimiento\":\"1990-05-20\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("La cotización trae una línea por noche y el total es la suma de los subtotales")
    void cotizarDetallaCadaNoche() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(90);
        String respuesta = cotizar(cuerpo("APT-201", entrada, entrada.plusDays(3), ADULTO + "," + ADULTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(3))
                .andExpect(jsonPath("$.detalles.length()").value(3))
                .andExpect(jsonPath("$.detalles[0].fecha").value(entrada.toString()))
                .andExpect(jsonPath("$.detalles[0].ocupantesFacturables").value(2))
                .andReturn().getResponse().getContentAsString();

        List<Number> subtotales = JsonPath.read(respuesta, "$.detalles[*].subtotal");
        Number total = JsonPath.read(respuesta, "$.valorTotal");
        assertThat(subtotales.stream().mapToLong(Number::longValue).sum()).isEqualTo(total.longValue());
    }

    @Test
    @DisplayName("400: la cotización necesita al menos un ocupante")
    void cotizarSinOcupantesResponde400() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(90);
        cotizar(cuerpo("APT-201", entrada, entrada.plusDays(2), ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.ocupantes").exists());
    }

    @Test
    @DisplayName("404: no se cotiza un apartamento que no existe")
    void cotizarApartamentoInexistenteResponde404() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(90);
        cotizar(cuerpo("APT-999", entrada, entrada.plusDays(2), ADULTO)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("409: la salida debe ser posterior a la entrada (RN-03)")
    void cotizarConFechasInvertidasResponde409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(90);
        cotizar(cuerpo("APT-201", entrada, entrada.minusDays(1), ADULTO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-03")));
    }

    private static String cuerpo(String apartamento, LocalDate entrada, LocalDate salida, String ocupantes) {
        return "{\"identificacionApartamento\":\"" + apartamento + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"ocupantes\":[" + ocupantes + "]}";
    }

    private ResultActions cotizar(String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/cotizaciones")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
