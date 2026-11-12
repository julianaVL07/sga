package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.port.CredencialesCanal;
import co.edu.uniquindio.sga.domain.service.CanalVentaService.CredencialEmitida;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoAutenticacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.CanalVentaRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.EventoCanalRepositoryInMemory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 10.3.1: canales de venta con credencial propia. CanalVentaService emite la credencial (solo se guarda el hash),
 * autentica cada mensaje contra el canal de la ruta y deja en la bitácora (7.8) cada intento rechazado.
 */
class CanalVentaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 10, 0);

    /** Sin red ni BCrypt: genera credenciales numeradas y el "hash" es la credencial con un prefijo. */
    private static final class CredencialesDePrueba implements CredencialesCanal {
        private int emitidas;

        @Override
        public String generar() {
            return "credencial-" + (++emitidas);
        }

        @Override
        public String cifrar(String credencial) {
            return "hash:" + credencial;
        }

        @Override
        public boolean coincide(String credencial, String hash) {
            return hash.equals("hash:" + credencial);
        }
    }

    private final CanalVentaRepositoryInMemory canales = new CanalVentaRepositoryInMemory();
    private final EventoCanalRepositoryInMemory eventos = new EventoCanalRepositoryInMemory();
    private final CanalVentaService servicio = new CanalVentaService(canales, eventos, new CredencialesDePrueba());

    @Test
    @DisplayName("10.3.1: crear un canal devuelve la credencial en claro y guarda solo su hash")
    void registrarGuardaSoloElHash() {
        CredencialEmitida emitida = servicio.registrar("Booking", AHORA);

        assertEquals("credencial-1", emitida.credencial());
        CanalVenta guardado = canales.buscarPorNombre("booking").orElseThrow();
        assertEquals("hash:credencial-1", guardado.getHashCredencial());
        assertNotEquals(emitida.credencial(), guardado.getHashCredencial());
    }

    @Test
    @DisplayName("10.3.1: el nombre del canal es único, sin distinguir mayúsculas")
    void nombreUnico() {
        servicio.registrar("booking", AHORA);

        assertThrows(ReglaDominioException.class, () -> servicio.registrar("BOOKING", AHORA));
    }

    @Test
    @DisplayName("10.3.1: con la credencial de ese canal y el canal activo, el mensaje pasa y no deja rechazo")
    void autenticarCanalValido() {
        String credencial = servicio.registrar("booking", AHORA).credencial();

        ResultadoAutenticacionCanal resultado = servicio.autenticar("Booking", credencial,
                TipoEventoCanal.CREACION_RESERVA, null, AHORA);

        assertEquals(ResultadoAutenticacionCanal.AUTENTICADO, resultado);
        assertTrue(eventos.listarBitacora().isEmpty());
    }

    @Test
    @DisplayName("10.3.1: sin credencial, con una incorrecta o con la de otro canal es CREDENCIAL_INVALIDA y queda en "
            + "la bitácora")
    void credencialInvalida() {
        servicio.registrar("booking", AHORA);
        String deAirbnb = servicio.registrar("airbnb", AHORA).credencial();

        assertEquals(ResultadoAutenticacionCanal.CREDENCIAL_INVALIDA,
                servicio.autenticar("booking", null, TipoEventoCanal.CONSULTA_DISPONIBILIDAD, null, AHORA));
        assertEquals(ResultadoAutenticacionCanal.CREDENCIAL_INVALIDA,
                servicio.autenticar("booking", "otra", TipoEventoCanal.CREACION_RESERVA, null, AHORA));
        assertEquals(ResultadoAutenticacionCanal.CREDENCIAL_INVALIDA,
                servicio.autenticar("booking", deAirbnb, TipoEventoCanal.CANCELACION_RESERVA, "BK-1", AHORA));
        assertEquals(ResultadoAutenticacionCanal.CREDENCIAL_INVALIDA,
                servicio.autenticar("no-existe", deAirbnb, TipoEventoCanal.CREACION_RESERVA, null, AHORA));

        List<EventoCanal> bitacora = eventos.listarBitacora();
        assertEquals(4, bitacora.size());
        assertTrue(bitacora.stream().allMatch(e -> e.getResultado() == ResultadoEventoCanal.RECHAZADO_CREDENCIAL));
        assertTrue(bitacora.stream().anyMatch(e -> "BK-1".equals(e.getIdentificadorExterno())
                && e.getTipoEvento() == TipoEventoCanal.CANCELACION_RESERVA));
        assertTrue(bitacora.stream().anyMatch(e -> "no-existe".equals(e.getNombreCanal())));
    }

    @Test
    @DisplayName("10.3.1: un canal desactivado con su credencial correcta es CANAL_INACTIVO y queda en la bitácora")
    void canalInactivo() {
        String credencial = servicio.registrar("booking", AHORA).credencial();
        servicio.desactivar(canales.buscarPorNombre("booking").orElseThrow());

        ResultadoAutenticacionCanal resultado = servicio.autenticar("booking", credencial,
                TipoEventoCanal.CONSULTA_DISPONIBILIDAD, null, AHORA);

        assertEquals(ResultadoAutenticacionCanal.CANAL_INACTIVO, resultado);
        EventoCanal evento = eventos.listarBitacora().get(0);
        assertEquals(ResultadoEventoCanal.RECHAZADO_CANAL_INACTIVO, evento.getResultado());
        assertEquals("booking", evento.getNombreCanal());
        assertNull(evento.getIdentificadorExterno());
        assertFalse(canales.buscarPorNombre("booking").orElseThrow().isActivo());
    }

    @Test
    @DisplayName("10.3.1: al regenerar, la credencial anterior deja de servir y la nueva sí")
    void regenerarInvalidaLaAnterior() {
        String anterior = servicio.registrar("booking", AHORA).credencial();

        String nueva = servicio.regenerarCredencial(canales.buscarPorNombre("booking").orElseThrow()).credencial();

        assertNotEquals(anterior, nueva);
        assertEquals(ResultadoAutenticacionCanal.CREDENCIAL_INVALIDA,
                servicio.autenticar("booking", anterior, TipoEventoCanal.CREACION_RESERVA, null, AHORA));
        assertEquals(ResultadoAutenticacionCanal.AUTENTICADO,
                servicio.autenticar("booking", nueva, TipoEventoCanal.CREACION_RESERVA, null, AHORA));
    }

    @Test
    @DisplayName("Los datos iniciales registran el canal con una credencial conocida")
    void registrarConCredencialConocida() {
        CredencialEmitida emitida = servicio.registrar("booking", "demo-123", AHORA);

        assertEquals("demo-123", emitida.credencial());
        assertEquals(ResultadoAutenticacionCanal.AUTENTICADO,
                servicio.autenticar("booking", "demo-123", TipoEventoCanal.CONSULTA_DISPONIBILIDAD, null, AHORA));
        assertThrows(ReglaDominioException.class, () -> servicio.registrar("airbnb", " ", AHORA));
    }
}
