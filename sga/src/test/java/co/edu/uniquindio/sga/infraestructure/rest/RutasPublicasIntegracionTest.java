package co.edu.uniquindio.sga.infraestructure.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consultas públicas del visitante: responden sin token (permitAll en SecurityConfig y en el @PreAuthorize).
 * Las escrituras de esas mismas rutas y la ruta fija pendientes-preparacion siguen pidiendo el JWT.
 * A propósito no importa SeguridadPruebasConfig: ninguna petición lleva token.
 * Solo hace consultas y cotiza, pero usa su propia base H2 para no depender del orden de las demás pruebas.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-rutas-publicas")
@AutoConfigureMockMvc
class RutasPublicasIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String MENSAJE_401 = "Debe iniciar sesión o el token no es válido";

    @Autowired
    private MockMvc mvc;

    // ---------- Consultas públicas: 200 sin token ----------

    @Test
    @DisplayName("Sin token: GET /api/apartamentos responde 200")
    void listarApartamentosSinTokenResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/apartamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    @DisplayName("Sin token: GET /api/apartamentos/disponibles responde 200")
    void buscarDisponiblesSinTokenResponde200() throws Exception {
        LocalDate entrada = lunesLejano();
        mvc.perform(MockMvcRequestBuilders.get("/api/apartamentos/disponibles")
                        .param("entrada", entrada.toString())
                        .param("salida", entrada.plusDays(2).toString())
                        .param("ocupantes", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    @DisplayName("Sin token: GET /api/apartamentos/{id} responde 200")
    void detalleApartamentoSinTokenResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/apartamentos/APT-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacion").value("APT-101"));
    }

    @Test
    @DisplayName("Sin token: GET /api/alojamiento responde 200")
    void consultarAlojamientoSinTokenResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/alojamiento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").exists());
    }

    @Test
    @DisplayName("Sin token: GET /api/politica-cancelacion responde 200")
    void politicaVigenteSinTokenResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/politica-cancelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    @DisplayName("Sin token: GET /api/temporadas responde 200")
    void listarTemporadasSinTokenResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/temporadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    @DisplayName("Sin token: GET /api/temporadas/{id} responde 200")
    void consultarTemporadaSinTokenResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/temporadas/T-BASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("T-BASE"));
    }

    @Test
    @DisplayName("Sin token: POST /api/cotizaciones responde 200")
    void cotizarSinTokenResponde200() throws Exception {
        LocalDate entrada = lunesLejano();
        String cuerpo = """
                {"identificacionApartamento":"APT-101","fechaEntrada":"%s","fechaSalida":"%s",
                 "ocupantes":[{"nombre":"Ana Gómez","fechaNacimiento":"1990-05-20"}]}
                """.formatted(entrada, entrada.plusDays(2));
        mvc.perform(MockMvcRequestBuilders.post("/api/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(2))
                .andExpect(jsonPath("$.valorTotal").exists());
    }

    // ---------- Lo demás sigue pidiendo token: 401 ----------

    @Test
    @DisplayName("Sin token: GET /api/apartamentos/pendientes-preparacion sigue respondiendo 401")
    void pendientesDePreparacionSinTokenResponde401() throws Exception {
        esperar401(mvc.perform(MockMvcRequestBuilders.get("/api/apartamentos/pendientes-preparacion")),
                "/api/apartamentos/pendientes-preparacion");
    }

    @Test
    @DisplayName("Sin token: PUT /api/alojamiento responde 401")
    void actualizarAlojamientoSinTokenResponde401() throws Exception {
        esperar401(mvc.perform(MockMvcRequestBuilders.put("/api/alojamiento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")), "/api/alojamiento");
    }

    @Test
    @DisplayName("Sin token: POST /api/apartamentos responde 401")
    void registrarApartamentoSinTokenResponde401() throws Exception {
        esperar401(mvc.perform(MockMvcRequestBuilders.post("/api/apartamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")), "/api/apartamentos");
    }

    private static void esperar401(ResultActions respuesta, String path) throws Exception {
        respuesta.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_401))
                .andExpect(jsonPath("$.path").value(path));
    }

    /** Lunes a 90 días: fuera de los datos iniciales y sin tocar la estancia mínima de fin de semana (RP-01) */
    private static LocalDate lunesLejano() {
        LocalDate fecha = LocalDate.now(BOGOTA).plusDays(90);
        while (fecha.getDayOfWeek() != DayOfWeek.MONDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }
}
