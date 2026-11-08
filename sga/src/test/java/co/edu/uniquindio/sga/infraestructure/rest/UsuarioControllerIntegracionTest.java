package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestión de usuarios, perfil propio y cambio de contraseña (Guía 12, 7.2). A propósito no importa
 * SeguridadPruebasConfig: cada caso lleva el rol y el sub (correo) de su usuario. Usa su propia base H2 y cada prueba
 * crea sus usuarios con un correo y un documento propios, para no depender del orden de ejecución.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-usuarios")
@AutoConfigureMockMvc
class UsuarioControllerIntegracionTest {

    private static final String CORREO_ADMIN = "admin@sga.local";
    private static final String CORREO_HUESPED_1 = "huesped1@sga.local";
    private static final String CONTRASENA = "Prueba2026";
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioJpaDataRepository usuarios;

    @Test
    @DisplayName("El administrador crea un recepcionista: 201 con Location y sin la contraseña")
    void adminCreaRecepcionistaResponde201() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        MvcResult resultado = mvc.perform(como(MockMvcRequestBuilders.post("/api/usuarios"), "ADMINISTRADOR", CORREO_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario(correo(n), documento(n), "RECEPCIONISTA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.correo").value(correo(n)))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andReturn();
        Integer id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

        assertTrue(resultado.getResponse().getHeader(HttpHeaders.LOCATION).endsWith("/api/usuarios/" + id));
    }

    @Test
    @DisplayName("Un huésped intenta crear un usuario: 403")
    void huespedCreaUsuarioResponde403() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        mvc.perform(como(MockMvcRequestBuilders.post("/api/usuarios"), "HUESPED", CORREO_HUESPED_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario(correo(n), documento(n), "ADMINISTRADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permiso para realizar esta operación"));
    }

    @Test
    @DisplayName("El administrador crea un usuario con un correo repetido: 409")
    void correoRepetidoResponde409() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        crear(correo(n), documento(n), "RECEPCIONISTA");

        mvc.perform(como(MockMvcRequestBuilders.post("/api/usuarios"), "ADMINISTRADOR", CORREO_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario(correo(n), documento(n + 1000), "RECEPCIONISTA")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un usuario registrado con ese correo o documento"));
    }

    @Test
    @DisplayName("El administrador cambia el rol de un usuario: 200")
    void cambiarRolResponde200() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        long id = crear(correo(n), documento(n), "HUESPED");

        mvc.perform(como(MockMvcRequestBuilders.put("/api/usuarios/{id}/rol", id), "ADMINISTRADOR", CORREO_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"PERSONAL_SERVICIO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.rol").value("PERSONAL_SERVICIO"));
    }

    @Test
    @DisplayName("Desactivar al titular de una reserva PENDIENTE: 409 y sigue activo")
    void desactivarTitularDeReservaPendienteResponde409() throws Exception {
        long id = usuarios.findByCorreo(CORREO_HUESPED_1).orElseThrow().getId();

        mvc.perform(como(MockMvcRequestBuilders.put("/api/usuarios/{id}/desactivar", id), "ADMINISTRADOR", CORREO_ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede desactivar: el usuario es titular de reservas PENDIENTE, CONFIRMADA o EN_CURSO"));
        mvc.perform(como(MockMvcRequestBuilders.get("/api/usuarios/{id}", id), "ADMINISTRADOR", CORREO_ADMIN))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("Desactivar a un usuario sin reservas vigentes: 200, y después su login responde 401")
    void desactivarSinReservasYLoginResponde401() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        long id = crear(correo(n), documento(n), "HUESPED");
        login(correo(n), CONTRASENA).andExpect(status().isOk());

        mvc.perform(como(MockMvcRequestBuilders.put("/api/usuarios/{id}/desactivar", id), "ADMINISTRADOR", CORREO_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        login(correo(n), CONTRASENA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("GET /api/usuarios/me devuelve el usuario del token")
    void meDevuelveElUsuarioDelToken() throws Exception {
        mvc.perform(como(MockMvcRequestBuilders.get("/api/usuarios/me"), "RECEPCIONISTA", "recepcion@sga.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("recepcion@sga.local"))
                .andExpect(jsonPath("$.documento").value("1000000002"))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/usuarios/me actualiza nombre, teléfono y foto, pero no el correo")
    void actualizarPerfilNoCambiaElCorreo() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        crear(correo(n), documento(n), "HUESPED");
        String perfil = """
                {"nombre":"Nombre Nuevo","telefono":"3009998877","fotoUrl":"https://fotos.sga.local/perfil.png",
                 "correo":"otro@prueba.co"}
                """;

        mvc.perform(como(MockMvcRequestBuilders.put("/api/usuarios/me"), "HUESPED", correo(n))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfil))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Nuevo"))
                .andExpect(jsonPath("$.telefono").value("3009998877"))
                .andExpect(jsonPath("$.fotoUrl").value("https://fotos.sga.local/perfil.png"))
                .andExpect(jsonPath("$.correo").value(correo(n)));
        mvc.perform(como(MockMvcRequestBuilders.get("/api/usuarios/me"), "HUESPED", correo(n)))
                .andExpect(jsonPath("$.correo").value(correo(n)));
    }

    @Test
    @DisplayName("Cambio de contraseña: con la actual errada 400, con la correcta 204 y el login con la nueva 200")
    void cambiarContrasena() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        crear(correo(n), documento(n), "HUESPED");

        cambiarContrasena(correo(n), "Errada2026", "Nueva2027")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta"));
        cambiarContrasena(correo(n), CONTRASENA, "Nueva2027")
                .andExpect(status().isNoContent());

        login(correo(n), "Nueva2027").andExpect(status().isOk());
        login(correo(n), CONTRASENA).andExpect(status().isUnauthorized());
    }

    private ResultActions cambiarContrasena(String correo, String actual, String nueva) throws Exception {
        return mvc.perform(como(MockMvcRequestBuilders.put("/api/usuarios/me/contrasena"), "HUESPED", correo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}"));
    }

    private ResultActions login(String correo, String contrasena) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }

    /** Crea el usuario como administrador y devuelve su id */
    private long crear(String correo, String documento, String rol) throws Exception {
        String cuerpo = mvc.perform(como(MockMvcRequestBuilders.post("/api/usuarios"), "ADMINISTRADOR", CORREO_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuario(correo, documento, rol)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, String rol, String correo) {
        return peticion.with(token(rol, correo));
    }

    private static JwtRequestPostProcessor token(String rol, String correo) {
        return jwt().jwt(token -> token.subject(correo))
                .authorities(new SimpleGrantedAuthority(rol));
    }

    private static String usuario(String correo, String documento, String rol) {
        return """
                {"nombre":"Usuario de Prueba","correo":"%s","contrasena":"%s","telefono":"3001112233",
                 "documento":"%s","fechaNacimiento":"1990-04-10","rol":"%s"}
                """.formatted(correo, CONTRASENA, documento, rol);
    }

    private static String correo(int n) {
        return "usuario" + n + "@prueba.co";
    }

    private static String documento(int n) {
        return "88000" + n;
    }
}
