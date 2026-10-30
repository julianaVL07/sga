package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.valueobject.CriterioBusqueda;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-07, implementada en DisponibilidadApartamentoService (validarDisponibilidad);
 * 7.5 (filtros de la búsqueda), implementada en DisponibilidadApartamentoService.buscarDisponibles con criterio.
 */
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

    /** Martes 24 a jueves 26 de noviembre de 2026: 2 noches en temporada base ($310.000 por ocupante). */
    private static final Estancia DOS_NOCHES_BASE = new Estancia(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 26));

    @Test
    @DisplayName("7.5: sin fechas de nacimiento todos los ocupantes son facturables en el valor estimado")
    void estimarValorSinFechasDeNacimiento() {
        List<ApartamentoDisponible> disponibles = disponibilidad.buscarDisponibles(DOS_NOCHES_BASE, 2, null, null);

        assertEquals(1, disponibles.size());
        assertEquals(Dinero.de(1_240_000), disponibles.get(0).valorEstimado());   // 310.000 × 2 ocupantes × 2 noches
        assertEquals(escenario.alojamiento.getUbicacion(), disponibles.get(0).ubicacion());
    }

    @Test
    @DisplayName("7.5 y RN-06: con fechas de nacimiento un niño menor del umbral no se cobra")
    void estimarValorConFechasDeNacimiento() {
        List<LocalDate> fechas = List.of(LocalDate.of(1990, 5, 20), LocalDate.of(2024, 1, 10));
        List<ApartamentoDisponible> disponibles = disponibilidad.buscarDisponibles(DOS_NOCHES_BASE, 2, fechas, null);

        assertEquals(Dinero.de(620_000), disponibles.get(0).valorEstimado());
        assertThrows(ReglaDominioException.class,
                () -> disponibilidad.buscarDisponibles(DOS_NOCHES_BASE, 3, fechas, null));
    }

    @Test
    @DisplayName("7.5: los filtros de capacidad, característica y precio se aplican sobre los disponibles")
    void filtrarDisponibles() {
        assertTrue(buscar(new CriterioBusqueda(4, null, null, null)).isEmpty());
        assertEquals(1, buscar(new CriterioBusqueda(3, null, null, null)).size());
        assertEquals(1, buscar(new CriterioBusqueda(null, null, null, "AIRE")).size());
        assertTrue(buscar(new CriterioBusqueda(null, null, null, "piscina")).isEmpty());
        assertTrue(buscar(new CriterioBusqueda(null, null, Dinero.de(1_000_000), null)).isEmpty());
        assertEquals(1, buscar(new CriterioBusqueda(null, Dinero.de(1_240_000), Dinero.de(1_240_000), null)).size());
    }

    @Test
    @DisplayName("7.5: un bloqueo saca al apartamento aunque cumpla todos los filtros (la disponibilidad va primero)")
    void filtrosNoSaltanLaDisponibilidad() {
        escenario.bloqueos.guardar(Bloqueo.registrar("BLQ-2", EscenarioCoral.APT_101,
                new RangoFechas(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 26)), "Pintura"));

        assertTrue(buscar(CriterioBusqueda.sinFiltros()).isEmpty());
    }

    private List<ApartamentoDisponible> buscar(CriterioBusqueda criterio) {
        return disponibilidad.buscarDisponibles(DOS_NOCHES_BASE, 2, null, criterio);
    }
}
