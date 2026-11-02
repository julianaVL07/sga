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
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-04 (Reserva.crear), RN-08 (métodos explícitos de transición), RN-09 (Reserva.confirmar),
 * RN-10 (Reserva.registrarLlegada), RN-12 (Reserva.estaActiva y ocupaNochesDe), RN-22 (cotización y política
 * congeladas) y L-15 (Reserva.declararNoShow).
 */
class ReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final PlazoConfirmacion PLAZO_12_HORAS = new PlazoConfirmacion(12);
    private static final LocalTime HORA_LIMITE_NO_SHOW = LocalTime.of(22, 0);
    private static final LocalDate ENTRADA = LocalDate.of(2026, 10, 5);
    private static final LocalDate SALIDA = LocalDate.of(2026, 10, 7);
    private static final UmbralEdadFacturable UMBRAL = new UmbralEdadFacturable(6);

    private Apartamento apartamento;
    private Titular titular;
    private Ocupante ana;
    private Tarifa tarifaBase;
    private PoliticaCancelacion politica;
    private Estancia estancia;
    private Reserva reserva;

    @BeforeEach
    void crearReservaPendienteSinHoraDeLlegada() {
        Temporada base = Temporada.crearBase("T-BASE", "Base");
        apartamento = Apartamento.crear(new IdentificacionApartamento("APT-101"), "Apartamento 101",
                "Vista al mar", 1, new Capacidad(3), new Dotacion(List.of("Aire acondicionado")),
                List.of(new Imagen("https://coral.co/apt-101.jpg", true)));
        tarifaBase = new Tarifa("T-BASE", Dinero.de(310_000));
        apartamento.definirTarifa(tarifaBase);
        apartamento.activar(List.of(base));

        DocumentoIdentidad documento = new DocumentoIdentidad("CC", "1094000000");
        titular = new Titular("Ana Gómez", documento, "ana@correo.co", "3001234567");
        ana = new Ocupante("Ana Gómez", documento, LocalDate.of(1990, 5, 20));
        estancia = new Estancia(ENTRADA, SALIDA);
        politica = new PoliticaCancelacion(1, LocalDate.of(2026, 1, 1),
                "Retención del 50 % del total", List.of(new TramoCancelacion(16, 0), new TramoCancelacion(7, 25),
                new TramoCancelacion(0, 50)));

        reserva = Reserva.crear(CodigoReserva.generar(2026, 1), apartamento, estancia, titular, List.of(ana),
                CanalOrigen.PORTAL, null, null, cotizacionBase(estancia), politica, UMBRAL, AHORA);
    }

    // ---------------------------------------------------------------- RN-09 y RN-08

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

    // ---------------------------------------------------------------- RN-04

    @Test
    @DisplayName("RN-04: una reserva con fecha de entrada en el pasado se rechaza")
    void rechazarEntradaEnElPasado() {
        Estancia pasada = new Estancia(AHORA.toLocalDate().minusDays(1), AHORA.toLocalDate().plusDays(1));

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> crearReserva(2, pasada));

        assertTrue(error.getMessage().startsWith("RN-04"));
    }

    @Test
    @DisplayName("RN-04: una reserva con fecha de entrada hoy o futura se acepta")
    void aceptarEntradaHoyOFutura() {
        Estancia hoy = new Estancia(AHORA.toLocalDate(), AHORA.toLocalDate().plusDays(2));
        Estancia futura = new Estancia(AHORA.toLocalDate().plusDays(30), AHORA.toLocalDate().plusDays(32));

        Reserva deHoy = assertDoesNotThrow(() -> crearReserva(2, hoy));
        Reserva aFuturo = assertDoesNotThrow(() -> crearReserva(3, futura));

        assertEquals(EstadoReserva.PENDIENTE, deHoy.getEstado());
        assertEquals(EstadoReserva.PENDIENTE, aFuturo.getEstado());
    }

    // ---------------------------------------------------------------- RN-10

    @Test
    @DisplayName("RN-10: el registro antes de la fecha de entrada se rechaza")
    void rechazarRegistroAntesDeLaFechaDeEntrada() {
        confirmar(reserva);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> reserva.registrarLlegada(ENTRADA.minusDays(1), EstadoOperativo.PREPARADO));

        assertTrue(error.getMessage().startsWith("RN-10"));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    @DisplayName("RN-10: el registro sobre una reserva que no está CONFIRMADA se rechaza")
    void rechazarRegistroDeReservaNoConfirmada() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> reserva.registrarLlegada(ENTRADA, EstadoOperativo.PREPARADO));

        assertTrue(error.getMessage().startsWith("RN-10"));
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    @DisplayName("RN-10: el registro en la fecha de entrada de una reserva CONFIRMADA se permite")
    void permitirRegistroEnLaFechaDeEntrada() {
        confirmar(reserva);

        reserva.registrarLlegada(ENTRADA, EstadoOperativo.PREPARADO);

        assertEquals(EstadoReserva.EN_CURSO, reserva.getEstado());
        assertEquals(ENTRADA, reserva.getFechaRegistro());
    }

    // ---------------------------------------------------------------- RN-12

    @Test
    @DisplayName("RN-12: una reserva PENDIENTE, CONFIRMADA o EN_CURSO es activa y ocupa sus noches")
    void reservaActivaOcupaSusNoches() {
        assertTrue(reserva.estaActiva());
        assertTrue(reserva.ocupaNochesDe(estancia));

        confirmar(reserva);
        assertTrue(reserva.ocupaNochesDe(estancia));

        reserva.registrarLlegada(ENTRADA, EstadoOperativo.PREPARADO);
        assertTrue(reserva.estaActiva());
        assertTrue(reserva.ocupaNochesDe(estancia));
    }

    @Test
    @DisplayName("RN-12: al cancelar la reserva deja de ser activa y no ocupa sus noches")
    void cancelarLiberaLasNoches() {
        reserva.cancelar("Cambio de planes del huésped", AHORA.toLocalDate());

        assertNoOcupaSusNoches(reserva);
    }

    @Test
    @DisplayName("RN-12: al declarar no-show la reserva deja de ser activa y no ocupa sus noches")
    void noShowLiberaLasNoches() {
        confirmar(reserva);

        reserva.declararNoShow(ENTRADA.atTime(HORA_LIMITE_NO_SHOW), HORA_LIMITE_NO_SHOW);

        assertNoOcupaSusNoches(reserva);
    }

    @Test
    @DisplayName("RN-12: al vencer el plazo de confirmación la reserva deja de ser activa y no ocupa sus noches")
    void vencimientoLiberaLasNoches() {
        reserva.vencerPorPlazoDeConfirmacion(AHORA.plusHours(13), PLAZO_12_HORAS);

        assertNoOcupaSusNoches(reserva);
    }

    @Test
    @DisplayName("RN-12: al registrar la salida la reserva deja de ser activa y no ocupa sus noches")
    void salidaLiberaLasNoches() {
        confirmar(reserva);
        reserva.registrarLlegada(ENTRADA, EstadoOperativo.PREPARADO);

        reserva.registrarSalida(SALIDA);

        assertEquals(EstadoReserva.FINALIZADA, reserva.getEstado());
        assertNoOcupaSusNoches(reserva);
    }

    // ---------------------------------------------------------------- RN-22

    @Test
    @DisplayName("RN-22: el valor y la versión de política congelados no cambian si después cambian la tarifa o la política")
    void valorYPoliticaQuedanCongelados() {
        Dinero valorCongelado = reserva.valorTotal();

        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(450_000)));
        PoliticaCancelacion nuevaPolitica = new PoliticaCancelacion(2, LocalDate.of(2026, 10, 1),
                "Retención del 100 % del total", List.of(new TramoCancelacion(16, 50),
                new TramoCancelacion(0, 100)));

        assertEquals(Dinero.de(620_000), valorCongelado);
        assertEquals(valorCongelado, reserva.valorTotal());
        assertEquals(1, reserva.getPoliticaCancelacion().version());
        assertEquals(2, nuevaPolitica.version());
        // RN-13 con la política congelada: 4 días de antelación → tramo del 50 %, no el 100 % de la versión 2.
        assertEquals(Dinero.de(310_000), reserva.calcularRetencionPorCancelacion(AHORA.toLocalDate()));
    }

    @Test
    @DisplayName("RN-22: una reserva sin cotización para congelar se rechaza")
    void rechazarReservaSinCotizacion() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> Reserva.crear(CodigoReserva.generar(2026, 2), apartamento, estancia, titular, List.of(ana),
                        CanalOrigen.PORTAL, null, null, null, politica, UMBRAL, AHORA));

        assertTrue(error.getMessage().startsWith("RN-22"));
    }

    // ---------------------------------------------------------------- L-15

    @Test
    @DisplayName("L-15: el no-show antes de la hora límite del día de entrada se rechaza")
    void rechazarNoShowAntesDeLaHoraLimite() {
        confirmar(reserva);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> reserva.declararNoShow(ENTRADA.atTime(21, 59), HORA_LIMITE_NO_SHOW));

        assertTrue(error.getMessage().startsWith("L-15"));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    @DisplayName("L-15: el no-show a partir de la hora límite del día de entrada se permite")
    void permitirNoShowDesdeLaHoraLimite() {
        confirmar(reserva);

        reserva.declararNoShow(ENTRADA.atTime(HORA_LIMITE_NO_SHOW), HORA_LIMITE_NO_SHOW);

        assertEquals(EstadoReserva.NO_SHOW, reserva.getEstado());
    }

    // ---------------------------------------------------------------- Apoyo

    private Reserva crearReserva(int secuencia, Estancia nuevaEstancia) {
        return Reserva.crear(CodigoReserva.generar(2026, secuencia), apartamento, nuevaEstancia, titular, List.of(ana),
                CanalOrigen.PORTAL, null, null, cotizacionBase(nuevaEstancia), politica, UMBRAL, AHORA);
    }

    /** Cotización con la tarifa base para un ocupante facturable, una línea por noche. */
    private Cotizacion cotizacionBase(Estancia estanciaACotizar) {
        List<DetalleNoche> noches = new ArrayList<>();
        for (LocalDate noche = estanciaACotizar.fechaEntrada(); noche.isBefore(estanciaACotizar.fechaSalida());
             noche = noche.plusDays(1)) {
            noches.add(DetalleNoche.calcular(noche, "Base", tarifaBase, 1));
        }
        return Cotizacion.desde(noches);
    }

    private static void confirmar(Reserva reserva) {
        reserva.registrarHoraEstimadaLlegada(LocalTime.of(16, 0));
        reserva.confirmar(AHORA.plusHours(1), PLAZO_12_HORAS);
    }

    private void assertNoOcupaSusNoches(Reserva liberada) {
        assertFalse(liberada.estaActiva());
        assertFalse(liberada.ocupaNochesDe(estancia));
    }
}
