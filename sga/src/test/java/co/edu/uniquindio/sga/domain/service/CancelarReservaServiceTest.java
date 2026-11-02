package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.PoliticaCancelacionRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ServicioAdicionalRepositoryInMemory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-12 (cancelar libera las noches) y RN-13 (la retención sale de la política congelada en
 * la reserva, no de la vigente), implementadas en CancelarReservaService.cancelar, Reserva.cancelar y
 * Folio.registrarPenalidad.
 * Escenario: reserva de APT-101 del 20 al 22 de octubre de 2026 (temporada base, $310.000 por noche, una ocupante:
 * $620.000), creada con la política versión 1 (16 días o más 0 %, de 7 a 15 días 25 %, menos de 7 días 50 %).
 */
class CancelarReservaServiceTest {

    private static final Estancia ESTANCIA = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
    private static final Dinero VALOR_TOTAL = Dinero.de(620_000);

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final DisponibilidadApartamentoService disponibilidad = new DisponibilidadApartamentoService(
            escenario.reservas, escenario.apartamentos, escenario.bloqueos, escenario.alojamientos);
    private final CreacionReservaService creacion = new CreacionReservaService(escenario.reservas, escenario.folios,
            escenario.apartamentos, escenario.alojamientos, new ServicioAdicionalRepositoryInMemory(), disponibilidad,
            new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos),
            DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), "SRV-RECEPCION-NOCTURNA");
    private final CancelarReservaService cancelacion = new CancelarReservaService(escenario.reservas,
            escenario.folios);

    // ---------------------------------------------------------------- RN-13

    @Test
    @DisplayName("RN-13: la retención usa la política congelada en la reserva aunque la vigente haya cambiado")
    void retencionConPoliticaCongelada() {
        PoliticaCancelacionRepositoryInMemory politicas = new PoliticaCancelacionRepositoryInMemory();
        PoliticaCancelacionService politicaService = new PoliticaCancelacionService(politicas);
        politicaService.registrarInicialSiNoExiste(escenario.politica);
        Reserva reserva = reservar(politicaService.vigente());
        // Después de reservar se publica la versión 2: de 7 a 15 días retendría el 50 % en vez del 25 %
        PoliticaCancelacion version2 = politicaService.actualizar("Se retiene el 100 % del valor total",
                List.of(new TramoCancelacion(16, 10), new TramoCancelacion(7, 50), new TramoCancelacion(0, 100)),
                EscenarioCoral.AHORA.toLocalDate());
        assertEquals(2, politicaService.vigente().version());

        // Cancela con 10 días de antelación: la versión 1 retiene el 25 %
        Dinero retencion = cancelacion.cancelar(reserva.getCodigo(), LocalDate.of(2026, 10, 10), "Cambio de planes");

        assertEquals(VALOR_TOTAL.porcentaje(25), retencion);
        assertEquals(Dinero.de(155_000), retencion);
        assertFalse(retencion.equals(VALOR_TOTAL.porcentaje(version2.obtenerPorcentajeRetencion(10))));
        Cargo penalidad = penalidades(folio(reserva)).get(0);
        assertEquals(retencion, penalidad.valor());
        assertTrue(penalidad.concepto().contains("versión 1"));
        assertEquals(1, escenario.reservas.buscarPorCodigo(reserva.getCodigo()).orElseThrow()
                .getPoliticaCancelacion().version());
    }

    @Test
    @DisplayName("RN-13: una reserva ya cancelada no se vuelve a cancelar ni se le cobra otra retención")
    void noSeRetieneDosVeces() {
        Reserva reserva = reservar(escenario.politica);
        cancelacion.cancelar(reserva.getCodigo(), LocalDate.of(2026, 10, 10), "Cambio de planes");
        int cargosAntes = folio(reserva).getCargos().size();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> cancelacion.cancelar(reserva.getCodigo(), LocalDate.of(2026, 10, 11), "Otra vez"));

        assertTrue(error.getMessage().startsWith("RN-08"));
        assertEquals(cargosAntes, folio(reserva).getCargos().size());
        assertEquals(1, penalidades(folio(reserva)).size());
    }

    // ---------------------------------------------------------------- RN-12

    @Test
    @DisplayName("RN-12: al cancelar se liberan las noches y la retención queda como cargo de penalidad en el folio")
    void cancelarLiberaLasNochesYAsientaLaRetencion() {
        Reserva reserva = reservar(escenario.politica);
        assertFalse(disponibilidad.estaDisponible(escenario.apartamento, ESTANCIA, 1, null));

        // 5 días de antelación: menos de 7 retiene el 50 % (RN-13)
        Dinero retencion = cancelacion.cancelar(reserva.getCodigo(), LocalDate.of(2026, 10, 15), "Enfermedad");

        Reserva cancelada = escenario.reservas.buscarPorCodigo(reserva.getCodigo()).orElseThrow();
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertFalse(cancelada.estaActiva());
        assertTrue(disponibilidad.estaDisponible(escenario.apartamento, ESTANCIA, 1, null));

        Folio folio = folio(reserva);
        List<Cargo> penalidades = penalidades(folio);
        assertEquals(1, penalidades.size());
        assertEquals(Dinero.de(310_000), retencion);
        assertEquals(retencion, penalidades.get(0).valor());
        // RN-16: los cargos anteriores se revierten, no se borran; solo queda por cobrar la retención
        assertEquals(retencion, folio.saldo());

        // Las noches liberadas se pueden volver a vender
        Reserva nueva = reservar(escenario.politica);
        assertTrue(nueva.estaActiva());
    }

    @Test
    @DisplayName("RN-12: mientras la reserva no se cancela, sus noches siguen retenidas y no se pueden volver a vender")
    void sinCancelarLasNochesSiguenRetenidas() {
        reservar(escenario.politica);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> reservar(escenario.politica));

        assertTrue(error.getMessage().startsWith("RN-01"));
        assertEquals(1, escenario.reservas.listarTodas().size());
    }

    /** Reserva PENDIENTE de la titular sola, con su folio (alojamiento + depósito), creada como en CU-03. */
    private Reserva reservar(PoliticaCancelacion politica) {
        return creacion.registrarNuevaReserva(EscenarioCoral.APT_101, ESTANCIA, escenario.titular,
                List.of(escenario.ana), LocalTime.of(16, 0), null, politica, CanalOrigen.PORTAL, null,
                EscenarioCoral.AHORA);
    }

    private Folio folio(Reserva reserva) {
        return escenario.folios.buscarPorReserva(reserva.getCodigo()).orElseThrow();
    }

    private static List<Cargo> penalidades(Folio folio) {
        return folio.getCargos().stream().filter(c -> c.tipo() == TipoCargo.PENALIDAD_CANCELACION_NOSHOW).toList();
    }
}
