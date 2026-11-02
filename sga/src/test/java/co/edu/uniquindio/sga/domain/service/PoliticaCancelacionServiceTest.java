package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.PoliticaCancelacionRepositoryInMemory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: CU-30, 7.4 y F-07 (una sola política vigente; al actualizarla se conserva la anterior),
 * RN-22 (cada reserva queda con la versión vigente al crearse) y L-10, implementadas en PoliticaCancelacionService
 * y PoliticaCancelacion.nuevaVersion.
 */
class PoliticaCancelacionServiceTest {

    private static final LocalDate HOY = EscenarioCoral.AHORA.toLocalDate();
    private static final String NO_SHOW = "Se retiene el 60 % del valor total";

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final PoliticaCancelacionRepositoryInMemory repositorio = new PoliticaCancelacionRepositoryInMemory();
    private final PoliticaCancelacionService servicio = new PoliticaCancelacionService(repositorio);

    @Test
    @DisplayName("F-07: sin ninguna versión no hay política vigente")
    void sinPoliticaNoHayVigente() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class, servicio::vigente);
        assertTrue(error.getMessage().startsWith("F-07"));
    }

    @Test
    @DisplayName("Carga inicial: guarda la versión 1 solo si aún no hay ninguna")
    void registrarInicialSoloUnaVez() {
        servicio.registrarInicialSiNoExiste(escenario.politica);
        PoliticaCancelacion otra = new PoliticaCancelacion(1, LocalDate.of(2026, 2, 1), NO_SHOW,
                List.of(new TramoCancelacion(10, 0), new TramoCancelacion(0, 100)));

        PoliticaCancelacion vigente = servicio.registrarInicialSiNoExiste(otra);

        assertEquals(escenario.politica, vigente);
        assertEquals(1, repositorio.listarVersiones(0, 10).totalElementos());
    }

    @Test
    @DisplayName("7.4: actualizar crea la versión 2 vigente desde hoy y conserva la versión 1 sin cambios")
    void actualizarCreaNuevaVersion() {
        servicio.registrarInicialSiNoExiste(escenario.politica);

        PoliticaCancelacion nueva = servicio.actualizar(NO_SHOW, tramosNuevos(), HOY);

        assertEquals(2, nueva.version());
        assertEquals(HOY, nueva.fechaVigencia());
        assertEquals(NO_SHOW, nueva.consecuenciaNoShow());
        assertEquals(nueva, servicio.vigente());
        Pagina<PoliticaCancelacion> versiones = repositorio.listarVersiones(0, 10);
        assertEquals(List.of(2, 1), versiones.contenido().stream().map(PoliticaCancelacion::version).toList());
        assertEquals(escenario.politica, versiones.contenido().get(1));
    }

    @Test
    @DisplayName("RN-22: la reserva creada con la versión 1 la conserva; una reserva nueva congela la versión 2")
    void reservasConservanSuVersion() {
        servicio.registrarInicialSiNoExiste(escenario.politica);
        Reserva existente = reserva(1, servicio.vigente());

        servicio.actualizar(NO_SHOW, tramosNuevos(), HOY);
        Reserva nueva = reserva(2, servicio.vigente());

        assertEquals(1, existente.getPoliticaCancelacion().version());
        assertEquals(2, nueva.getPoliticaCancelacion().version());
        // Con 3 días de antelación la versión 1 retiene 50 % y la versión 2 retiene 60 %
        Dinero total = existente.valorTotal();
        LocalDate cancelacion = existente.getEstancia().fechaEntrada().minusDays(3);
        assertEquals(total.porcentaje(50), existente.calcularRetencionPorCancelacion(cancelacion));
        assertEquals(nueva.valorTotal().porcentaje(60), nueva.calcularRetencionPorCancelacion(cancelacion));
    }

    @Test
    @DisplayName("L-10: una política con un solo tramo se rechaza y sigue vigente la versión anterior")
    void unSoloTramo() {
        servicio.registrarInicialSiNoExiste(escenario.politica);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> servicio.actualizar(NO_SHOW, List.of(new TramoCancelacion(0, 50)), HOY));

        assertTrue(error.getMessage().startsWith("L-10"));
        assertEquals(1, servicio.vigente().version());
        assertEquals(1, repositorio.listarVersiones(0, 10).totalElementos());
    }

    @Test
    @DisplayName("L-10: debe existir un tramo desde 0 días")
    void sinTramoDesdeCero() {
        servicio.registrarInicialSiNoExiste(escenario.politica);

        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> servicio.actualizar(NO_SHOW,
                List.of(new TramoCancelacion(15, 0), new TramoCancelacion(5, 50)), HOY));

        assertTrue(error.getMessage().startsWith("L-10"));
    }

    @Test
    @DisplayName("F-07: la nueva versión no puede regir antes que la vigente")
    void vigenciaAnteriorALaVigente() {
        servicio.registrarInicialSiNoExiste(escenario.politica);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> servicio.actualizar(NO_SHOW, tramosNuevos(), LocalDate.of(2025, 12, 31)));

        assertTrue(error.getMessage().startsWith("F-07"));
    }

    /** 20 días o más retiene 10 %, de 10 a 19 retiene 30 %, menos de 10 o no-show retiene 60 %. */
    private static List<TramoCancelacion> tramosNuevos() {
        return List.of(new TramoCancelacion(20, 10), new TramoCancelacion(10, 30), new TramoCancelacion(0, 60));
    }

    /** Reserva de APT-101 de 2 noches, del 20 al 22 de octubre de 2026, con la política dada congelada. */
    private Reserva reserva(int secuencia, PoliticaCancelacion politica) {
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
        CotizadorEstanciaService cotizador = new CotizadorEstanciaService(escenario.apartamentos,
                escenario.alojamientos);
        return Reserva.crear(CodigoReserva.generar(2026, secuencia), escenario.apartamento, estancia,
                escenario.titular, List.of(escenario.ana), CanalOrigen.PORTAL, null, LocalTime.of(16, 0),
                cotizador.cotizar(escenario.apartamento, estancia, List.of(escenario.ana), null), politica,
                escenario.alojamiento.getUmbralEdadFacturable(), EscenarioCoral.AHORA);
    }
}
