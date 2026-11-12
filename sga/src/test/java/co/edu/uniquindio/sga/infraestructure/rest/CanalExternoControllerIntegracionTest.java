package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * integración del canal externo (CU-08): disponibilidad, reserva y cancelación desde una plataforma.
 * Todo mensaje queda en la bitácora (7.8), así que un rechazo no es una excepción sino un resultado:
 * ACEPTADO 201, DUPLICADO_IGNORADO 200 (RN-19), RECHAZADO_CONFLICTO 409 (RN-18), RECHAZADO_INVALIDO 422.
 * Por eso este controlador no tiene 404: una reserva del canal que no existe es RECHAZADO_INVALIDO.
 *
 * Cada canal se autentica con su credencial en X-Canal-Credencial (10.3.1). Los canales de esta prueba los crea el
 * administrador con POST /api/canales (SeguridadPruebasConfig pone su JWT en toda petición), y su credencial se
 * guarda la primera vez porque solo se muestra una. Ese JWT de administrador no basta para las operaciones del
 * canal: sin credencial, 401. Usa APT-101 lejos en el calendario para no chocar con las demás pruebas (los
 * escenarios obligatorios van a más de 230 días).
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class CanalExternoControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String BOOKING = "booking-ext";
    private static final String AIRBNB = "airbnb-ext";

    /** Credencial de cada canal creado en esta clase: el contexto de Spring (y su base H2) se reutiliza. */
    private static final Map<String, String> CREDENCIALES = new ConcurrentHashMap<>();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReservaRepository reservas;

    @Test
    @DisplayName("Reserva del canal: aceptada, duplicada, cancelada y las noches vuelven a la venta (RN-18, RN-19)")
    void cicloDeUnaReservaDelCanal() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(120);
        String disponibilidad = "/api/canales/" + BOOKING + "/disponibilidad?entrada=" + entrada + "&salida="
                + entrada.plusDays(2) + "&ocupantes=1";
        get(BOOKING, disponibilidad).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-101")));

        post(BOOKING, reserva("BK-EXT-1", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"))
                .andExpect(jsonPath("$.tipoEvento").value("CREACION_RESERVA"));
        get(BOOKING, disponibilidad).andExpect(jsonPath("$.contenido[*].identificacion", not(hasItem("APT-101"))));

        // RN-19: el canal reenvía el mismo mensaje y no se crea otra reserva
        post(BOOKING, reserva("BK-EXT-1", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO_IGNORADO"));

        cancelar(BOOKING, "BK-EXT-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"))
                .andExpect(jsonPath("$.tipoEvento").value("CANCELACION_RESERVA"));
        cancelar(BOOKING, "BK-EXT-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO_IGNORADO"));
        get(BOOKING, disponibilidad).andExpect(jsonPath("$.contenido[*].identificacion", hasItem("APT-101")));
    }

    @Test
    @DisplayName("Escenario obligatorio: una reserva externa en noches libres se acepta y nace PENDIENTE")
    void reservaExternaAceptada() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(230);
        post(BOOKING, reserva("BK-EXT-4", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificadorExterno").value("BK-EXT-4"))
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"));
        Reserva creada = reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "BK-EXT-4").orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals(CanalOrigen.EXTERNO, creada.getCanalOrigen());
        assertEquals(BOOKING, creada.getNombreCanalExterno());
        assertEquals(entrada, creada.getEstancia().fechaEntrada());
    }

    @Test
    @DisplayName("RN-19: el mismo identificador desde dos canales crea dos reservas; desde el mismo canal no duplica")
    void mismoIdentificadorEnDosCanales() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(270);
        long antes = reservas.listarTodas().size();

        post(BOOKING, reserva("COMPARTIDO-1", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"));
        post(AIRBNB, reserva("COMPARTIDO-1", "APT-101", entrada.plusDays(5), entrada.plusDays(7)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"));
        post(BOOKING, reserva("COMPARTIDO-1", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO_IGNORADO"));

        assertEquals(antes + 2, reservas.listarTodas().size());
        assertEquals(entrada, reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "COMPARTIDO-1").orElseThrow()
                .getEstancia().fechaEntrada());
        assertEquals(entrada.plusDays(5), reservas.buscarPorCanalEIdentificadorExterno(AIRBNB, "COMPARTIDO-1")
                .orElseThrow().getEstancia().fechaEntrada());

        // Cada canal cancela solo la suya: la de booking sigue PENDIENTE
        cancelar(AIRBNB, "COMPARTIDO-1").andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("ACEPTADO"));
        assertEquals(EstadoReserva.PENDIENTE, reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "COMPARTIDO-1")
                .orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Escenario obligatorio RN-19: el mismo mensaje repetido no crea una segunda reserva")
    void mensajeRepetidoNoDuplica() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(235);
        post(BOOKING, reserva("BK-EXT-5", "APT-101", entrada, entrada.plusDays(2))).andExpect(status().isCreated());
        long antes = reservas.listarTodas().size();

        post(BOOKING, reserva("BK-EXT-5", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO_IGNORADO"));

        assertEquals(antes, reservas.listarTodas().size(), "RN-19: el mensaje repetido no crea otra reserva");
        assertEquals(1, reservas.listarTodas().stream()
                .filter(r -> "BK-EXT-5".equals(r.getIdentificadorCanalExterno())).count());
    }

    @Test
    @DisplayName("12.4: la disponibilidad del canal se pagina de 10 en 10; una página fuera de rango viene vacía")
    void disponibilidadPaginada() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(160);
        String base = "/api/canales/" + BOOKING + "/disponibilidad?entrada=" + entrada + "&salida="
                + entrada.plusDays(2) + "&ocupantes=1";
        ResultActions primera = get(BOOKING, base).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido.length()", lessThanOrEqualTo(10)));
        long total = ((Number) JsonPath.read(primera.andReturn().getResponse().getContentAsString(),
                "$.totalElementos")).longValue();
        get(BOOKING, base + "&pagina=1").andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalElementos").value(total))
                .andExpect(jsonPath("$.contenido.length()").value((int) Math.max(0, total - 10)));
    }

    @Test
    @DisplayName("Escenario obligatorio RN-18: el canal pide noches ya vendidas, se rechaza con 409 y queda el conflicto")
    void nochesVendidasResponden409() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(125);
        post(BOOKING, reserva("BK-EXT-2", "APT-101", entrada, entrada.plusDays(3)))
                .andExpect(status().isCreated());
        post(AIRBNB, reserva("AB-EXT-2", "APT-101", entrada.plusDays(1), entrada.plusDays(3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO_CONFLICTO"));
        mvc.perform(MockMvcRequestBuilders.get("/api/canales/conflictos").param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.identificadorExterno == 'AB-EXT-2')].canal", hasItem(AIRBNB)));
    }

    @Test
    @DisplayName("422: datos que no cumplen las reglas o cancelación de una reserva que el canal no creó")
    void mensajesInvalidosResponden422() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(125);
        post(BOOKING, reserva("BK-EXT-3", "APT-999", entrada, entrada.plusDays(2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO_INVALIDO"));
        cancelar(BOOKING, "BK-NO-EXISTE")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.resultado").value("RECHAZADO_INVALIDO"));
    }

    @Test
    @DisplayName("400: falta el identificador externo, un parámetro de la disponibilidad o la página es negativa")
    void datosMalEscritosResponden400() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(125);
        post(BOOKING, reserva("", "APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.identificadorExterno").exists());
        get(BOOKING, "/api/canales/" + BOOKING + "/disponibilidad?entrada=" + entrada)
                .andExpect(status().isBadRequest());
        get(BOOKING, "/api/canales/" + BOOKING + "/disponibilidad?entrada=" + entrada + "&salida="
                + entrada.plusDays(2) + "&ocupantes=1&pagina=-1").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("401: sin credencial el canal no entra, aunque la petición lleve el JWT del administrador")
    void sinCredencialResponde401() throws Exception {
        credencial(BOOKING);
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(240);
        mvc.perform(MockMvcRequestBuilders.post("/api/canales/" + BOOKING + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva("BK-EXT-6", "APT-101", entrada, entrada.plusDays(2))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Falta la credencial del canal en el encabezado "
                        + "X-Canal-Credencial"))
                .andExpect(jsonPath("$.path").value("/api/canales/" + BOOKING + "/reservas"));
    }

    // ---------------------------------------------------------------- apoyo

    /** Crea el canal la primera vez (la credencial solo se muestra al crearlo) y la recuerda. */
    private String credencial(String canal) {
        return CREDENCIALES.computeIfAbsent(canal, nombre -> {
            try {
                String cuerpo = mvc.perform(MockMvcRequestBuilders.post("/api/canales")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"" + nombre + "\"}"))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
                return JsonPath.read(cuerpo, "$.credencial");
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo crear el canal " + nombre, e);
            }
        });
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

    private MockHttpServletRequestBuilder conCredencial(String canal, MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Canal-Credencial", credencial(canal));
    }

    private ResultActions get(String canal, String url) throws Exception {
        return mvc.perform(conCredencial(canal, MockMvcRequestBuilders.get(url)));
    }

    private ResultActions post(String canal, String json) throws Exception {
        return mvc.perform(conCredencial(canal, MockMvcRequestBuilders.post("/api/canales/" + canal + "/reservas"))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions cancelar(String canal, String identificadorExterno) throws Exception {
        return mvc.perform(conCredencial(canal, MockMvcRequestBuilders.put(
                "/api/canales/" + canal + "/reservas/" + identificadorExterno + "/cancelar")));
    }
}
