package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-11, implementada en EntregaApartamentoService.autorizarEntrega. */
class EntregaApartamentoServiceTest {

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final EntregaApartamentoService entrega =
            new EntregaApartamentoService(escenario.reservas, escenario.apartamentos);

    @Test
    @DisplayName("RN-11: no se entrega un apartamento que sigue EN_PREPARACION y nada cambia de estado")
    void rechazarLlegadaSiApartamentoEstaEnPreparacion() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
        Reserva reserva = escenario.reservaPendiente(1, estancia, LocalTime.of(16, 0));
        reserva.confirmar(EscenarioCoral.AHORA.plusHours(1), new PlazoConfirmacion(12));
        // El grupo anterior salió y el personal de servicio apenas está preparando el apartamento.
        escenario.apartamento.recibirGrupo();
        escenario.apartamento.liberarTrasSalida();
        escenario.apartamento.iniciarPreparacion();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> entrega.autorizarEntrega(reserva.getCodigo(), estancia.fechaEntrada()));

        assertTrue(error.getMessage().startsWith("RN-11"));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertEquals(EstadoOperativo.EN_PREPARACION, escenario.apartamento.getEstadoOperativo());
    }
}
