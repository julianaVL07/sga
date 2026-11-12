package co.edu.uniquindio.sga.infraestructure.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de 10.2 con el proveedor por defecto (local, sin red): la sugerencia llega con gravedad y
 * explicación, y solo la pueden pedir el personal de servicio y el administrador. No guarda nada.
 * Cada petición lleva el token del rol que se prueba. Usa su propia base H2 (sga-ia-local).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-ia-local")
@AutoConfigureMockMvc
class SugerenciaGravedadLocalIntegracionTest {

    private static final String SUGERIR = "/api/apartamentos/novedades/sugerir-gravedad";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("10.2: el proveedor local sugiere ALTA para una fuga de gas, con su explicación")
    void sugerenciaLocal() throws Exception {
        sugerir("ADMINISTRADOR", "Huele a gas en la cocina")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.gravedad").value("ALTA"))
                .andExpect(jsonPath("$.explicacion", not(emptyString())));
    }

    @Test
    @DisplayName("10.2: el personal de servicio puede pedir la sugerencia; la recepción y el huésped no (403)")
    void permisosPorRol() throws Exception {
        sugerir("PERSONAL_SERVICIO", "Falta una toalla").andExpect(status().isOk())
                .andExpect(jsonPath("$.gravedad").value("BAJA"));
        sugerir("RECEPCIONISTA", "Falta una toalla").andExpect(status().isForbidden());
        sugerir("HUESPED", "Falta una toalla").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10.2: sin token la sugerencia responde 401")
    void sinToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post(SUGERIR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Falta una toalla\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions sugerir(String rol, String descripcion) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(SUGERIR)
                .with(jwt().authorities(new SimpleGrantedAuthority(rol)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcion\":\"" + descripcion + "\"}"));
    }
}
