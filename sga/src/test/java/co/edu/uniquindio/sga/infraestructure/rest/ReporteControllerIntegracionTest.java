package co.edu.uniquindio.sga.infraestructure.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de los reportes del administrador (7.9) contra la aplicación completa sobre H2 con los datos
 * iniciales de DatosInicialesReservasConfig, en el periodo de hoy-30 a hoy+30 (61 noches):
 *   noches vendidas: FINALIZADA APT-101 (3) + EN_CURSO APT-201 (3) + CONFIRMADA APT-102 (3) = 9, sobre 5
 *   apartamentos activos; reservas: PORTAL 3 (pendiente, cancelada y no-show), DIRECTO 3 y EXTERNO 1.
 * Cada petición lleva el token del rol que se prueba. Usa su propia base H2 (sga-reportes).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-reportes")
@AutoConfigureMockMvc
class ReporteControllerIntegracionTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
    private static final String PERIODO = "?desde=" + HOY.minusDays(30) + "&hasta=" + HOY.plusDays(30);

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("7.9: ocupación con noches vendidas y porcentaje sobre apartamentos activos × noches del periodo")
    void ocupacion() throws Exception {
        get("/api/reportes/ocupacion" + PERIODO, "ADMINISTRADOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nochesPeriodo").value(61))
                .andExpect(jsonPath("$.apartamentosActivos").value(5))
                .andExpect(jsonPath("$.nochesDisponibles").value(305))
                .andExpect(jsonPath("$.nochesVendidas").value(9))
                .andExpect(jsonPath("$.porcentajeOcupacion").value(2.95))
                .andExpect(jsonPath("$.apartamentos[*].apartamento",
                        contains("APT-101", "APT-102", "APT-201", "APT-202", "APT-301")));
    }

    @Test
    @DisplayName("7.9: ingresos por medio de pago; el total es la suma de los medios")
    void ingresos() throws Exception {
        String cuerpo = get("/api/reportes/ingresos" + PERIODO, "ADMINISTRADOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0)))
                .andExpect(jsonPath("$.medios", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        long total = ((Number) JsonPath.read(cuerpo, "$.total")).longValue();
        List<Number> porMedio = JsonPath.read(cuerpo, "$.medios[*].total");
        assertEquals(total, porMedio.stream().mapToLong(Number::longValue).sum());
    }

    @Test
    @DisplayName("7.9: reservas por canal, incluidas las canceladas y los no-show")
    void reservasPorCanal() throws Exception {
        get("/api/reportes/reservas-por-canal" + PERIODO, "ADMINISTRADOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservas").value(7))
                .andExpect(jsonPath("$.canales[*].canal", contains("PORTAL", "DIRECTO", "EXTERNO")))
                .andExpect(jsonPath("$.canales[0].total").value(3))
                .andExpect(jsonPath("$.canales[0].canceladas").value(1))
                .andExpect(jsonPath("$.canales[0].noShow").value(1))
                .andExpect(jsonPath("$.canales[1].total").value(3))
                .andExpect(jsonPath("$.canales[2].total").value(1));
    }

    @Test
    @DisplayName("7.9: desde posterior a hasta es 409 y una fecha faltante o mal escrita es 400")
    void periodoInvalido() throws Exception {
        get("/api/reportes/ocupacion?desde=" + HOY + "&hasta=" + HOY.minusDays(1), "ADMINISTRADOR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.9")));
        get("/api/reportes/ingresos?desde=" + HOY, "ADMINISTRADOR").andExpect(status().isBadRequest());
        get("/api/reportes/reservas-por-canal?desde=ayer&hasta=" + HOY, "ADMINISTRADOR")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7.9: los reportes son solo del administrador (403 para recepción) y piden token (401)")
    void soloAdministrador() throws Exception {
        get("/api/reportes/ocupacion" + PERIODO, "RECEPCIONISTA").andExpect(status().isForbidden());
        get("/api/reportes/ingresos" + PERIODO, "PERSONAL_SERVICIO").andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.get("/api/reportes/reservas-por-canal" + PERIODO))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions get(String url, String rol) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).with(jwt().authorities(new SimpleGrantedAuthority(rol))));
    }
}
