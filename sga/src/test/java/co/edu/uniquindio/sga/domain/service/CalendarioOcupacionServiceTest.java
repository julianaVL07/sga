package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNoche;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: 7.1 (CU-27), implementada en CalendarioOcupacionService. */
class CalendarioOcupacionServiceTest {

    private static final LocalDate HOY = EscenarioCoral.AHORA.toLocalDate();

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final CalendarioOcupacionService calendario =
            new CalendarioOcupacionService(escenario.reservas, escenario.bloqueos);

    @Test
    @DisplayName("7.1: sin reservas ni bloqueos todas las noches salen LIBRE, una por fecha y sin la de hasta")
    void nochesLibres() {
        List<NocheCalendario> noches = calendario.calendario(EscenarioCoral.APT_101, HOY, HOY.plusDays(3));

        assertEquals(List.of(HOY, HOY.plusDays(1), HOY.plusDays(2)),
                noches.stream().map(NocheCalendario::fecha).toList());
        assertTrue(noches.stream().allMatch(n -> n.estado() == EstadoNoche.LIBRE));
        assertNull(noches.get(0).codigoReserva());
        assertNull(noches.get(0).motivoBloqueo());
    }

    @Test
    @DisplayName("7.1: las noches de una reserva activa salen RESERVADA con su código y estado; la de salida no")
    void nochesReservadas() {
        Reserva reserva = escenario.reservaPendiente(1, new Estancia(HOY.plusDays(1), HOY.plusDays(3)),
                LocalTime.of(16, 0));

        List<NocheCalendario> noches = calendario.calendario(EscenarioCoral.APT_101, HOY, HOY.plusDays(4));

        assertEquals(EstadoNoche.LIBRE, noches.get(0).estado());
        for (NocheCalendario noche : noches.subList(1, 3)) {
            assertEquals(EstadoNoche.RESERVADA, noche.estado());
            assertEquals(reserva.getCodigo(), noche.codigoReserva());
            assertEquals(EstadoReserva.PENDIENTE, noche.estadoReserva());
        }
        assertEquals(EstadoNoche.LIBRE, noches.get(3).estado());
    }

    @Test
    @DisplayName("RN-12: una reserva cancelada ya no retiene sus noches y estas salen LIBRE")
    void reservaCanceladaLiberaLasNoches() {
        Reserva reserva = escenario.reservaPendiente(1, new Estancia(HOY.plusDays(1), HOY.plusDays(3)),
                LocalTime.of(16, 0));
        reserva.cancelar("Cambio de planes", HOY);
        escenario.reservas.guardar(reserva);

        List<NocheCalendario> noches = calendario.calendario(EscenarioCoral.APT_101, HOY, HOY.plusDays(4));

        assertTrue(noches.stream().allMatch(n -> n.estado() == EstadoNoche.LIBRE));
    }

    @Test
    @DisplayName("RN-07: las noches de un bloqueo vigente salen BLOQUEADA con su motivo; uno levantado no cuenta")
    void nochesBloqueadas() {
        escenario.bloqueos.guardar(Bloqueo.registrar("BLQ-1", EscenarioCoral.APT_101,
                new RangoFechas(HOY.plusDays(2), HOY.plusDays(4)), "Mantenimiento del aire acondicionado"));
        Bloqueo levantado = Bloqueo.registrar("BLQ-2", EscenarioCoral.APT_101,
                new RangoFechas(HOY, HOY.plusDays(1)), "Fumigación");
        levantado.levantar(HOY);
        escenario.bloqueos.guardar(levantado);

        List<NocheCalendario> noches = calendario.calendario(EscenarioCoral.APT_101, HOY, HOY.plusDays(5));

        assertEquals(EstadoNoche.LIBRE, noches.get(0).estado());
        assertEquals(EstadoNoche.LIBRE, noches.get(1).estado());
        assertEquals(EstadoNoche.BLOQUEADA, noches.get(2).estado());
        assertEquals("Mantenimiento del aire acondicionado", noches.get(2).motivoBloqueo());
        assertNull(noches.get(2).codigoReserva());
        assertEquals(EstadoNoche.BLOQUEADA, noches.get(3).estado());
        assertEquals(EstadoNoche.LIBRE, noches.get(4).estado());
    }

    @Test
    @DisplayName("7.1: desde igual o posterior a hasta se rechaza")
    void rangoInvertidoOVacio() {
        ReglaDominioException invertido = assertThrows(ReglaDominioException.class,
                () -> calendario.calendario(EscenarioCoral.APT_101, HOY.plusDays(3), HOY));
        ReglaDominioException vacio = assertThrows(ReglaDominioException.class,
                () -> calendario.calendario(EscenarioCoral.APT_101, HOY, HOY));

        assertTrue(invertido.getMessage().startsWith("7.1"));
        assertTrue(vacio.getMessage().startsWith("7.1"));
    }

    @Test
    @DisplayName("7.1: el calendario admite 62 noches y rechaza 63 (decisión del equipo)")
    void rangoMaximoDeNoches() {
        assertDoesNotThrow(() -> calendario.calendario(EscenarioCoral.APT_101, HOY,
                HOY.plusDays(CalendarioOcupacionService.MAXIMO_NOCHES)));

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> calendario.calendario(EscenarioCoral.APT_101, HOY,
                        HOY.plusDays(CalendarioOcupacionService.MAXIMO_NOCHES + 1)));

        assertTrue(error.getMessage().startsWith("7.1"));
    }

    @Test
    @DisplayName("7.1: sin fechas se rechaza")
    void rangoSinFechas() {
        assertThrows(ReglaDominioException.class, () -> calendario.calendario(EscenarioCoral.APT_101, null, HOY));
        assertThrows(ReglaDominioException.class, () -> calendario.calendario(EscenarioCoral.APT_101, HOY, null));
    }
}
