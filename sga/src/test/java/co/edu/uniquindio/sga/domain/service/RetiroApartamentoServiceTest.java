package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: 7.3 (CU-21), implementada en RetiroApartamentoService y Apartamento.retirarDeLaVenta. */
class RetiroApartamentoServiceTest {

    private static final LocalDate HOY = EscenarioCoral.AHORA.toLocalDate();

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final RetiroApartamentoService retiro =
            new RetiroApartamentoService(escenario.apartamentos, escenario.reservas);

    @Test
    @DisplayName("7.3: sin reservas activas ni futuras se retira y deja de salir en los listados (12.4)")
    void retirarApartamentoSinReservas() {
        Apartamento retirado = retiro.retirar(EscenarioCoral.APT_101, HOY);

        assertTrue(retirado.isEliminado());
        assertTrue(escenario.apartamentos.listarVigentesParaVenta().isEmpty());
        assertTrue(escenario.apartamentos.listarNoEliminados().isEmpty());
        assertTrue(escenario.apartamentos.buscarPorIdentificacion(EscenarioCoral.APT_101).isPresent());
    }

    @Test
    @DisplayName("7.3: con una reserva futura el retiro se rechaza y el apartamento sigue a la venta")
    void rechazarRetiroConReservaFutura() {
        escenario.reservaPendiente(1, new Estancia(HOY.plusDays(20), HOY.plusDays(22)), LocalTime.of(16, 0));

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> retiro.retirar(EscenarioCoral.APT_101, HOY));

        assertTrue(error.getMessage().startsWith("7.3"));
        assertFalse(escenario.apartamentos.buscarPorIdentificacion(EscenarioCoral.APT_101).orElseThrow().isEliminado());
    }
}
