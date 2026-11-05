package co.edu.uniquindio.sga.infraestructure.rest;

import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Permisos por rol y por propietario (Guía 12, 7). A propósito no importa SeguridadPruebasConfig: cada caso lleva el
 * rol y el claim documento de su usuario. Los huéspedes son los de application.properties: el huésped 1 es el titular
 * de una reserva PENDIENTE de los datos iniciales y el huésped 2 no lo es.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sga-autorizacion")
@AutoConfigureMockMvc
class AutorizacionIntegracionTest {

    private static final String DOCUMENTO_HUESPED_1 = "1094777888";
    private static final String DOCUMENTO_HUESPED_2 = "1094333444";
    private static final String DOCUMENTO_RECEPCION = "1000000002";
    private static final String DOCUMENTO_SERVICIO = "1000000003";
    private static final String MENSAJE_403 = "No tiene permiso para realizar esta operación";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReservaRepository reservas;

    @Test
    @DisplayName("Huésped lista reservas: 403 con el ErrorResponse del proyecto")
    void huespedListaReservasResponde403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas").param("estado", "PENDIENTE")
                        .with(usuario("HUESPED", DOCUMENTO_HUESPED_1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    @Test
    @DisplayName("Recepción lista reservas: 200")
    void recepcionListaReservasResponde200() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas").param("estado", "PENDIENTE")
                        .with(usuario("RECEPCIONISTA", DOCUMENTO_RECEPCION)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El huésped 1 ve su reserva PENDIENTE: 200")
    void huespedVeSuReservaResponde200() throws Exception {
        String codigo = reservaPendienteDelHuesped1();

        mvc.perform(MockMvcRequestBuilders.get("/api/reservas/{codigo}", codigo)
                        .with(usuario("HUESPED", DOCUMENTO_HUESPED_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    @DisplayName("El huésped 2 ve la reserva del huésped 1: 403")
    void huespedVeReservaAjenaResponde403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas/{codigo}", reservaPendienteDelHuesped1())
                        .with(usuario("HUESPED", DOCUMENTO_HUESPED_2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    @Test
    @DisplayName("Recepción consulta un código inexistente bien formado: 404")
    void recepcionConCodigoInexistenteResponde404() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/reservas/RES-2000-99999")
                        .with(usuario("RECEPCIONISTA", DOCUMENTO_RECEPCION)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Personal de servicio confirma una reserva: 403")
    void servicioConfirmaResponde403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/reservas/{codigo}/confirmar", reservaPendienteDelHuesped1())
                        .with(usuario("PERSONAL_SERVICIO", DOCUMENTO_SERVICIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    @Test
    @DisplayName("Personal de servicio pone un apartamento fuera de servicio: 403")
    void servicioPoneFueraDeServicioResponde403() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/apartamentos/APT-101/poner-fuera-de-servicio")
                        .with(usuario("PERSONAL_SERVICIO", DOCUMENTO_SERVICIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_403));
    }

    private static JwtRequestPostProcessor usuario(String rol, String documento) {
        return jwt().jwt(token -> token.claim("documento", documento))
                .authorities(new SimpleGrantedAuthority(rol));
    }

    private String reservaPendienteDelHuesped1() {
        return reservas.listarTodas().stream()
                .filter(reserva -> reserva.getTitular().documento().numero().equals(DOCUMENTO_HUESPED_1))
                .filter(reserva -> reserva.getEstado() == EstadoReserva.PENDIENTE)
                .findFirst().orElseThrow()
                .getCodigo().valor();
    }
}
