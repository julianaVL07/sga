package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login y registro público (Guía 12) de punta a punta. A propósito no importa SeguridadPruebasConfig: las peticiones
 * viajan sin token, como las de un visitante.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-autenticacion")
@AutoConfigureMockMvc
class AutenticacionIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioJpaDataRepository usuarios;

    @Test
    @DisplayName("Login correcto: 200 con el token y solo el rol del usuario")
    void loginCorrectoResponde200ConRoles() throws Exception {
        login("recepcion@sga.local", "Recepcion2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").exists())
                .andExpect(jsonPath("$.roles", contains("RECEPCIONISTA")));
    }

    @Test
    @DisplayName("Login con contraseña errada: 401 con el ErrorResponse del proyecto")
    void contrasenaErradaResponde401() throws Exception {
        login("recepcion@sga.local", "Errada2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    @DisplayName("Login con correo sin formato: 400 que señala el campo")
    void correoSinFormatoResponde400() throws Exception {
        login("no-es-un-correo", "Recepcion2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").value("El correo no tiene un formato válido"));
    }

    @Test
    @DisplayName("Registro: 201 con HUESPED aunque pida otro rol, sin contraseña y guardada con BCrypt")
    void registroResponde201ConRolHuesped() throws Exception {
        String cuerpo = registro("nuevo@correo.co", "1095000001")
                .replace("\"nombre\"", "\"rol\":\"ADMINISTRADOR\",\"nombre\"");

        registrar(cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana Torres"))
                .andExpect(jsonPath("$.correo").value("nuevo@correo.co"))
                .andExpect(jsonPath("$.rol").value("HUESPED"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        assertTrue(usuarios.findByCorreo("nuevo@correo.co").orElseThrow().getContrasena().startsWith("{bcrypt}"));
        login("nuevo@correo.co", "Segura2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("HUESPED")));
    }

    @Test
    @DisplayName("Registro repetido: 409 si el correo o el documento ya existen")
    void registroRepetidoResponde409() throws Exception {
        registrar(registro("repetido@correo.co", "1095000002")).andExpect(status().isCreated());

        registrar(registro("repetido@correo.co", "1095000003"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("Ya existe un usuario registrado")));
        registrar(registro("distinto@correo.co", "1095000002"))
                .andExpect(status().isConflict());
    }

    private ResultActions login(String correo, String contrasena) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)));
    }

    private ResultActions registrar(String cuerpo) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String registro(String correo, String documento) {
        return """
                {"nombre":"Ana Torres","correo":"%s","contrasena":"Segura2026","telefono":"3160000000",
                 "documento":"%s","fechaNacimiento":"1990-05-20"}""".formatted(correo, documento);
    }
}
