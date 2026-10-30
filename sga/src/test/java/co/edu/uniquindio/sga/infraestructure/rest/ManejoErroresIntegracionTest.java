package co.edu.uniquindio.sga.infraestructure.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * transversal: todos los errores salen con el mismo cuerpo ErrorResponse (timestamp, status, error,
 * mensaje, path, errores), también los que genera Spring antes de llegar a un controlador.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManejoErroresIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("400: JSON mal formado")
    void jsonMalFormadoResponde400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identificacionApartamento\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/reservas"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("404: ruta que no existe, con el mismo formato de error")
    void rutaInexistenteResponde404() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/huespedes"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/huespedes"));
    }

    @Test
    @DisplayName("405: método no permitido en una ruta que sí existe")
    void metodoNoPermitidoResponde405() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/reservas/RES-2026-00001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("409: el código de reserva no tiene el formato RES-AAAA-NNNNN")
    void codigoMalFormadoResponde409() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas/ABC"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").exists());
    }
}