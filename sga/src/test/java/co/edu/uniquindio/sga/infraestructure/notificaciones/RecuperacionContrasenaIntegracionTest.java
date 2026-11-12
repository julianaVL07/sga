package co.edu.uniquindio.sga.infraestructure.notificaciones;

import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
import co.edu.uniquindio.sga.infrastructure.notificaciones.MensajeCorreo;
import co.edu.uniquindio.sga.infrastructure.notificaciones.NotificadorTitularLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static co.edu.uniquindio.sga.infraestructure.notificaciones.ApoyoNotificaciones.esperarMensajes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recuperación de contraseña de punta a punta (7.2) con el adaptador local: el código llega por el mismo puerto de
 * notificaciones, sirve una sola vez y deja iniciar sesión con la contraseña nueva. Las rutas son públicas.
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-recuperacion")
@AutoConfigureMockMvc
class RecuperacionContrasenaIntegracionTest {

    private static final Pattern CODIGO = Pattern.compile("(?m)^\\s+(\\d{6})$");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NotificadorTitular notificador;

    @Test
    @DisplayName("Solicitar, restablecer con el código e iniciar sesión con la contraseña nueva")
    void recorridoCompleto() throws Exception {
        String correo = "huesped2@sga.local";
        solicitar(correo).andExpect(status().isAccepted());
        String codigo = codigoRecibido(correo);

        // Un código incorrecto no sirve
        String otro = codigo.equals("000000") ? "111111" : "000000";
        restablecer(correo, otro, "Nueva2027").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El código no es válido o ya venció. Solicita uno nuevo."));

        restablecer(correo, codigo, "Nueva2027").andExpect(status().isNoContent());

        // Sirve una sola vez
        restablecer(correo, codigo, "Otra2028").andExpect(status().isBadRequest());

        login(correo, "Nueva2027").andExpect(status().isOk());
        login(correo, "Huesped2026").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Responde 202 aunque el correo no exista, y no envía nada")
    void correoInexistenteResponde202SinEnviar() throws Exception {
        String correo = "nadie@correo.co";
        solicitar(correo).andExpect(status().isAccepted());
        restablecer(correo, "123456", "Nueva2027").andExpect(status().isBadRequest());
        assertTrue(buzon().mensajesPara(correo).isEmpty());
    }

    @Test
    @DisplayName("400: la contraseña nueva sigue la regla del registro y el código tiene 6 dígitos")
    void validaLosDatos() throws Exception {
        restablecer("huesped1@sga.local", "123456", "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nuevaContrasena").exists());
        restablecer("huesped1@sga.local", "12ab", "Nueva2027").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists());
        solicitar("no-es-un-correo").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());
    }

    @Test
    @DisplayName("Pedir un código nuevo anula el anterior")
    void elCodigoNuevoAnulaElAnterior() throws Exception {
        String correo = "recepcion@sga.local";
        solicitar(correo).andExpect(status().isAccepted());
        String primero = codigoRecibido(correo);
        buzon().limpiar();
        solicitar(correo).andExpect(status().isAccepted());
        String segundo = codigoRecibido(correo);

        if (!primero.equals(segundo)) {
            restablecer(correo, primero, "Nueva2027").andExpect(status().isBadRequest());
        }
        restablecer(correo, segundo, "Nueva2027").andExpect(status().isNoContent());
    }

    private String codigoRecibido(String correo) throws InterruptedException {
        List<MensajeCorreo> mensajes = esperarMensajes(buzon(), correo, "CODIGO_RECUPERACION");
        assertEquals(1, mensajes.size(), "Debía llegar un correo con el código");
        Matcher encontrado = CODIGO.matcher(mensajes.getLast().cuerpo());
        assertTrue(encontrado.find(), "El correo no trae un código de 6 dígitos");
        return encontrado.group(1);
    }

    private NotificadorTitularLocal buzon() {
        return (NotificadorTitularLocal) notificador;
    }

    private ResultActions solicitar(String correo) throws Exception {
        return post("/api/auth/recuperacion/solicitar", "{\"correo\":\"" + correo + "\"}");
    }

    private ResultActions restablecer(String correo, String codigo, String nueva) throws Exception {
        return post("/api/auth/recuperacion/restablecer",
                "{\"correo\":\"" + correo + "\",\"codigo\":\"" + codigo + "\",\"nuevaContrasena\":\"" + nueva + "\"}");
    }

    private ResultActions login(String correo, String contrasena) throws Exception {
        return post("/api/auth/login", "{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}");
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
