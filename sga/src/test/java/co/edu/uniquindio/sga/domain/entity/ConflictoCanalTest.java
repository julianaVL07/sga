package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoConflictoCanal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RN-18: el conflicto de canal queda PENDIENTE y el administrador lo resuelve una sola vez, con motivo. */
class ConflictoCanalTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    private static ConflictoCanal pendiente() {
        return ConflictoCanal.registrar("CNF-1", CanalOrigen.EXTERNO, "booking", "BK-100",
                "APT-102 ya tiene la reserva RES-2026-00004 en esas noches", HOY);
    }

    @Test
    @DisplayName("RN-18: el conflicto nace PENDIENTE, con el canal que envió la reserva")
    void registrarConflicto() {
        ConflictoCanal conflicto = pendiente();

        assertTrue(conflicto.estaPendiente());
        assertEquals(EstadoConflictoCanal.PENDIENTE, conflicto.estado());
        assertEquals("booking", conflicto.getNombreCanal());
        assertNull(conflicto.getResueltoPor());
    }

    @Test
    @DisplayName("RN-18: solo una reserva de canal externo, con su canal e identificador, genera un conflicto")
    void rechazarConflictoSinDatosDelCanal() {
        assertThrows(ReglaDominioException.class, () -> ConflictoCanal.registrar("CNF-1", CanalOrigen.PORTAL,
                "portal", "X", "detalle", HOY));
        assertThrows(ReglaDominioException.class, () -> ConflictoCanal.registrar("CNF-1", CanalOrigen.EXTERNO,
                " ", "BK-1", "detalle", HOY));
        assertThrows(ReglaDominioException.class, () -> ConflictoCanal.registrar("CNF-1", CanalOrigen.EXTERNO,
                "booking", null, "detalle", HOY));
    }

    @Test
    @DisplayName("7.1: resolver deja el conflicto RESUELTO con el motivo, quién lo resolvió y cuándo")
    void resolverConflicto() {
        ConflictoCanal conflicto = pendiente();

        conflicto.resolver("  Se reubicó al huésped en APT-301  ", "admin@sga.local", HOY.plusDays(1));

        assertFalse(conflicto.estaPendiente());
        assertEquals(EstadoConflictoCanal.RESUELTO, conflicto.estado());
        assertEquals("Se reubicó al huésped en APT-301", conflicto.getMotivoResolucion());
        assertEquals("admin@sga.local", conflicto.getResueltoPor());
        assertEquals(HOY.plusDays(1), conflicto.getFechaResolucion());
    }

    @Test
    @DisplayName("RN-18: un conflicto se resuelve una sola vez")
    void noSeResuelveDosVeces() {
        ConflictoCanal conflicto = pendiente();
        conflicto.resolver("Primera revisión", "admin@sga.local", HOY);

        assertThrows(ReglaDominioException.class,
                () -> conflicto.resolver("Segunda revisión", "otro@sga.local", HOY));
        assertEquals("Primera revisión", conflicto.getMotivoResolucion());
        assertEquals("admin@sga.local", conflicto.getResueltoPor());
    }

    @Test
    @DisplayName("Resolver exige motivo, administrador y una fecha que no sea anterior al conflicto")
    void resolverExigeLosDatos() {
        ConflictoCanal conflicto = pendiente();

        assertThrows(ReglaDominioException.class, () -> conflicto.resolver(" ", "admin@sga.local", HOY));
        assertThrows(ReglaDominioException.class, () -> conflicto.resolver("motivo", null, HOY));
        assertThrows(ReglaDominioException.class, () -> conflicto.resolver("motivo", "admin@sga.local", null));
        assertThrows(ReglaDominioException.class,
                () -> conflicto.resolver("motivo", "admin@sga.local", HOY.minusDays(1)));
        assertTrue(conflicto.estaPendiente());
    }
}
