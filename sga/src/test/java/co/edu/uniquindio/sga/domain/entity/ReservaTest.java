package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DetalleNoche;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.Titular;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-08 (métodos explícitos de transición) y RN-09 (Reserva.confirmar). */
class ReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final PlazoConfirmacion PLAZO_12_HORAS = new PlazoConfirmacion(12);

    private Reserva reserva;

    @BeforeEach
    void crearReservaPendienteSinHoraDeLlegada() {
        Temporada base = Temporada.crearBase("T-BASE", "Base");
        Apartamento apartamento = Apartamento.crear(new IdentificacionApartamento("APT-101"), "Apartamento 101",
                "Vista al mar", 1, new Capacidad(3), new Dotacion(List.of("Aire acondicionado")),
                List.of(new Imagen("https://coral.co/apt-101.jpg", true)));
        Tarifa tarifaBase = new Tarifa("T-BASE", Dinero.de(310_000));
        apartamento.definirTarifa(tarifaBase);
        apartamento.activar(List.of(base));

        DocumentoIdentidad documento = new DocumentoIdentidad("CC", "1094000000");
        Titular titular = new Titular("Ana Gómez", documento, "ana@correo.co", "3001234567");
        Ocupante ana = new Ocupante("Ana Gómez", documento, LocalDate.of(1990, 5, 20));
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7));
        Cotizacion cotizacion = Cotizacion.desde(List.of(
                DetalleNoche.calcular(LocalDate.of(2026, 10, 5), "Base", tarifaBase, 1),
                DetalleNoche.calcular(LocalDate.of(2026, 10, 6), "Base", tarifaBase, 1)));
        PoliticaCancelacion politica = new PoliticaCancelacion(1, LocalDate.of(2026, 1, 1),
                "Retención del 50 % del total", List.of(new TramoCancelacion(16, 0), new TramoCancelacion(7, 25),
                new TramoCancelacion(0, 50)));

        reserva = Reserva.crear(CodigoReserva.generar(2026, 1), apartamento, estancia, titular, List.of(ana),
                CanalOrigen.PORTAL, null, null, cotizacion, politica, new UmbralEdadFacturable(6), AHORA);
    }

    @Test
    @DisplayName("RN-09: una reserva sin hora estimada de llegada no se puede confirmar")
    void rechazarConfirmacionSinHoraEstimadaDeLlegada() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> reserva.confirmar(AHORA.plusHours(1), PLAZO_12_HORAS));

        assertTrue(error.getMessage().startsWith("RN-09"));
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    @DisplayName("RN-08: una reserva CANCELADA no puede pasar a CONFIRMADA")
    void rechazarTransicionInvalidaDesdeCancelada() {
        reserva.registrarHoraEstimadaLlegada(LocalTime.of(16, 0));
        reserva.cancelar("Cambio de planes del huésped", AHORA.toLocalDate());

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> reserva.confirmar(AHORA.plusHours(1), PLAZO_12_HORAS));

        assertTrue(error.getMessage().startsWith("RN-08"));
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }
}
