package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del agregado Apartamento: disponibilidad con filtros y paginación, listado, detalle, retiro de la
 * venta, bloqueos, estado operativo y novedades por HTTP contra la aplicación completa sobre H2 con los datos
 * iniciales del Coral (12.3: APT-202 fuera de servicio y bloqueado de hoy+3 a hoy+15, APT-301 bloqueado de hoy+20
 * a hoy+25).
 * Los datos iniciales de reservas (DatosInicialesReservasConfig) ocupan APT-101, APT-102 y APT-201 entre hoy-10 y
 * hoy+22; las búsquedas usan hoy+5, hoy+21 y hoy+40, que no chocan con reservas activas, y el retiro usa APT-301,
 * que no tiene reservas.
 * Usa su propia base H2 (sga-apartamentos) para que retirar un apartamento no afecte a las demás pruebas de
 * integración; la prueba que retira deja el contexto sucio y la siguiente arranca con los datos iniciales.
 * Las pruebas de CU-22 a CU-25 registran apartamentos nuevos (APT-401 a APT-404) y también dejan el contexto sucio,
 * para que el listado y las búsquedas de las demás pruebas sigan viendo solo los 5 apartamentos iniciales.
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-apartamentos")
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
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-301")));

        String bloqueo = "{\"fechaInicio\":\"" + inicio + "\",\"fechaFin\":\"" + inicio.plusDays(2)
                + "\",\"motivo\":\"Mantenimiento de la piscina\"}";
        ResultActions creado = post("/api/apartamentos/APT-301/bloqueos", bloqueo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-301"))
                .andExpect(jsonPath("$.vigente").value(true));
        // 201 con Location: apunta al bloqueo creado
        String idBloqueo = JsonPath.read(creado.andReturn().getResponse().getContentAsString(), "$.id");
        creado.andExpect(header().string("Location", endsWith("/api/apartamentos/APT-301/bloqueos/" + idBloqueo)));
        // El Location lleva a un recurso que existe: el mismo bloqueo
        get(creado.andReturn().getResponse().getHeader("Location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idBloqueo))
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-301"))
                .andExpect(jsonPath("$.motivo").value("Mantenimiento de la piscina"))
                .andExpect(jsonPath("$.vigente").value(true));

        get(disponibles)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-301"))))
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-201")));
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
        operacionEstado("APT-301", "poner-fuera-de-servicio").andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("FUERA_DE_SERVICIO"));
        // 7.6: de FUERA_DE_SERVICIO no se salta directo a PREPARADO
        operacionEstado("APT-301", "terminar-preparacion")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.6")));
        operacionEstado("APT-301", "restablecer-servicio").andExpect(status().isOk());
        operacionEstado("APT-301", "iniciar-preparacion").andExpect(status().isOk());
        operacionEstado("APT-301", "terminar-preparacion").andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
    }

    @Test
    @DisplayName("Novedad registrada con autor, descripción y gravedad; 201 con Location (7.6)")
    void registrarNovedad() throws Exception {
        ResultActions creada = post("/api/apartamentos/APT-201/novedades",
                "{\"autor\":\"Camarera Luz\",\"descripcion\":\"Falta una toalla de piscina\",\"gravedad\":\"BAJA\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-201"))
                .andExpect(jsonPath("$.gravedad").value("BAJA"))
                .andExpect(jsonPath("$.fechaAtencion").isEmpty());
        String idNovedad = JsonPath.read(creada.andReturn().getResponse().getContentAsString(), "$.id");
        creada.andExpect(header().string("Location", endsWith("/api/apartamentos/APT-201/novedades/" + idNovedad)));
        // El Location lleva a un recurso que existe: la misma novedad
        get(creada.andReturn().getResponse().getHeader("Location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idNovedad))
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-201"))
                .andExpect(jsonPath("$.descripcion").value("Falta una toalla de piscina"))
                .andExpect(jsonPath("$.gravedad").value("BAJA"));
    }

    @Test
    @DisplayName("400: falta un parámetro de la búsqueda o un dato de la novedad")
    void datosMalEscritosResponden400() throws Exception {
        get("/api/apartamentos/disponibles?entrada=2026-12-01&salida=2026-12-03").andExpect(status().isBadRequest());
        post("/api/apartamentos/APT-201/novedades", "{\"autor\":\"Luz\",\"gravedad\":\"ALTA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());
    }

    @Test
    @DisplayName("404: el apartamento no existe")
    void apartamentoInexistenteResponde404() throws Exception {
        for (String operacion : List.of("iniciar-preparacion", "terminar-preparacion", "poner-fuera-de-servicio",
                "restablecer-servicio")) {
            operacionEstado("APT-999", operacion).andExpect(status().isNotFound());
        }
        post("/api/apartamentos/APT-999/bloqueos",
                "{\"fechaInicio\":\"2027-03-01\",\"fechaFin\":\"2027-03-03\",\"motivo\":\"Pintura\"}")
                .andExpect(status().isNotFound());
        post("/api/apartamentos/APT-999/novedades",
                "{\"autor\":\"Luz\",\"descripcion\":\"Grieta\",\"gravedad\":\"MEDIA\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("404: bloqueo o novedad inexistente, de otro apartamento o de un apartamento que no existe")
    void bloqueoONovedadInexistenteResponde404() throws Exception {
        LocalDate inicio = LocalDate.now(BOGOTA).plusDays(150);
        String idBloqueo = JsonPath.read(post("/api/apartamentos/APT-102/bloqueos",
                "{\"fechaInicio\":\"" + inicio + "\",\"fechaFin\":\"" + inicio.plusDays(2) + "\",\"motivo\":\"Pintura\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        String idNovedad = JsonPath.read(post("/api/apartamentos/APT-102/novedades",
                "{\"autor\":\"Luz\",\"descripcion\":\"Grieta en la pared\",\"gravedad\":\"MEDIA\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        get("/api/apartamentos/APT-102/bloqueos/" + idBloqueo).andExpect(status().isOk());
        get("/api/apartamentos/APT-102/bloqueos/BLQ-no-existe")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", startsWith("No se encontró el bloqueo")));
        get("/api/apartamentos/APT-101/bloqueos/" + idBloqueo).andExpect(status().isNotFound());
        get("/api/apartamentos/APT-999/bloqueos/" + idBloqueo)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", startsWith("No se encontró el apartamento")));

        get("/api/apartamentos/APT-102/novedades/" + idNovedad).andExpect(status().isOk());
        get("/api/apartamentos/APT-102/novedades/NOV-no-existe")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", startsWith("No se encontró la novedad")));
        get("/api/apartamentos/APT-101/novedades/" + idNovedad).andExpect(status().isNotFound());
        get("/api/apartamentos/APT-999/novedades/" + idNovedad)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", startsWith("No se encontró el apartamento")));
    }

    // ==================== 7.6: historial de novedades ====================

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("7.6: el historial de novedades es paginado de 10 en 10, de la más reciente a la más antigua")
    void historialDeNovedadesPaginado() throws Exception {
        get("/api/apartamentos/APT-101/novedades")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0));

        for (int i = 1; i <= 11; i++) {
            post("/api/apartamentos/APT-101/novedades",
                    "{\"autor\":\"Camarera Luz\",\"descripcion\":\"Novedad " + i + "\",\"gravedad\":\"BAJA\"}")
                    .andExpect(status().isCreated());
        }

        get("/api/apartamentos/APT-101/novedades")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido", hasSize(10)))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("Novedad 11"))
                .andExpect(jsonPath("$.contenido[9].descripcion").value("Novedad 2"))
                .andExpect(jsonPath("$.contenido[*].identificacionApartamento", not(hasItem(not("APT-101")))));
        get("/api/apartamentos/APT-101/novedades?pagina=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("Novedad 1"));
        get("/api/apartamentos/APT-101/novedades?pagina=-1").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("404: no hay historial de novedades de un apartamento que no existe")
    void historialDeNovedadesDeApartamentoInexistenteResponde404() throws Exception {
        get("/api/apartamentos/APT-999/novedades").andExpect(status().isNotFound());
    }

    // ==================== 7.1: apartamentos pendientes de preparación ====================

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("7.1: pendientes de preparación lista PENDIENTE_PREPARACION y EN_PREPARACION, paginado de 10 en 10")
    void pendientesDePreparacionPaginado() throws Exception {
        // Recién arrancada ninguno está por preparar: APT-201 OCUPADO, APT-202 FUERA_DE_SERVICIO y el resto PREPARADO
        get("/api/apartamentos/pendientes-preparacion")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0));

        operacionEstado("APT-202", "restablecer-servicio").andExpect(status().isOk());
        operacionEstado("APT-301", "poner-fuera-de-servicio").andExpect(status().isOk());
        operacionEstado("APT-301", "restablecer-servicio").andExpect(status().isOk());
        operacionEstado("APT-301", "iniciar-preparacion").andExpect(status().isOk());

        get("/api/apartamentos/pendientes-preparacion")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-202", "APT-301")))
                .andExpect(jsonPath("$.contenido[*].estadoOperativo",
                        contains("PENDIENTE_PREPARACION", "EN_PREPARACION")));
        get("/api/apartamentos/pendientes-preparacion?pagina=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(2));
        get("/api/apartamentos/pendientes-preparacion?pagina=-1").andExpect(status().isBadRequest());

        // Al terminar la preparación sale de la lista
        operacionEstado("APT-301", "terminar-preparacion").andExpect(status().isOk());
        get("/api/apartamentos/pendientes-preparacion")
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-202")));
    }

    // ==================== CU-01: búsqueda con filtros y paginación (7.5) ====================

    @Test
    @DisplayName("7.5: la búsqueda responde una página de máximo 10 tarjetas con valor estimado y ubicación")
    void busquedaPaginadaConTarjetas() throws Exception {
        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 2, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido.length()", lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.contenido[0].identificacion").value("APT-101"))
                .andExpect(jsonPath("$.contenido[0].imagenPrincipal", startsWith("https://")))
                .andExpect(jsonPath("$.contenido[0].valorEstimado", greaterThan(0)))
                .andExpect(jsonPath("$.contenido[0].latitud").value(10.3975))
                .andExpect(jsonPath("$.contenido[0].longitud").value(-75.559));

        // Una página fuera de rango llega vacía pero conserva el total
        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 2, "&pagina=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(5));
    }

    @Test
    @DisplayName("7.5: filtro por capacidad mínima")
    void filtroPorCapacidadMinima() throws Exception {
        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 2, "&capacidadMinima=6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-301")));
        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 2, "&capacidadMinima=4"))
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-201", "APT-202", "APT-301")));
    }

    @Test
    @DisplayName("7.5: filtro por característica de la dotación, sin distinguir mayúsculas")
    void filtroPorCaracteristica() throws Exception {
        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 2, "&caracteristica=HIDROMASAJE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].identificacion", contains("APT-101", "APT-201")));
        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 2, "&caracteristica=helipuerto"))
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("7.5: filtro por rango de precio sobre el valor estimado de la estancia")
    void filtroPorPrecio() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        List<Number> valores = JsonPath.read(get(disponibles(entrada, 2, "")).andReturn().getResponse()
                .getContentAsString(), "$.contenido[*].valorEstimado");
        long minimo = valores.stream().mapToLong(Number::longValue).min().orElseThrow();
        long maximo = valores.stream().mapToLong(Number::longValue).max().orElseThrow();

        List<Number> baratos = JsonPath.read(get(disponibles(entrada, 2, "&precioMaximo=" + minimo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.contenido[*].valorEstimado");
        assertFalse(baratos.isEmpty());
        assertTrue(baratos.size() < valores.size());
        assertTrue(baratos.stream().allMatch(v -> v.longValue() <= minimo));

        List<Number> caros = JsonPath.read(get(disponibles(entrada, 2, "&precioMinimo=" + maximo))
                .andReturn().getResponse().getContentAsString(), "$.contenido[*].valorEstimado");
        assertFalse(caros.isEmpty());
        assertTrue(caros.stream().allMatch(v -> v.longValue() >= maximo));

        get(disponibles(entrada, 2, "&precioMinimo=900000&precioMaximo=100000"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.5")));
    }

    @Test
    @DisplayName("7.5 y RN-06: con fechas de nacimiento el niño no facturable baja el valor estimado")
    void fechasDeNacimientoAjustanElValorEstimado() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        Number sinFechas = JsonPath.read(get(disponibles(entrada, 2, "&capacidadMinima=6")).andReturn()
                .getResponse().getContentAsString(), "$.contenido[0].valorEstimado");
        Number conNino = JsonPath.read(get(disponibles(entrada, 2,
                        "&capacidadMinima=6&fechasNacimiento=1990-05-20&fechasNacimiento=" + entrada.minusYears(2)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.contenido[0].valorEstimado");

        assertEquals(sinFechas.longValue() / 2, conNino.longValue());
    }

    @Test
    @DisplayName("12.3: APT-202 está fuera de servicio y bloqueado, no aparece como disponible en esas noches")
    void apartamentoFueraDeServicioNoApareceEnElBloqueo() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(disponibles(hoy.plusDays(5), 2, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-202"))));

        get("/api/apartamentos/APT-202")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("FUERA_DE_SERVICIO"))
                .andExpect(jsonPath("$.bloqueosVigentes", hasSize(1)))
                .andExpect(jsonPath("$.bloqueosVigentes[0].fechaInicio").value(hoy.plusDays(3).toString()))
                .andExpect(jsonPath("$.bloqueosVigentes[0].fechaFin").value(hoy.plusDays(15).toString()));
    }

    @Test
    @DisplayName("12.3: APT-301 no aparece en las fechas de su bloqueo vigente y sí fuera de ellas (RN-07)")
    void apartamentoBloqueadoNoApareceEnLasFechasDelBloqueo() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        get(disponibles(hoy.plusDays(21), 2, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-301"))));
        get(disponibles(hoy.plusDays(40), 2, ""))
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-301")));
    }

    // ==================== CU-20: listado y detalle (7.3, 12.4) ====================

    @Test
    @DisplayName("CU-20: el listado de apartamentos es paginado de 10 en 10 y ordenado por identificación")
    void listadoPaginado() throws Exception {
        get("/api/apartamentos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido.length()", lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.contenido[*].identificacion",
                        contains("APT-101", "APT-102", "APT-201", "APT-202", "APT-301")));
        get("/api/apartamentos?pagina=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)));
        get("/api/apartamentos?pagina=-1").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CU-20: el detalle trae dotación, imágenes, tarifas por temporada y bloqueos vigentes")
    void detalleDelApartamento() throws Exception {
        get("/api/apartamentos/APT-301")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacion").value("APT-301"))
                .andExpect(jsonPath("$.capacidadMaxima").value(6))
                .andExpect(jsonPath("$.imagenes[0].principal").value(true))
                .andExpect(jsonPath("$.imagenPrincipal", startsWith("https://")))
                .andExpect(jsonPath("$.tarifas", hasSize(3)))
                .andExpect(jsonPath("$.bloqueosVigentes[*].motivo", hasItem("Mantenimiento de la piscina de inmersión")))
                .andExpect(jsonPath("$.eliminado").value(false));
        get("/api/apartamentos/APT-999").andExpect(status().isNotFound());
    }

    // ==================== CU-21: retiro de la venta (7.3, 12.4) ====================

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-21: retirar responde 200 y el apartamento deja de salir en el listado, el detalle y la búsqueda")
    void retirarApartamento() throws Exception {
        // APT-301 no tiene reservas en los datos iniciales; su bloqueo vigente (12.3) no impide retirarlo,
        // porque 7.3 solo exige que no haya reservas activas ni futuras.
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        get("/api/apartamentos/APT-301")
                .andExpect(jsonPath("$.bloqueosVigentes[*].motivo", hasItem("Mantenimiento de la piscina de inmersión")));
        get(disponibles(entrada, 2, "")).andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-301")));

        put("/api/apartamentos/APT-301/retirar", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificacion").value("APT-301"))
                .andExpect(jsonPath("$.eliminado").value(true))
                .andExpect(jsonPath("$.activo").value(false));

        get("/api/apartamentos")
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-301"))));
        get(disponibles(entrada, 2, ""))
                .andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-301"))));
        get("/api/apartamentos/APT-301").andExpect(status().isNotFound());

        // Ya retirado: retirarlo otra vez es una regla de negocio (7.3)
        put("/api/apartamentos/APT-301/retirar", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.3")));
    }

    @Test
    @DisplayName("CU-21: 409 al retirar un apartamento con reservas activas o futuras, 404 si no existe")
    void retirarConReservasActivasResponde409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(90);
        post("/api/reservas", reserva("APT-102", entrada, entrada.plusDays(2))).andExpect(status().isCreated());

        put("/api/apartamentos/APT-102/retirar", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.3")));
        get("/api/apartamentos/APT-102")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminado").value(false));
        put("/api/apartamentos/APT-999/retirar", "").andExpect(status().isNotFound());
    }

    // ==================== CU-22 a CU-25: administración y tarifas (7.3, 7.4) ====================

    private static final String TODAS_LAS_TARIFAS = """
            [{"idTemporada":"T-BASE","valorPorOcupante":300000},
             {"idTemporada":"T-MEDIA","valorPorOcupante":420000},
             {"idTemporada":"T-ALTA","valorPorOcupante":610000}]""";

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-22: registrar responde 201 con Location y el apartamento nace inactivo")
    void registrarApartamentoResponde201() throws Exception {
        post("/api/apartamentos", apartamentoNuevo("APT-401", 2, "[{\"idTemporada\":\"T-BASE\",\"valorPorOcupante\":300000}]"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/APT-401")))
                .andExpect(jsonPath("$.identificacion").value("APT-401"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"))
                .andExpect(jsonPath("$.imagenPrincipal").value("https://coral.co/img/apt-401-sala.jpg"))
                .andExpect(jsonPath("$.tarifas", hasSize(1)));

        get("/api/apartamentos/APT-401").andExpect(status().isOk());
        get("/api/apartamentos").andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    @DisplayName("CU-22: 409 si la identificación ya existe (L-04) o se viola una regla del agregado; 400 si falta un dato")
    void registrarConIdentificacionDuplicadaResponde409() throws Exception {
        post("/api/apartamentos", apartamentoNuevo("apt-101", 2, "[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("L-04")));

        String sinPrincipal = apartamentoNuevo("APT-401", 2, "[]").replace("\"principal\":true", "\"principal\":false");
        post("/api/apartamentos", sinPrincipal)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El apartamento debe tener exactamente una imagen principal"));

        post("/api/apartamentos", apartamentoNuevo("APT-401", 2, "[]").replace("\"nombre\":\"Coral Nuevo APT-401\",", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        get("/api/apartamentos/APT-401").andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-25: 409 al activar sin tarifa en todas las temporadas (7.4) y al activar uno retirado (7.3)")
    void activarSinTarifasCompletasResponde409() throws Exception {
        post("/api/apartamentos", apartamentoNuevo("APT-402", 2, "[{\"idTemporada\":\"T-BASE\",\"valorPorOcupante\":300000}]"))
                .andExpect(status().isCreated());

        put("/api/apartamentos/APT-402/activar", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.4")));
        get("/api/apartamentos/APT-402").andExpect(jsonPath("$.activo").value(false));

        // Retirado de la venta: no se activa (409) ni se actualiza (404)
        put("/api/apartamentos/APT-402/retirar", "").andExpect(status().isOk());
        put("/api/apartamentos/APT-402/activar", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.3")));
        put("/api/apartamentos/APT-402", actualizacion(2)).andExpect(status().isNotFound());
        put("/api/apartamentos/APT-999/activar", "").andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-22 + CU-24 + CU-25: crear, definir tarifas y activar lo pone en la búsqueda de disponibles")
    void crearDefinirTarifasYActivarApareceEnDisponibles() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        post("/api/apartamentos", apartamentoNuevo("APT-403", 2, "[]")).andExpect(status().isCreated());
        get(disponibles(entrada, 2, "")).andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-403"))));

        put("/api/apartamentos/APT-403/tarifas", "{\"tarifas\":" + TODAS_LAS_TARIFAS + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifas", hasSize(3)))
                .andExpect(jsonPath("$.activo").value(false));
        put("/api/apartamentos/APT-403/activar", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        get(disponibles(entrada, 2, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-403")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-24: el histórico conserva la tarifa anterior con su vigencia; 409 si la temporada no existe")
    void historicoConservaLaTarifaAnterior() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        post("/api/apartamentos", apartamentoNuevo("APT-404", 2, TODAS_LAS_TARIFAS)).andExpect(status().isCreated());

        put("/api/apartamentos/APT-404/tarifas", "{\"tarifas\":[{\"idTemporada\":\"T-BASE\",\"valorPorOcupante\":320000}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifas[?(@.idTemporada=='T-BASE')].valorPorOcupante", contains(320000)));

        get("/api/apartamentos/APT-404/tarifas/historico")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.contenido[0].idTemporada").value("T-BASE"))
                .andExpect(jsonPath("$.contenido[0].valorPorOcupante").value(320000))
                .andExpect(jsonPath("$.contenido[0].vigente").value(true))
                .andExpect(jsonPath("$.contenido[?(@.valorPorOcupante==300000)].vigenteDesde", contains(hoy.toString())))
                .andExpect(jsonPath("$.contenido[?(@.valorPorOcupante==300000)].vigenteHasta", contains(hoy.toString())))
                .andExpect(jsonPath("$.contenido[?(@.valorPorOcupante==300000)].vigente", contains(false)));

        put("/api/apartamentos/APT-404/tarifas", "{\"tarifas\":[{\"idTemporada\":\"T-NAVIDAD\",\"valorPorOcupante\":900000}]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.4")));
        put("/api/apartamentos/APT-404/tarifas", "{\"tarifas\":[{\"idTemporada\":\"T-BASE\",\"valorPorOcupante\":0}]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.4")));
        put("/api/apartamentos/APT-404/tarifas", "{\"tarifas\":[]}").andExpect(status().isBadRequest());
        get("/api/apartamentos/APT-404/tarifas/historico?pagina=-1").andExpect(status().isBadRequest());
        get("/api/apartamentos/APT-404/tarifas/historico").andExpect(jsonPath("$.totalElementos").value(4));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-23 (7.3): bajar la capacidad lista las reservas activas o futuras que la superan, sin tocarlas")
    void bajarCapacidadListaLasReservasAfectadas() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(50);
        post("/api/apartamentos", apartamentoNuevo("APT-401", 4, TODAS_LAS_TARIFAS)).andExpect(status().isCreated());
        put("/api/apartamentos/APT-401/activar", "").andExpect(status().isOk());

        String respuesta = post("/api/reservas", reservaDeGrupo("APT-401", entrada, entrada.plusDays(3), 3))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(respuesta, "$.codigo");

        put("/api/apartamentos/APT-401", actualizacion(3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasSobreCapacidad", hasSize(0)))
                .andExpect(jsonPath("$.advertencia").doesNotExist());

        put("/api/apartamentos/APT-401", actualizacion(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartamento.capacidadMaxima").value(2))
                .andExpect(jsonPath("$.apartamento.nombre").value("Coral Renovado"))
                .andExpect(jsonPath("$.reservasSobreCapacidad", contains(codigo)))
                .andExpect(jsonPath("$.advertencia", startsWith("7.3")));

        get("/api/reservas/" + codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes", hasSize(3)));
        put("/api/apartamentos/APT-401", actualizacion(0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-02")));
    }

    @Test
    @DisplayName("Cuerpos de error de /api/apartamentos: 400 con errores por campo, 401, 403, 404 y 409")
    void cuerposDeErrorDeApartamentos() throws Exception {
        put("/api/apartamentos/APT-101/tarifas", "{\"tarifas\":[]}")
                .andExpect(CuerpoError.validacion("/api/apartamentos/APT-101/tarifas",
                        Map.of("tarifas", "Debe indicar al menos una tarifa")));

        mvc.perform(MockMvcRequestBuilders.get("/api/apartamentos/pendientes-preparacion").with(anonymous()))
                .andExpect(CuerpoError.error(HttpStatus.UNAUTHORIZED, CuerpoError.MENSAJE_401,
                        "/api/apartamentos/pendientes-preparacion"));

        mvc.perform(MockMvcRequestBuilders.put("/api/apartamentos/APT-101/retirar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("HUESPED"))))
                .andExpect(CuerpoError.error(HttpStatus.FORBIDDEN, CuerpoError.MENSAJE_403,
                        "/api/apartamentos/APT-101/retirar"));

        get("/api/apartamentos/APT-999")
                .andExpect(CuerpoError.error(HttpStatus.NOT_FOUND,
                        "No se encontró el apartamento con identificación: APT-999", "/api/apartamentos/APT-999"));

        get(disponibles(LocalDate.now(BOGOTA).plusDays(40), 1, "&capacidadMinima=0"))
                .andExpect(CuerpoError.error(HttpStatus.CONFLICT,
                        "7.5: la capacidad mínima debe ser mayor que cero", "/api/apartamentos/disponibles"));
    }

    private static String apartamentoNuevo(String id, int capacidad, String tarifas) {
        return """
                {"identificacion":"%s","nombre":"Coral Nuevo %s","descripcion":"Unidad de prueba",
                 "dormitorios":1,"capacidadMaxima":%d,"dotacion":["Balcón con vista al mar","Cocina equipada"],
                 "imagenes":[{"url":"https://coral.co/img/%s-sala.jpg","principal":true},
                             {"url":"https://coral.co/img/%s-balcon.jpg","principal":false}],
                 "tarifas":%s}
                """.formatted(id, id, capacidad, id.toLowerCase(), id.toLowerCase(), tarifas);
    }

    private static String actualizacion(int capacidad) {
        return """
                {"nombre":"Coral Renovado","descripcion":"Remodelado","dormitorios":2,"capacidadMaxima":%d,
                 "dotacion":["Balcón con vista al mar","Tina de hidromasaje"]}
                """.formatted(capacidad);
    }

    private static String reservaDeGrupo(String apartamento, LocalDate entrada, LocalDate salida, int ocupantes) {
        StringBuilder grupo = new StringBuilder();
        for (int i = 0; i < ocupantes; i++) {
            if (i > 0) grupo.append(',');
            grupo.append("""
                    {"nombre":"Ocupante %d","tipoDocumento":"CC","numeroDocumento":"10940002%02d",
                     "fechaNacimiento":"1990-05-20"}""".formatted(i + 1, i));
        }
        return """
                {"identificacionApartamento":"%s","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"16:00","canalOrigen":"PORTAL",
                 "titular":{"nombre":"Ocupante 1","tipoDocumento":"CC","numeroDocumento":"1094000200",
                            "email":"grupo@correo.co","telefono":"3001234567"},
                 "ocupantes":[%s]}
                """.formatted(apartamento, entrada, salida, grupo);
    }

    private static String disponibles(LocalDate entrada, int ocupantes, String filtros) {
        return "/api/apartamentos/disponibles?entrada=" + entrada + "&salida=" + entrada.plusDays(3)
                + "&ocupantes=" + ocupantes + filtros;
    }

    /** CU-16: operación de estado operativo sin cuerpo, por ejemplo iniciar-preparacion (7.6). */
    private ResultActions operacionEstado(String apartamento, String operacion) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put("/api/apartamentos/" + apartamento + "/" + operacion));
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
