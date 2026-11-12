package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.infraestructure.config.SeguridadPruebasConfig;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de 10.2 con la IA apagada (sga.ia.proveedor=deshabilitado): la sugerencia responde 200 con
 * disponible=false y la novedad se registra igual con la gravedad que elige la persona. Sin red.
 * Usa su propia base H2 (sga-ia-deshabilitada) para no tocar las novedades de las demás pruebas.
 */
@Import(SeguridadPruebasConfig.class)
@SpringBootTest(properties = {
        "sga.ia.proveedor=deshabilitado",
        "spring.datasource.url=jdbc:h2:mem:sga-ia-deshabilitada"})
@AutoConfigureMockMvc
class SugerenciaGravedadDeshabilitadaIntegracionTest {

    private static final String SUGERIR = "/api/apartamentos/novedades/sugerir-gravedad";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("10.2: con la IA deshabilitada la sugerencia responde 200 con disponible=false")
    void sugerenciaNoDisponible() throws Exception {
        post(SUGERIR, "{\"descripcion\":\"Huele a gas en la cocina\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.gravedad").isEmpty())
                .andExpect(jsonPath("$.explicacion").isEmpty());
    }

    @Test
    @DisplayName("10.2: sin sugerencia la novedad se registra igual con la gravedad elegida por la persona")
    void novedadSeRegistraSinSugerencia() throws Exception {
        String descripcion = "Huele a gas en la cocina";
        post(SUGERIR, "{\"descripcion\":\"" + descripcion + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));

        post("/api/apartamentos/APT-201/novedades",
                "{\"autor\":\"Camarera Luz\",\"descripcion\":\"" + descripcion + "\",\"gravedad\":\"MEDIA\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value(descripcion))
                .andExpect(jsonPath("$.gravedad").value("MEDIA"));
    }

    @Test
    @DisplayName("10.2: la descripción es obligatoria para pedir la sugerencia (400)")
    void descripcionObligatoria() throws Exception {
        post(SUGERIR, "{\"descripcion\":\"  \"}").andExpect(status().isBadRequest());
        post(SUGERIR, "{}").andExpect(status().isBadRequest());
    }

    private ResultActions post(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
