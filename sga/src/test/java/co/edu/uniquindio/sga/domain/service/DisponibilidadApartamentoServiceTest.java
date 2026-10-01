package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-07, implementada en DisponibilidadApartamentoService (validarDisponibilidad). */
class DisponibilidadApartamentoServiceTest {

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final DisponibilidadApartamentoService disponibilidad = new DisponibilidadApartamentoService(
            escenario.reservas, escenario.apartamentos, escenario.bloqueos, escenario.alojamientos);

    @Test
    @DisplayName("RN-07: un bloqueo vigente deja el apartamento no disponible en esas noches")
    void rechazarDisponibilidadSiExisteBloqueoVigente() {
        escenario.bloqueos.guardar(Bloqueo.registrar("BLQ-1", EscenarioCoral.APT_101,
                new RangoFechas(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 23)), "Mantenimiento de pintura"));
        Estancia dentroDelBloqueo = new Estancia(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 23));

        assertFalse(disponibilidad.estaDisponible(escenario.apartamento, dentroDelBloqueo, 1, null));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> disponibilidad.validarDisponibilidad(escenario.apartamento, dentroDelBloqueo, 1, null));
        assertTrue(error.getMessage().startsWith("RN-07"));
        assertTrue(disponibilidad.buscarDisponibles(dentroDelBloqueo, 1).isEmpty());
    }
}
