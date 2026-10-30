package co.edu.uniquindio.sga.infrastructure.rest;

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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del agregado Apartamento: disponibilidad, bloqueos, estado operativo y novedades por HTTP
 * contra la aplicación completa sobre H2 con los datos iniciales del Coral.
 * Comparte la base con las demás pruebas de integración, por eso usa fechas y apartamentos propios:
 * APT-301 y APT-102 lejos en el calendario y APT-301 para el ciclo de preparación.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApartamentoControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Un bloqueo retira el apartamento de la búsqueda de disponibles (RN-07)")
    void bloqueoSacaElApartamentoDeLaDisponibilidad() throws Exception {
        LocalDate inicio = LocalDate.now(BOGOTA).plusDays(70);
        String disponibles = "/api/apartamentos/disponibles?entrada=" + inicio + "&salida=" + inicio.plusDays(2)
                + "&ocupantes=2";
        get(disponibles)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].identificacion", hasItem("APT-301")));

        String bloqueo = "{\"fechaInicio\":\"" + inicio + "\",\"fechaFin\":\"" + inicio.plusDays(2)
                + "\",\"motivo\":\"Mantenimiento de la piscina\"}";
        post("/api/apartamentos/APT-301/bloqueos", bloqueo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-301"))
                .andExpect(jsonPath("$.vigente").value(true));

        get(disponibles)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].identificacion", not(hasItem("APT-301"))))
                .andExpect(jsonPath("$[*].identificacion", hasItem("APT-201")));
    }

    @Test
    @DisplayName("409: no se bloquean noches que ya tienen una reserva activa (7.3, RN-01)")
    void bloqueoSobreReservaActivaResponde409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(80);
        post("/api/reservas", reserva("APT-102", entrada, entrada.plusDays(2))).andExpect(status().isCreated());

        String bloqueo = "{\"fechaInicio\":\"" + entrada + "\",\"fechaFin\":\"" + entrada.plusDays(1)
                + "\",\"motivo\":\"Pintura\"}";
        post("/api/apartamentos/APT-102/bloqueos", bloqueo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-01")));
    }

    @Test
    @DisplayName("Ciclo de preparación: fuera de servicio, pendiente, en preparación y preparado (7.6)")
    void cicloDeEstadoOperativo() throws Exception {
        cambiarEstado("APT-301", "FUERA_DE_SERVICIO").andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("FUERA_DE_SERVICIO"));
        // 7.6: de FUERA_DE_SERVICIO no se salta directo a PREPARADO
        cambiarEstado("APT-301", "PREPARADO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.6")));
        cambiarEstado("APT-301", "PENDIENTE_PREPARACION").andExpect(status().isOk());
        cambiarEstado("APT-301", "EN_PREPARACION").andExpect(status().isOk());
        cambiarEstado("APT-301", "PREPARADO").andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
    }

    @Test
    @DisplayName("Novedad registrada con autor, descripción y gravedad (7.6)")
    void registrarNovedad() throws Exception {
        post("/api/apartamentos/APT-201/novedades",
                "{\"autor\":\"Camarera Luz\",\"descripcion\":\"Falta una toalla de piscina\",\"gravedad\":\"BAJA\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-201"))
                .andExpect(jsonPath("$.gravedad").value("BAJA"))
                .andExpect(jsonPath("$.fechaAtencion").isEmpty());
    }

    @Test
    @DisplayName("400: falta un parámetro de la búsqueda o un estado que no existe")
    void datosMalEscritosResponden400() throws Exception {
        get("/api/apartamentos/disponibles?entrada=2026-12-01&salida=2026-12-03").andExpect(status().isBadRequest());
        cambiarEstado("APT-201", "LIMPIO").andExpect(status().isBadRequest());
        post("/api/apartamentos/APT-201/novedades", "{\"autor\":\"Luz\",\"gravedad\":\"ALTA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());
    }

    @Test
    @DisplayName("404: el apartamento no existe")
    void apartamentoInexistenteResponde404() throws Exception {
        cambiarEstado("APT-999", "FUERA_DE_SERVICIO").andExpect(status().isNotFound());
        post("/api/apartamentos/APT-999/bloqueos",
                "{\"fechaInicio\":\"2027-03-01\",\"fechaFin\":\"2027-03-03\",\"motivo\":\"Pintura\"}")
                .andExpect(status().isNotFound());
        post("/api/apartamentos/APT-999/novedades",
                "{\"autor\":\"Luz\",\"descripcion\":\"Grieta\",\"gravedad\":\"MEDIA\"}")
                .andExpect(status().isNotFound());
    }

    private ResultActions cambiarEstado(String apartamento, String estado) throws Exception {
        return put("/api/apartamentos/" + apartamento + "/estado-operativo", "{\"nuevoEstado\":\"" + estado + "\"}");
    }

    private static String reserva(String apartamento, LocalDate entrada, LocalDate salida) {
        return """
                {"identificacionApartamento":"%s","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"16:00","canalOrigen":"PORTAL",
                 "titular":{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                            "email":"ana@correo.co","telefono":"3001234567"},
                 "ocupantes":[{"nombre":"Ana Gomez","tipoDocumento":"CC","numeroDocumento":"1094000111",
                               "fechaNacimiento":"1990-05-20"}]}
                """.formatted(apartamento, entrada, salida);
    }

    private ResultActions get(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url));
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
