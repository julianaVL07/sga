package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService.DatosModificacion;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-14, implementada en ModificacionReservaService.modificar y Reserva.modificar. */
class ModificacionReservaServiceTest {

    private static final LocalTime HORA_LLEGADA = LocalTime.of(16, 0);

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final CotizadorEstanciaService cotizador =
            new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos);
    private final DisponibilidadApartamentoService disponibilidad = new DisponibilidadApartamentoService(
            escenario.reservas, escenario.apartamentos, escenario.bloqueos, escenario.alojamientos, cotizador);
    private final ModificacionReservaService modificacion = new ModificacionReservaService(escenario.reservas,
            escenario.folios, escenario.apartamentos, escenario.alojamientos, disponibilidad, cotizador);

    @Test
    @DisplayName("RN-14: la modificación revalida las condiciones y rechaza un rango no disponible")
    void rechazarModificacionAUnRangoNoDisponible() {
        // Martes 20 a jueves 22 y lunes 26 a miércoles 28 de octubre, en temporada base.
        Reserva otra = escenario.reservaPendiente(1, new Estancia(LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 10, 28)), HORA_LLEGADA);
        Reserva reserva = conFolio(escenario.reservaPendiente(2, new Estancia(LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 22)), HORA_LLEGADA));
        Estancia ocupada = otra.getEstancia();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> modificacion.modificar(reserva.getCodigo(), new DatosModificacion(null, ocupada, null),
                        EscenarioCoral.AHORA));

        assertTrue(error.getMessage().startsWith("RN-01"));
        assertEquals(new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22)), reserva.getEstancia());
        assertEquals(Dinero.de(620_000), reserva.valorTotal());
        assertTrue(ajustesDe(reserva).isEmpty());
    }

    @Test
    @DisplayName("RN-14: con un rango válido se recalcula el valor y se registra el ajuste en el folio")
    void recalcularValorYRegistrarAjuste() {
        Reserva reserva = conFolio(escenario.reservaPendiente(1, new Estancia(LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 22)), HORA_LLEGADA));
        // Una noche más en temporada base: de $620.000 a $930.000.
        Estancia nueva = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));

        Dinero diferencia = modificacion.modificar(reserva.getCodigo(), new DatosModificacion(null, nueva, null),
                EscenarioCoral.AHORA);

        assertEquals(Dinero.de(310_000), diferencia);
        assertEquals(nueva, reserva.getEstancia());
        assertEquals(Dinero.de(930_000), reserva.valorTotal());
        List<Cargo> ajustes = ajustesDe(reserva);
        assertEquals(1, ajustes.size());
        assertEquals(Dinero.de(310_000), ajustes.get(0).valor());
    }

    /** Folio abierto como en CU-03: cargo de alojamiento y depósito de garantía (RP-02). */
    private Reserva conFolio(Reserva reserva) {
        LocalDate apertura = EscenarioCoral.AHORA.toLocalDate();
        escenario.folios.guardar(Folio.abrir("FOL-" + reserva.getCodigo().valor(), reserva.getCodigo(),
                new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento", reserva.valorTotal(), apertura),
                DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), apertura));
        return reserva;
    }

    /**
     * Ajustes por modificación (RN-14). El depósito de garantía también es AJUSTE_MODIFICACION (RP-02, 7.7),
     * así que se distinguen por el concepto que asienta ModificacionReservaService.
     */
    private List<Cargo> ajustesDe(Reserva reserva) {
        return escenario.folios.buscarPorReserva(reserva.getCodigo()).orElseThrow().getCargos().stream()
                .filter(cargo -> cargo.tipo() == TipoCargo.AJUSTE_MODIFICACION)
                .filter(cargo -> cargo.concepto().startsWith("Ajuste por modificación"))
                .toList();
    }
}
