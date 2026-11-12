package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 10.3.1: canal de venta externo, su credencial (solo el hash) y su eliminación lógica. */
class CanalVentaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 9, 0);

    @Test
    @DisplayName("10.3.1: un canal nuevo es EXTERNO, activo y guarda su nombre en minúsculas")
    void registrarCanal() {
        CanalVenta canal = CanalVenta.registrar("  Booking ", "hash-1", AHORA);

        assertEquals("booking", canal.getNombre());
        assertEquals(CanalOrigen.EXTERNO, canal.getTipo());
        assertEquals("hash-1", canal.getHashCredencial());
        assertTrue(canal.isActivo());
        assertTrue(canal.admiteMensajes());
        assertEquals(AHORA, canal.getFechaCreacion());
    }

    @Test
    @DisplayName("10.3.1: el nombre viaja en la ruta: sin espacios ni símbolos, y no puede ser una ruta de la API")
    void rechazarNombreInvalido() {
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("mi canal", "hash", AHORA));
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("x", "hash", AHORA));
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("-booking", "hash", AHORA));
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar(" ", "hash", AHORA));
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("conflictos", "hash", AHORA));
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("eventos", "hash", AHORA));
    }

    @Test
    @DisplayName("10.3.1: un canal sin credencial o sin fecha no se registra")
    void rechazarSinCredencialNiFecha() {
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("booking", " ", AHORA));
        assertThrows(ReglaDominioException.class, () -> CanalVenta.registrar("booking", "hash", null));
    }

    @Test
    @DisplayName("10.3.1: regenerar reemplaza el hash de la credencial")
    void regenerarCredencial() {
        CanalVenta canal = CanalVenta.registrar("booking", "hash-1", AHORA);

        canal.regenerarCredencial("hash-2");

        assertEquals("hash-2", canal.getHashCredencial());
    }

    @Test
    @DisplayName("Eliminación lógica: el canal desactivado no admite mensajes, no se desactiva dos veces y no recibe "
            + "credenciales nuevas")
    void desactivarCanal() {
        CanalVenta canal = CanalVenta.registrar("booking", "hash-1", AHORA);

        canal.desactivar();

        assertFalse(canal.isActivo());
        assertFalse(canal.admiteMensajes());
        assertThrows(ReglaDominioException.class, canal::desactivar);
        assertThrows(ReglaDominioException.class, () -> canal.regenerarCredencial("hash-2"));
        assertEquals("hash-1", canal.getHashCredencial());
    }
}
