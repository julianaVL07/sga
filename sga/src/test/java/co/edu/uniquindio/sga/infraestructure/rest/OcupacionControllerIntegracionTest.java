package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de la vista de recepción (7.1): llegadas y salidas previstas (CU-26, ReservaController) y calendario
 * de ocupación (CU-27, OcupacionController), sobre los datos iniciales del Coral (12.3):
 *   APT-102  CONFIRMADA  hoy+12 → hoy+15  (llegada prevista el día hoy+12)
 *   APT-201  EN_CURSO    hoy-1  → hoy+2   (salida prevista el día hoy+2)
 *   APT-301  bloqueo vigente de hoy+20 a hoy+25 por mantenimiento de la piscina de inmersión
 * Las pruebas comparten la base H2 con las demás clases, así que las de "hoy" no cuentan filas: verifican que
 * todo lo que llega tenga el estado y la fecha del día.
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class OcupacionControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mvc;

    // ==================== CU-26 ====================

    @Test
    @DisplayName("CU-26: sin fecha, las llegadas son las reservas CONFIRMADA que entran hoy en Colombia")
    void llegadasDeHoy() throws Exception {
        String hoy = LocalDate.now(BOGOTA).toString();
        get("/api/reservas/llegadas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(is("CONFIRMADA"))))
                .andExpect(jsonPath("$.contenido[*].fechaEntrada", everyItem(is(hoy))));
    }

    @Test
    @DisplayName("CU-26: sin fecha, las salidas son las reservas EN_CURSO que salen hoy en Colombia")
    void salidasDeHoy() throws Exception {
        String hoy = LocalDate.now(BOGOTA).toString();
        get("/api/reservas/salidas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(is("EN_CURSO"))))
                .andExpect(jsonPath("$.contenido[*].fechaSalida", everyItem(is(hoy))));
    }

    @Test
    @DisplayName("CU-26: la llegada prevista de los datos iniciales aparece el día de su entrada (APT-102)")
    void llegadasDeUnaFecha() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(12);
        get("/api/reservas/llegadas?fecha=" + entrada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].apartamento", hasItem("APT-102")))
                .andExpect(jsonPath("$.contenido[*].nombreTitular", hasItem("Santiago Herrera")))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(is("CONFIRMADA"))))
                .andExpect(jsonPath("$.contenido[*].fechaEntrada", everyItem(is(entrada.toString()))));
    }

    @Test
    @DisplayName("CU-26: la salida prevista de los datos iniciales aparece el día de su salida (APT-201)")
    void salidasDeUnaFecha() throws Exception {
        LocalDate salida = LocalDate.now(BOGOTA).plusDays(2);
        get("/api/reservas/salidas?fecha=" + salida)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].apartamento", hasItem("APT-201")))
                .andExpect(jsonPath("$.contenido[*].nombreTitular", hasItem("Andrés Mejía")))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(is("EN_CURSO"))))
                .andExpect(jsonPath("$.contenido[*].fechaSalida", everyItem(is(salida.toString()))));
    }

    @Test
    @DisplayName("CU-26, 400: página negativa o fecha mal escrita")
    void llegadasConParametrosInvalidosResponde400() throws Exception {
        get("/api/reservas/llegadas?pagina=-1").andExpect(status().isBadRequest());
        get("/api/reservas/salidas?fecha=2026-13-40").andExpect(status().isBadRequest());
    }

    // ==================== CU-27 ====================

    @Test
    @DisplayName("CU-27: el calendario de APT-201 muestra la reserva EN_CURSO de los datos iniciales noche por noche")
    void calendarioConReservaEnCurso() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(calendario("APT-201", hoy.minusDays(2), hoy.plusDays(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartamento").value("APT-201"))
                .andExpect(jsonPath("$.noches", hasSize(5)))
                .andExpect(jsonPath("$.noches[0].fecha").value(hoy.minusDays(2).toString()))
                .andExpect(jsonPath("$.noches[0].estado").value("LIBRE"))
                // Estancia hoy-1 → hoy+2: tres noches ocupadas, la de salida no (3.1)
                .andExpect(jsonPath("$.noches[1].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.noches[1].estadoReserva").value("EN_CURSO"))
                .andExpect(jsonPath("$.noches[1].codigoReserva", startsWith("RES-")))
                .andExpect(jsonPath("$.noches[2].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.noches[3].estado").value("RESERVADA"))
                .andExpect(jsonPath("$.noches[3].estadoReserva").value("EN_CURSO"))
                .andExpect(jsonPath("$.noches[4].fecha").value(hoy.plusDays(2).toString()))
                .andExpect(jsonPath("$.noches[4].estado").value("LIBRE"));
    }

    @Test
    @DisplayName("CU-27: el calendario de APT-301 muestra su bloqueo vigente con el motivo (RN-07)")
    void calendarioConBloqueo() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(calendario("APT-301", hoy.plusDays(19), hoy.plusDays(26)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches", hasSize(7)))
                .andExpect(jsonPath("$.noches[0].estado").value("LIBRE"))
                // Bloqueo hoy+20 → hoy+25: cinco noches, la fecha fin no queda incluida
                .andExpect(jsonPath("$.noches[1].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.noches[1].motivoBloqueo").value("Mantenimiento de la piscina de inmersión"))
                .andExpect(jsonPath("$.noches[1].codigoReserva").doesNotExist())
                .andExpect(jsonPath("$.noches[5].estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.noches[6].estado").value("LIBRE"));
    }

    @Test
    @DisplayName("CU-27, 409: la fecha desde debe ser anterior a la fecha hasta")
    void calendarioConRangoInvertidoResponde409() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(calendario("APT-101", hoy.plusDays(5), hoy))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.1")));
    }

    @Test
    @DisplayName("CU-27, 409: el calendario abarca máximo 62 noches (decisión del equipo)")
    void calendarioDemasiadoLargoResponde409() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(calendario("APT-101", hoy, hoy.plusDays(62))).andExpect(status().isOk())
                .andExpect(jsonPath("$.noches", hasSize(62)));
        get(calendario("APT-101", hoy, hoy.plusDays(63)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.1")));
    }

    @Test
    @DisplayName("CU-27, 404: el apartamento no existe")
    void calendarioDeApartamentoInexistenteResponde404() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(calendario("APT-999", hoy, hoy.plusDays(3))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CU-27, 400: faltan parámetros obligatorios")
    void calendarioSinParametrosResponde400() throws Exception {
        get("/api/reservas/calendario?apartamento=APT-101").andExpect(status().isBadRequest());
    }

    private static String calendario(String apartamento, LocalDate desde, LocalDate hasta) {
        return "/api/reservas/calendario?apartamento=" + apartamento + "&desde=" + desde + "&hasta=" + hasta;
    }

    private ResultActions get(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url));
    }
}
