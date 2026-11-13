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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * integración del agregado Folio: pagos, cierre con saldo en cero y cierre autorizado con saldo
 * (RN-15, RN-16, RN-17), por HTTP contra la aplicación completa sobre H2. El depósito de garantía de RP-02 se asienta
 * como cargo de tipo AJUSTE_MODIFICACION (Anexo A, glosario y 7.7). También cubre el cargo de un servicio adicional
 * (7.1, L-17) y la corrección por movimiento inverso (RN-16); en un folio nuevo la posición 0 es el alojamiento, la
 * 1 el depósito y la 2 el primer cargo que se agregue.
 * Cada prueba crea su propia reserva en APT-102 lejos en el calendario para no chocar con las demás.
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class FolioControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final long VALOR_DEPOSITO_GARANTIA = 250_000;
    /** Datos iniciales del Coral (A.5): cena para 2 personas en el balcón. */
    private static final String CENA = "SRV-CENA-BALCON";
    private static final long VALOR_CENA = 260_000;
    private static final String RESERVA_INEXISTENTE = "/api/reservas/RES-2026-99999/folio";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("El folio nace con alojamiento + depósito (RP-02, AJUSTE_MODIFICACION), el pago responde 201 con "
            + "Location y se cierra solo cuando el saldo es cero (RN-17)")
    void pagarTodoYCerrar() throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(130);
        ResultActions creada = crearReserva(entrada);
        String folio = "/api/reservas/" + codigo(creada) + "/folio";
        long valorTotal = numero(creada, "$.valorTotal");

        ResultActions abono = post(folio + "/pagos", pago(100_000))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(folio)))
                .andExpect(jsonPath("$.cargos[0].tipo").value("ALOJAMIENTO"))
                .andExpect(jsonPath("$.cargos[1].tipo").value("AJUSTE_MODIFICACION"))
                .andExpect(jsonPath("$.cargos[1].valor").value(VALOR_DEPOSITO_GARANTIA))
                .andExpect(jsonPath("$.totalCargos").value(valorTotal + VALOR_DEPOSITO_GARANTIA))
                .andExpect(jsonPath("$.totalPagado").value(100_000))
                .andExpect(jsonPath("$.pagos.length()").value(1))
                .andExpect(jsonPath("$.cerrado").value(false));
        long saldo = numero(abono, "$.saldo");

        // La Location del pago lleva al folio, que se consulta con GET
        get(abono.andReturn().getResponse().getHeader("Location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(saldo))
                .andExpect(jsonPath("$.pagos.length()").value(1));

        // RN-17: con saldo pendiente el folio no se cierra
        put(folio + "/cerrar").andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", startsWith("RN-17")));

        post(folio + "/pagos", pago(saldo)).andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(folio)))
                .andExpect(jsonPath("$.saldo").value(0));
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
        get(folio).andExpect(status().isNotFound());
        post(folio + "/pagos", pago(5_000)).andExpect(status().isNotFound());
        put(folio + "/cerrar").andExpect(status().isNotFound());
    }


    // ==================== 7.1: cargo de un servicio adicional ====================

    @Test
    @DisplayName("7.1 y L-17: el cargo de un servicio adicional responde 201 con Location y se suma al saldo")
    void registrarCargoDeServicioAdicional() throws Exception {
        ResultActions creada = crearReserva(LocalDate.now(BOGOTA).plusDays(170));
        String folio = "/api/reservas/" + codigo(creada) + "/folio";
        long valorTotal = numero(creada, "$.valorTotal");

        post(folio + "/cargos", cargo(CENA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(folio)))
                .andExpect(jsonPath("$.cargos.length()").value(3))
                .andExpect(jsonPath("$.cargos[2].tipo").value("SERVICIO_ADICIONAL"))
                .andExpect(jsonPath("$.cargos[2].concepto").value("Cena en balcón por cocinero privado"))
                .andExpect(jsonPath("$.cargos[2].valor").value(VALOR_CENA))
                .andExpect(jsonPath("$.cargos[2].fecha").value(LocalDate.now(BOGOTA).toString()))
                .andExpect(jsonPath("$.totalCargos").value(valorTotal + VALOR_DEPOSITO_GARANTIA + VALOR_CENA))
                .andExpect(jsonPath("$.saldo").value(valorTotal + VALOR_DEPOSITO_GARANTIA + VALOR_CENA));
        get(folio).andExpect(jsonPath("$.cargos.length()").value(3));
    }

    @Test
    @DisplayName("Cargo de servicio: 404 si la reserva no existe y 400 si falta el id del servicio")
    void cargoDeServicioResponde404Y400() throws Exception {
        post(RESERVA_INEXISTENTE + "/cargos", cargo(CENA)).andExpect(status().isNotFound());
        post(RESERVA_INEXISTENTE + "/cargos", "{\"idServicio\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idServicio").exists());
    }

    @Test
    @DisplayName("Cargo de servicio: 409 si el alojamiento no ofrece el servicio (L-17) o si el folio está cerrado "
            + "(RN-16)")
    void cargoDeServicioResponde409() throws Exception {
        String folio = "/api/reservas/" + codigo(crearReserva(LocalDate.now(BOGOTA).plusDays(175))) + "/folio";

        post(folio + "/cargos", cargo("SRV-NO-EXISTE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("L-17")));

        cerrarConAutorizacion(folio);
        post(folio + "/cargos", cargo(CENA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-16")));
        get(folio).andExpect(jsonPath("$.cargos.length()").value(2));
    }

    // ==================== RN-16: corrección por movimiento inverso ====================

    @Test
    @DisplayName("RN-16: la corrección responde 201 con Location, asienta el cargo contrario y deja el original")
    void registrarCorreccion() throws Exception {
        ResultActions creada = crearReserva(LocalDate.now(BOGOTA).plusDays(180));
        String folio = "/api/reservas/" + codigo(creada) + "/folio";
        long valorTotal = numero(creada, "$.valorTotal");
        post(folio + "/cargos", cargo(CENA)).andExpect(status().isCreated());

        post(folio + "/correcciones", correccion(2, "La cena no se consumió"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(folio)))
                .andExpect(jsonPath("$.cargos.length()").value(4))
                .andExpect(jsonPath("$.cargos[2].valor").value(VALOR_CENA))           // el original sigue igual
                .andExpect(jsonPath("$.cargos[3].tipo").value("SERVICIO_ADICIONAL"))
                .andExpect(jsonPath("$.cargos[3].valor").value(-VALOR_CENA))
                .andExpect(jsonPath("$.cargos[3].concepto", containsString("La cena no se consumió")))
                .andExpect(jsonPath("$.totalCargos").value(valorTotal + VALOR_DEPOSITO_GARANTIA))
                .andExpect(jsonPath("$.saldo").value(valorTotal + VALOR_DEPOSITO_GARANTIA));
    }

    @Test
    @DisplayName("Corrección: 404 si la reserva no existe y 400 si falta la posición del cargo o el motivo")
    void correccionResponde404Y400() throws Exception {
        post(RESERVA_INEXISTENTE + "/correcciones", correccion(0, "Error")).andExpect(status().isNotFound());
        post(RESERVA_INEXISTENTE + "/correcciones", "{\"motivo\":\"Error\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.indiceCargo").exists());
        post(RESERVA_INEXISTENTE + "/correcciones", "{\"indiceCargo\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    @DisplayName("Corrección: 409 si no hay un cargo en esa posición o si el folio está cerrado (RN-16)")
    void correccionResponde409() throws Exception {
        String folio = "/api/reservas/" + codigo(crearReserva(LocalDate.now(BOGOTA).plusDays(185))) + "/folio";

        post(folio + "/correcciones", correccion(99, "No existe"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-16")));
        post(folio + "/correcciones", correccion(-1, "No existe"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-16")));

        cerrarConAutorizacion(folio);
        post(folio + "/correcciones", correccion(0, "Tarde"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("RN-16")));
        get(folio).andExpect(jsonPath("$.cargos.length()").value(2));
    }

    @Test
    @DisplayName("Cuerpos de error del folio: 400 con errores por campo, 401, 403, 404 y 409")
    void cuerposDeErrorDelFolio() throws Exception {
        post(RESERVA_INEXISTENTE + "/pagos", "{\"valor\":-5,\"medio\":\"TARJETA_DEBITO_CREDITO\"}")
                .andExpect(CuerpoError.validacion(RESERVA_INEXISTENTE + "/pagos",
                        Map.of("valor", "El valor del pago debe ser mayor que cero")));

        mvc.perform(MockMvcRequestBuilders.get(RESERVA_INEXISTENTE).with(anonymous()))
                .andExpect(CuerpoError.error(HttpStatus.UNAUTHORIZED, CuerpoError.MENSAJE_401, RESERVA_INEXISTENTE));

        mvc.perform(MockMvcRequestBuilders.put(RESERVA_INEXISTENTE + "/cerrar")
                        .with(jwt().authorities(new SimpleGrantedAuthority("HUESPED"))))
                .andExpect(CuerpoError.error(HttpStatus.FORBIDDEN, CuerpoError.MENSAJE_403,
                        RESERVA_INEXISTENTE + "/cerrar"));

        post(RESERVA_INEXISTENTE + "/pagos", pago(100_000))
                .andExpect(CuerpoError.error(HttpStatus.NOT_FOUND,
                        "No se encontró la reserva con código: RES-2026-99999", RESERVA_INEXISTENTE + "/pagos"));

        String folio = "/api/reservas/" + codigo(crearReserva(LocalDate.now(BOGOTA).plusDays(195))) + "/folio";
        post(folio + "/cargos", cargo("SRV-SPA"))
                .andExpect(CuerpoError.error(HttpStatus.CONFLICT,
                        startsWith("L-17: El alojamiento no ofrece el servicio adicional SRV-SPA"), folio + "/cargos"));
    }

    private void cerrarConAutorizacion(String folio) throws Exception {
        post(folio + "/autorizacion-cierre", "{\"autorizadoPor\":\"Administradora Marta\","
                + "\"motivo\":\"Cierre de prueba\"}").andExpect(status().isOk());
    }

    private static String cargo(String idServicio) {
        return "{\"idServicio\":\"" + idServicio + "\"}";
    }

    private static String correccion(int indiceCargo, String motivo) {
        return "{\"indiceCargo\":" + indiceCargo + ",\"motivo\":\"" + motivo + "\"}";
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

    private ResultActions get(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url));
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