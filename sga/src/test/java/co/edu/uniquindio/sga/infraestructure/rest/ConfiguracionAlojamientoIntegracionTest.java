package co.edu.uniquindio.sga.infraestructure.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de la configuración del alojamiento: CU-28 (alojamiento), CU-29 (temporadas) y CU-30 (política de
 * cancelación versionada), por HTTP contra la aplicación completa sobre H2 con los datos iniciales del Coral:
 * temporadas Base, Media y Alta (de 2026 al 20 de enero de 2027), cinco apartamentos activos (APT-101, APT-102,
 * APT-201, APT-202 y APT-301) y la política versión 1 de application.properties.
 * Usa su propia base H2 (sga-configuracion) y las pruebas que cambian datos dejan el contexto sucio, para que las
 * demás pruebas sigan viendo el alojamiento, las temporadas y la política iniciales.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-configuracion")
@AutoConfigureMockMvc
class ConfiguracionAlojamientoIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Temporada de festival del 1 al 10 de marzo de 2027 (fecha final excluida): no choca con Media ni Alta. */
    private static final String RANGO_FESTIVAL = "{\"fechaInicio\":\"2027-03-01\",\"fechaFin\":\"2027-03-11\"}";

    @Autowired
    private MockMvc mvc;

    // ==================== CU-28: alojamiento ====================

    @Test
    @DisplayName("CU-28: GET devuelve datos, ubicación, horario, normas, servicios y parámetros vigentes")
    void consultarAlojamiento() throws Exception {
        get("/api/alojamiento")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ALO-CORAL"))
                .andExpect(jsonPath("$.nombre").value("Coral"))
                .andExpect(jsonPath("$.ciudad").value("Cartagena de Indias"))
                .andExpect(jsonPath("$.latitud").value(10.3975))
                .andExpect(jsonPath("$.longitud").value(-75.5590))
                .andExpect(jsonPath("$.horaEntrada", startsWith("15:00")))
                .andExpect(jsonPath("$.horaSalida", startsWith("11:00")))
                .andExpect(jsonPath("$.normaConvivencia", startsWith("Silencio estricto")))
                .andExpect(jsonPath("$.serviciosAdicionales", hasSize(3)))
                .andExpect(jsonPath("$.serviciosAdicionales[*].id", hasItem("SRV-RECEPCION-NOCTURNA")))
                .andExpect(jsonPath("$.parametros.umbralEdadFacturable").value(6))
                .andExpect(jsonPath("$.parametros.tiempoPreparacionHoras").value(3))
                .andExpect(jsonPath("$.parametros.plazoConfirmacionHoras").value(12))
                .andExpect(jsonPath("$.parametros.horaLimiteNoShow", startsWith("22:00")))
                .andExpect(jsonPath("$.parametros.estanciaMinimaNoches").value(2))
                .andExpect(jsonPath("$.parametros.porcentajeAnticipo").value(50))
                .andExpect(jsonPath("$.parametros.cargoAseoMascota").value(110000));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-28: PUT actualiza datos, ubicación, normas y horario; los parámetros no cambian")
    void actualizarAlojamiento() throws Exception {
        put("/api/alojamiento", alojamiento("Coral Bocagrande", 10.40, "\"14:00\"", "\"10:00\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Coral Bocagrande"))
                .andExpect(jsonPath("$.latitud").value(10.40))
                .andExpect(jsonPath("$.normaConvivencia").value("Silencio desde las 22:00"))
                .andExpect(jsonPath("$.horaEntrada", startsWith("14:00")))
                .andExpect(jsonPath("$.horaSalida", startsWith("10:00")));

        get("/api/alojamiento")
                .andExpect(jsonPath("$.nombre").value("Coral Bocagrande"))
                .andExpect(jsonPath("$.horaEntrada", startsWith("14:00")))
                .andExpect(jsonPath("$.parametros.porcentajeAnticipo").value(50))
                .andExpect(jsonPath("$.serviciosAdicionales", hasSize(3)));
    }

    @Test
    @DisplayName("CU-28: sin horario se conserva el vigente; 409 por coordenada fuera de rango o media hora (L-12)")
    void actualizarAlojamientoConErrores() throws Exception {
        put("/api/alojamiento", alojamiento("Coral", 95, "null", "null"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.3")));
        put("/api/alojamiento", alojamiento("Coral", 10.40, "\"14:00\"", "null"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("L-12")));
        put("/api/alojamiento", alojamiento(" ", 10.40, "null", "null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        get("/api/alojamiento")
                .andExpect(jsonPath("$.nombre").value("Coral"))
                .andExpect(jsonPath("$.horaEntrada", startsWith("15:00")));
    }

    // ==================== CU-29: temporadas ====================

    @Test
    @DisplayName("CU-29: GET lista las temporadas iniciales con sus rangos, 10 por página, y consulta una por su id")
    void listarTemporadas() throws Exception {
        get("/api/temporadas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[*].id", hasItems("T-BASE", "T-MEDIA", "T-ALTA")))
                .andExpect(jsonPath("$.contenido[?(@.id == 'T-BASE')].rangos[*]", hasSize(0)))
                .andExpect(jsonPath("$.contenido[?(@.id == 'T-ALTA')].rangos[*]", hasSize(5)));
        get("/api/temporadas?pagina=-1").andExpect(status().isBadRequest());
        get("/api/temporadas/T-ALTA").andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ALTA"))
                .andExpect(jsonPath("$.rangos", hasSize(5)));
        get("/api/temporadas/T-NO-EXISTE").andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-29: con la tarifa de todos los apartamentos activos responde 201 con Location y cada uno la "
            + "recibe (7.4)")
    void crearTemporadaConTarifasCompletas() throws Exception {
        ResultActions creada = post("/api/temporadas", temporada("T-FESTIVAL", "MEDIA", "[" + RANGO_FESTIVAL + "]",
                tarifas("APT-101", "APT-102", "APT-201", "APT-202", "APT-301")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/temporadas/T-FESTIVAL")))
                .andExpect(jsonPath("$.id").value("T-FESTIVAL"))
                .andExpect(jsonPath("$.tipo").value("MEDIA"))
                .andExpect(jsonPath("$.rangos[0].fechaInicio").value("2027-03-01"))
                .andExpect(jsonPath("$.rangos[0].fechaFin").value("2027-03-11"));

        get(creada.andReturn().getResponse().getHeader("Location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("T-FESTIVAL"))
                .andExpect(jsonPath("$.rangos", hasSize(1)));
        get("/api/temporadas").andExpect(jsonPath("$.totalElementos").value(4));
        get("/api/apartamentos/APT-101")
                .andExpect(jsonPath("$.tarifas[*].idTemporada", hasItem("T-FESTIVAL")));
        get("/api/apartamentos/APT-101/tarifas/historico")
                .andExpect(jsonPath("$.contenido[?(@.idTemporada == 'T-FESTIVAL')].vigenteDesde",
                        hasItem(LocalDate.now(BOGOTA).toString())));
    }

    @Test
    @DisplayName("CU-29: 409 si falta la tarifa de un apartamento activo; no se crea la temporada (7.4)")
    void crearTemporadaSinTodasLasTarifas() throws Exception {
        post("/api/temporadas", temporada("T-FESTIVAL", "MEDIA", "[" + RANGO_FESTIVAL + "]",
                tarifas("APT-101", "APT-102", "APT-201", "APT-202")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.4")))
                .andExpect(jsonPath("$.mensaje", containsString("APT-301")));

        get("/api/temporadas")
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem("T-FESTIVAL"))));
        get("/api/apartamentos/APT-101")
                .andExpect(jsonPath("$.tarifas[*].idTemporada", not(hasItem("T-FESTIVAL"))));
    }

    @Test
    @DisplayName("CU-29: 409 si la temporada se solapa con otra (7.4) o si sería una segunda base (F-05)")
    void crearTemporadaSolapadaOSegundaBase() throws Exception {
        String todas = tarifas("APT-101", "APT-102", "APT-201", "APT-202", "APT-301");
        post("/api/temporadas", temporada("T-NAVIDAD", "ALTA",
                "[{\"fechaInicio\":\"2026-12-20\",\"fechaFin\":\"2026-12-26\"}]", todas))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.4")))
                .andExpect(jsonPath("$.mensaje", containsString("se solapa")));

        post("/api/temporadas", temporada("T-BASE-2", "BASE", "[]", todas))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("F-05")));

        get("/api/temporadas").andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    @DisplayName("CU-29: 400 por tipo inexistente o faltan datos de forma")
    void crearTemporadaConFormaInvalida() throws Exception {
        post("/api/temporadas", temporada("T-X", "BAJA", "[]", "[]")).andExpect(status().isBadRequest());
        post("/api/temporadas", "{\"nombre\":\"Sin id\",\"tipo\":\"MEDIA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.id").exists());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-29: POST rangos agrega un rango libre (201 con Location); 409 si se solapa con otra temporada; "
            + "404 si no existe")
    void agregarRango() throws Exception {
        post("/api/temporadas/T-MEDIA/rangos", "{\"fechaInicio\":\"2027-02-01\",\"fechaFin\":\"2027-02-05\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/temporadas/T-MEDIA")))
                .andExpect(jsonPath("$.id").value("T-MEDIA"))
                .andExpect(jsonPath("$.rangos", hasSize(8)))
                .andExpect(jsonPath("$.rangos[7].fechaInicio").value("2027-02-01"));

        post("/api/temporadas/T-MEDIA/rangos", "{\"fechaInicio\":\"2026-12-20\",\"fechaFin\":\"2026-12-26\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("7.4")));
        post("/api/temporadas/T-BASE/rangos", RANGO_FESTIVAL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("F-05")));
        post("/api/temporadas/T-NO-EXISTE/rangos", RANGO_FESTIVAL).andExpect(status().isNotFound());

        get("/api/temporadas").andExpect(jsonPath("$.contenido[?(@.id == 'T-MEDIA')].rangos[*]", hasSize(8)));
        get("/api/temporadas/T-MEDIA").andExpect(status().isOk()).andExpect(jsonPath("$.rangos", hasSize(8)));
    }

    // ==================== CU-30: política de cancelación versionada ====================

    @Test
    @DisplayName("CU-30: al arrancar la versión 1 sale de application.properties y es la única")
    void politicaInicial() throws Exception {
        get("/api/politica-cancelacion")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.vigenteDesde").value("2026-01-01"))
                .andExpect(jsonPath("$.tramos", hasSize(3)))
                .andExpect(jsonPath("$.tramos[0].antelacionMinimaDias").value(16))
                .andExpect(jsonPath("$.tramos[2].antelacionMinimaDias").value(0))
                .andExpect(jsonPath("$.tramos[2].porcentajeRetencion").value(50));
        get("/api/politica-cancelacion/versiones")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("CU-30: actualizar crea la versión 2; una reserva nueva queda con la 2 y una existente sigue con la 1")
    void actualizarPoliticaCreaVersion2() throws Exception {
        String existente = codigoDeReserva("/api/reservas?estado=CONFIRMADA");

        put("/api/politica-cancelacion", politica("""
                [{"antelacionMinimaDias":20,"porcentajeRetencion":10},
                 {"antelacionMinimaDias":10,"porcentajeRetencion":30},
                 {"antelacionMinimaDias":0,"porcentajeRetencion":60}]"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.vigenteDesde").value(LocalDate.now(BOGOTA).toString()))
                .andExpect(jsonPath("$.consecuenciaNoShow").value("Se retiene el 60 % del valor total"))
                .andExpect(jsonPath("$.tramos[0].antelacionMinimaDias").value(20));

        get("/api/politica-cancelacion").andExpect(jsonPath("$.version").value(2));
        get("/api/politica-cancelacion/versiones")
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].version").value(2))
                .andExpect(jsonPath("$.contenido[1].version").value(1))
                .andExpect(jsonPath("$.contenido[1].tramos[2].porcentajeRetencion").value(50));

        // RN-22: la reserva que ya existía conserva la versión que congeló al crearse
        get("/api/reservas/" + existente).andExpect(jsonPath("$.versionPoliticaCancelacion").value(1));

        // Una reserva nueva congela la versión vigente: la 2
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(40);
        post("/api/reservas", reserva("APT-301", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionPoliticaCancelacion").value(2));
    }

    @Test
    @DisplayName("CU-30: 409 con un solo tramo o sin tramo desde 0 días (L-10); la versión 1 sigue vigente")
    void actualizarPoliticaQueNoCumpleL10() throws Exception {
        put("/api/politica-cancelacion", politica("[{\"antelacionMinimaDias\":0,\"porcentajeRetencion\":50}]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("L-10")));
        put("/api/politica-cancelacion", politica("""
                [{"antelacionMinimaDias":15,"porcentajeRetencion":0},
                 {"antelacionMinimaDias":5,"porcentajeRetencion":50}]"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("L-10")));
        put("/api/politica-cancelacion", "{\"consecuenciaNoShow\":\"Retiene 50 %\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tramos").exists());

        get("/api/politica-cancelacion").andExpect(jsonPath("$.version").value(1));
        get("/api/politica-cancelacion/versiones").andExpect(jsonPath("$.totalElementos").value(1));
    }

    // ==================== Auxiliares ====================

    private static String alojamiento(String nombre, double latitud, String horaEntrada, String horaSalida) {
        return """
                {"nombre":"%s","descripcion":"Apartamentos frente al mar","ciudad":"Cartagena de Indias",
                 "direccion":"Avenida del Malecón","latitud":%s,"longitud":-75.5590,
                 "normaConvivencia":"Silencio desde las 22:00","horaEntrada":%s,"horaSalida":%s}
                """.formatted(nombre, latitud, horaEntrada, horaSalida);
    }

    private static String temporada(String id, String tipo, String rangos, String tarifas) {
        return """
                {"id":"%s","nombre":"Temporada %s","tipo":"%s","rangos":%s,"tarifas":%s}
                """.formatted(id, id, tipo, rangos, tarifas);
    }

    /** La misma tarifa de 450.000 por ocupante para cada apartamento dado. */
    private static String tarifas(String... apartamentos) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < apartamentos.length; i++) {
            if (i > 0) json.append(',');
            json.append("{\"identificacionApartamento\":\"").append(apartamentos[i])
                    .append("\",\"valorPorOcupante\":450000}");
        }
        return json.append(']').toString();
    }

    private static String politica(String tramos) {
        return """
                {"consecuenciaNoShow":"Se retiene el 60 %% del valor total","tramos":%s}
                """.formatted(tramos);
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

    private String codigoDeReserva(String listado) throws Exception {
        String json = get(listado).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.contenido[0].codigo");
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
