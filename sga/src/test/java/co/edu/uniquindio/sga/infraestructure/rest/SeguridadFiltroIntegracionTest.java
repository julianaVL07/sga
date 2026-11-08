package co.edu.uniquindio.sga.infraestructure.rest;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Errores de la cadena de filtros (7.5.6), canal por rol al crear una reserva (7.5.3) y CORS del frontend (Guía 12).
 * A propósito no importa SeguridadPruebasConfig: cada caso decide si lleva token y con qué rol y documento.
 * Usa su propia base H2 para que la reserva creada no choque con las de otras pruebas.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-seguridad-filtro")
@AutoConfigureMockMvc
class SeguridadFiltroIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String DOCUMENTO_HUESPED_1 = "1094777888";
    private static final String DOCUMENTO_HUESPED_2 = "1094333444";
    private static final String DOCUMENTO_RECEPCION = "1000000002";
    private static final String MENSAJE_401 = "Debe iniciar sesión o el token no es válido";
    private static final String MENSAJE_403 = "No tiene permiso para realizar esta operación";
    private static final String ORIGEN_FRONTEND = "http://localhost:5173";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Sin token: 401 con el ErrorResponse y WWW-Authenticate: Bearer")
    void sinTokenResponde401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_401))
                .andExpect(jsonPath("$.path").value("/api/reservas"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Token firmado con otra clave: 401 con el ErrorResponse y WWW-Authenticate: Bearer")
    void tokenMalFirmadoResponde401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFirmadoConOtraClave()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_401))
                .andExpect(jsonPath("$.path").value("/api/reservas"));
    }

    @Test
    @DisplayName("Huésped crea una reserva DIRECTO: 403")
    void huespedCreaDirectoResponde403() throws Exception {
        crear(reserva("DIRECTO", DOCUMENTO_HUESPED_1), usuario("HUESPED", DOCUMENTO_HUESPED_1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    @Test
    @DisplayName("Huésped crea una reserva PORTAL a nombre de otro documento: 403")
    void huespedCreaPortalAjenaResponde403() throws Exception {
        crear(reserva("PORTAL", DOCUMENTO_HUESPED_2), usuario("HUESPED", DOCUMENTO_HUESPED_1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    @Test
    @DisplayName("Huésped crea una reserva PORTAL a su nombre: 201")
    void huespedCreaPortalPropiaResponde201() throws Exception {
        crear(reserva("PORTAL", DOCUMENTO_HUESPED_1), usuario("HUESPED", DOCUMENTO_HUESPED_1))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.canalOrigen").value("PORTAL"));
    }

    @Test
    @DisplayName("Recepción crea una reserva PORTAL: 403")
    void recepcionCreaPortalResponde403() throws Exception {
        crear(reserva("PORTAL", DOCUMENTO_HUESPED_1), usuario("RECEPCIONISTA", DOCUMENTO_RECEPCION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    @Test
    @DisplayName("Preflight OPTIONS desde el frontend: responde con Access-Control-Allow-Origin")
    void preflightDesdeElFrontendPermiteElOrigen() throws Exception {
        mvc.perform(MockMvcRequestBuilders.options("/api/reservas")
                        .header(HttpHeaders.ORIGIN, ORIGEN_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONTEND));
    }

    private ResultActions crear(String cuerpo, JwtRequestPostProcessor usuario) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .with(usuario));
    }

    private static JwtRequestPostProcessor usuario(String rol, String documento) {
        return jwt().jwt(token -> token.claim("documento", documento))
                .authorities(new SimpleGrantedAuthority(rol));
    }

    /** Reserva válida a 90 días, de lunes a jueves para no tocar la estancia mínima de fin de semana (RP-01) */
    private static String reserva(String canal, String documentoTitular) {
        LocalDate entrada = LocalDate.now(BOGOTA).plusDays(90);
        while (entrada.getDayOfWeek() != DayOfWeek.MONDAY) {
            entrada = entrada.plusDays(1);
        }
        return """
                {"identificacionApartamento":"APT-101","fechaEntrada":"%s","fechaSalida":"%s",
                 "horaEstimadaLlegada":"16:00","canalOrigen":"%s",
                 "titular":{"nombre":"Laura Gomez","tipoDocumento":"CC","numeroDocumento":"%s",
                            "email":"huesped1@sga.local","telefono":"3135550404"},
                 "ocupantes":[{"nombre":"Laura Gomez","tipoDocumento":"CC","numeroDocumento":"%s",
                               "fechaNacimiento":"1995-01-30"}]}
                """.formatted(entrada, entrada.plusDays(2), canal, documentoTitular, documentoTitular);
    }

    /** Token con emisor y vigencia correctos, pero firmado con una clave que no es la del SGA */
    private static String tokenFirmadoConOtraClave() {
        byte[] otraClave = (UUID.randomUUID() + "" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(otraClave, "HmacSHA256")));
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sga")
                .subject("recepcion@sga.local")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(30, ChronoUnit.MINUTES))
                .claim("roles", List.of("RECEPCIONISTA"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
