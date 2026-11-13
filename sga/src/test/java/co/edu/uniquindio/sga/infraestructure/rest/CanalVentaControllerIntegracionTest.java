package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Canales de venta (10.3.1, 7.8): el administrador los crea, regenera su credencial y los desactiva, y revisa los
 * conflictos (RN-18) y la bitácora. Cada canal se autentica en sus tres operaciones con su propia credencial
 * (X-Canal-Credencial): sin ella o con una incorrecta, 401; con el canal inactivo, 403; y el intento queda en la
 * bitácora. A propósito no importa SeguridadPruebasConfig: el JWT de administrador va solo donde corresponde.
 * Cada prueba usa canales propios y APT-102 a más de 250 días, fechas que no usa ninguna otra prueba (la base H2 en
 * memoria es la misma para todos los contextos de Spring).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CanalVentaControllerIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String ADMIN = "admin@sga.local";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReservaRepository reservas;

    // ---------------------------------------------------------------- canales

    @Test
    @DisplayName("10.3.1: crear un canal responde 201 con la credencial una sola vez; el listado nunca la muestra")
    void crearMuestraLaCredencialUnaSolaVez() throws Exception {
        admin(MockMvcRequestBuilders.post("/api/canales").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"expedia-cv1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("expedia-cv1"))
                .andExpect(jsonPath("$.tipo").value("EXTERNO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.credencial", hasLength(43)));

        String listado = admin(MockMvcRequestBuilders.get("/api/canales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido[*].nombre", hasItem("booking")))
                .andReturn().getResponse().getContentAsString();
        assertEquals(List.of(), JsonPath.read(listado, "$.contenido[?(@.credencial)]"));
        assertEquals(List.of(), JsonPath.read(listado, "$.contenido[?(@.hashCredencial)]"));
    }

    @Test
    @DisplayName("10.3.1: nombre repetido o reservado 409, nombre con formato inválido 400")
    void crearConNombreInvalido() throws Exception {
        crearCanal("vrbo-cv2");
        crear("{\"nombre\":\"vrbo-cv2\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("10.3.1: Ya existe un canal llamado vrbo-cv2"));
        crear("{\"nombre\":\"eventos\"}").andExpect(status().isConflict());
        crear("{\"nombre\":\"Mi Canal\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    @DisplayName("Solo el administrador gestiona los canales: sin token 401, recepción 403")
    void soloElAdministrador() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/canales")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.get("/api/canales/conflictos").with(rol("RECEPCIONISTA")))
                .andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.get("/api/canales/eventos").with(rol("RECEPCIONISTA")))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- credencial del canal

    @Test
    @DisplayName("401: credencial incorrecta o de otro canal, con ErrorResponse, y el intento queda en la bitácora")
    void credencialIncorrectaResponde401() throws Exception {
        crearCanal("canal-cv3");
        String deOtroCanal = crearCanal("otro-cv3");

        disponibilidad("canal-cv3", "no-es-la-credencial")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("La credencial no es válida para el canal canal-cv3"));
        mvc.perform(MockMvcRequestBuilders.put("/api/canales/canal-cv3/reservas/BK-CV3/cancelar")
                        .header("X-Canal-Credencial", deOtroCanal))
                .andExpect(status().isUnauthorized());

        admin(MockMvcRequestBuilders.get("/api/canales/eventos").param("canal", "canal-cv3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[*].resultado", everyItem(is("RECHAZADO_CREDENCIAL"))))
                .andExpect(jsonPath("$.contenido[*].tipoEvento", hasItem("CANCELACION_RESERVA")))
                .andExpect(jsonPath("$.contenido[*].identificadorExterno", hasItem("BK-CV3")));
    }

    @Test
    @DisplayName("403: el canal desactivado no entra aunque su credencial sea correcta, y queda en la bitácora")
    void canalInactivoResponde403() throws Exception {
        String credencial = crearCanal("canal-cv4");
        disponibilidad("canal-cv4", credencial).andExpect(status().isOk());

        admin(MockMvcRequestBuilders.put("/api/canales/canal-cv4/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.credencial").doesNotExist());

        disponibilidad("canal-cv4", credencial)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El canal canal-cv4 está inactivo"));
        admin(MockMvcRequestBuilders.get("/api/canales/eventos").param("canal", "canal-cv4"))
                .andExpect(jsonPath("$.contenido[0].resultado").value("RECHAZADO_CANAL_INACTIVO"));
        // Eliminación lógica: sigue en la base, no se desactiva dos veces ni recibe credencial nueva
        admin(MockMvcRequestBuilders.put("/api/canales/canal-cv4/desactivar")).andExpect(status().isConflict());
        admin(MockMvcRequestBuilders.put("/api/canales/canal-cv4/regenerar-credencial"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("10.3.1: al regenerar la credencial la anterior responde 401 y la nueva funciona")
    void regenerarInvalidaLaAnterior() throws Exception {
        String anterior = crearCanal("canal-cv5");

        String cuerpo = admin(MockMvcRequestBuilders.put("/api/canales/canal-cv5/regenerar-credencial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credencial", hasLength(43)))
                .andReturn().getResponse().getContentAsString();
        String nueva = JsonPath.read(cuerpo, "$.credencial");

        disponibilidad("canal-cv5", anterior).andExpect(status().isUnauthorized());
        disponibilidad("canal-cv5", nueva).andExpect(status().isOk());
    }

    @Test
    @DisplayName("404: regenerar o desactivar un canal que no existe")
    void canalQueNoExiste() throws Exception {
        admin(MockMvcRequestBuilders.put("/api/canales/no-existe-cv/regenerar-credencial"))
                .andExpect(status().isNotFound());
        admin(MockMvcRequestBuilders.put("/api/canales/no-existe-cv/desactivar")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- conflictos y bitácora

    @Test
    @DisplayName("RN-18: resolver un conflicto guarda el motivo y quién lo resolvió, y nunca toca la reserva vigente")
    void resolverConflicto() throws Exception {
        String credencial = crearCanal("canal-cv6");
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(250);
        reservar("canal-cv6", credencial, "CV6-VIGENTE", entrada, entrada.plusDays(3))
                .andExpect(status().isCreated());
        reservar("canal-cv6", credencial, "CV6-CHOQUE", entrada.plusDays(1), entrada.plusDays(2))
                .andExpect(status().isConflict());
        Reserva vigente = reservas.buscarPorCanalEIdentificadorExterno("canal-cv6", "CV6-VIGENTE")
                .orElseThrow();

        String pendientes = admin(MockMvcRequestBuilders.get("/api/canales/conflictos").param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(pendientes, "$.contenido[?(@.identificadorExterno == 'CV6-CHOQUE')].id");
        assertEquals(1, ids.size());
        String id = ids.get(0);

        resolver(id, "{\"motivo\":\" \"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
        resolver(id, "{\"motivo\":\"El huésped aceptó otras fechas por el canal\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.canal").value("canal-cv6"))
                .andExpect(jsonPath("$.motivoResolucion").value("El huésped aceptó otras fechas por el canal"))
                .andExpect(jsonPath("$.resueltoPor").value(ADMIN))
                .andExpect(jsonPath("$.fechaResolucion").value(LocalDate.now(BOGOTA).toString()));
        resolver(id, "{\"motivo\":\"Otra vez\"}").andExpect(status().isConflict());
        resolver("CNF-NO-EXISTE", "{\"motivo\":\"x\"}").andExpect(status().isNotFound());

        admin(MockMvcRequestBuilders.get("/api/canales/conflictos").param("estado", "RESUELTO"))
                .andExpect(jsonPath("$.contenido[*].id", hasItem(id)));
        Reserva despues = reservas.buscarPorCodigo(vigente.getCodigo()).orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE, despues.getEstado());
        assertEquals(new Estancia(entrada, entrada.plusDays(3)), despues.getEstancia());
    }

    @Test
    @DisplayName("7.8: la bitácora se filtra por canal y por fechas; desde posterior a hasta responde 409")
    void bitacoraFiltrable() throws Exception {
        String credencial = crearCanal("canal-cv7");
        disponibilidad("canal-cv7", credencial).andExpect(status().isOk());
        String hoy = LocalDate.now(BOGOTA).toString();

        admin(MockMvcRequestBuilders.get("/api/canales/eventos").param("canal", "canal-cv7")
                .param("desde", hoy).param("hasta", hoy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].canal").value("canal-cv7"))
                .andExpect(jsonPath("$.contenido[0].tipoEvento").value("CONSULTA_DISPONIBILIDAD"))
                .andExpect(jsonPath("$.contenido[0].resultado").value("ACEPTADO"));
        admin(MockMvcRequestBuilders.get("/api/canales/eventos").param("canal", "canal-cv7")
                .param("desde", LocalDate.now(BOGOTA).plusDays(1).toString()))
                .andExpect(jsonPath("$.totalElementos").value(0));
        admin(MockMvcRequestBuilders.get("/api/canales/eventos").param("desde", hoy)
                .param("hasta", LocalDate.now(BOGOTA).minusDays(1).toString()))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- apoyo

    @Test
    @DisplayName("Cuerpos de error de /api/canales: 400 con errores por campo, 401, 403, 404 y 409")
    void cuerposDeErrorDeCanales() throws Exception {
        crear("{}").andExpect(CuerpoError.validacion("/api/canales",
                Map.of("nombre", "El nombre del canal es obligatorio")));

        mvc.perform(MockMvcRequestBuilders.get("/api/canales"))
                .andExpect(CuerpoError.error(HttpStatus.UNAUTHORIZED, CuerpoError.MENSAJE_401, "/api/canales"));

        mvc.perform(MockMvcRequestBuilders.get("/api/canales").with(rol("RECEPCIONISTA")))
                .andExpect(CuerpoError.error(HttpStatus.FORBIDDEN, CuerpoError.MENSAJE_403, "/api/canales"));

        admin(MockMvcRequestBuilders.put("/api/canales/no-existe-err/desactivar"))
                .andExpect(CuerpoError.error(HttpStatus.NOT_FOUND, "No se encontró el canal de venta: no-existe-err",
                        "/api/canales/no-existe-err/desactivar"));

        crearCanal("canal-err");
        crear("{\"nombre\":\"canal-err\"}")
                .andExpect(CuerpoError.error(HttpStatus.CONFLICT, "10.3.1: Ya existe un canal llamado canal-err",
                        "/api/canales"));
    }

    private static JwtRequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority(rol)).jwt(token -> token.subject(ADMIN));
    }

    private ResultActions admin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion)
            throws Exception {
        return mvc.perform(peticion.with(rol("ADMINISTRADOR")));
    }

    private ResultActions crear(String json) throws Exception {
        return admin(MockMvcRequestBuilders.post("/api/canales").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Crea el canal y devuelve su credencial, que solo se muestra en esta respuesta. */
    private String crearCanal(String nombre) throws Exception {
        String cuerpo = crear("{\"nombre\":\"" + nombre + "\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.credencial");
    }

    private ResultActions resolver(String id, String json) throws Exception {
        return admin(MockMvcRequestBuilders.put("/api/canales/conflictos/{id}/resolver", id)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Las operaciones del canal no llevan JWT: solo la credencial del canal. */
    private ResultActions disponibilidad(String canal, String credencial) throws Exception {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(260);
        return mvc.perform(MockMvcRequestBuilders.get("/api/canales/{canal}/disponibilidad", canal)
                .param("entrada", entrada.toString()).param("salida", entrada.plusDays(2).toString())
                .param("ocupantes", "1")
                .header("X-Canal-Credencial", credencial));
    }

    private ResultActions reservar(String canal, String credencial, String idExterno, LocalDate entrada,
                                   LocalDate salida) throws Exception {
        String json = """
                {"identificadorExterno":"%s","identificacionApartamento":"APT-102","fechaEntrada":"%s",
                 "fechaSalida":"%s",
                 "titular":{"nombre":"Ana Torres","tipoDocumento":"PASAPORTE","numeroDocumento":"Y7654321",
                            "email":"ana@correo.com","telefono":"3101234567"},
                 "ocupantes":[{"nombre":"Ana Torres","tipoDocumento":"PASAPORTE","numeroDocumento":"Y7654321",
                               "fechaNacimiento":"1990-06-01"}]}
                """.formatted(idExterno, entrada, salida);
        return mvc.perform(MockMvcRequestBuilders.post("/api/canales/{canal}/reservas", canal)
                .header("X-Canal-Credencial", credencial)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
