package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.valueobject.CriterioBusqueda;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinima;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-01 (reservas activas solapadas), RN-07 (bloqueos) y RN-20 (tiempo de preparación),
 * implementadas en DisponibilidadApartamentoService (estaDisponible, validarDisponibilidad, conflictoDeInventario);
 * RP-01 (estancia mínima de fin de semana), que el servicio aplica con Alojamiento.validarEstanciaMinima;
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

    // ==================== RN-01: sin reservas activas solapadas ====================

    /** Martes 24 a viernes 27 de noviembre de 2026, temporada base. */
    private static final Estancia RESERVADA = new Estancia(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 27));

    @Test
    @DisplayName("RN-01: con una reserva activa que se solapa el apartamento no está disponible")
    void rechazarDisponibilidadConReservaActivaSolapada() {
        escenario.reservaPendiente(1, RESERVADA, LocalTime.of(16, 0));
        Estancia solapada = new Estancia(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 28));

        assertFalse(disponibilidad.estaDisponible(escenario.apartamento, solapada, 1, null));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> disponibilidad.validarDisponibilidad(escenario.apartamento, solapada, 1, null));
        assertTrue(error.getMessage().startsWith("RN-01"));
        assertTrue(disponibilidad.buscarDisponibles(solapada, 1).isEmpty());
    }

    @Test
    @DisplayName("RN-01: la misma reserva CANCELADA ya no ocupa las noches y el apartamento está disponible")
    void permitirDisponibilidadSiLaReservaSolapadaEstaCancelada() {
        Reserva reserva = escenario.reservaPendiente(1, RESERVADA, LocalTime.of(16, 0));
        reserva.cancelar("El huésped desistió del viaje", LocalDate.of(2026, 10, 2));
        escenario.reservas.guardar(reserva);
        Estancia solapada = new Estancia(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 28));

        assertTrue(disponibilidad.estaDisponible(escenario.apartamento, solapada, 1, null));
        assertDoesNotThrow(() -> disponibilidad.validarDisponibilidad(escenario.apartamento, solapada, 1, null));
    }

    @Test
    @DisplayName("RN-01: una estancia que empieza el día de salida de otra no se solapa con ella")
    void estanciaQueEmpiezaElDiaDeSalidaNoSeSolapa() {
        Reserva reserva = escenario.reservaPendiente(1, RESERVADA, LocalTime.of(16, 0));
        Estancia siguiente = new Estancia(RESERVADA.fechaSalida(), RESERVADA.fechaSalida().plusDays(2));

        assertFalse(reserva.ocupaNochesDe(siguiente));
        assertFalse(RESERVADA.seSolapaCon(siguiente));
        assertTrue(disponibilidad.estaDisponible(escenario.apartamento, siguiente, 1, null));
    }

    // ==================== RN-20: tiempo de preparación entre salida y entrada ====================

    @Test
    @DisplayName("RN-20: con los parámetros del Coral (ventana de 4 h, preparación de 3 h) se entra el mismo día de una salida")
    void permitirEntradaElMismoDiaDeUnaSalidaConParametrosDelCoral() {
        escenario.reservaPendiente(1, RESERVADA, LocalTime.of(16, 0));
        Estancia entraElDiaDeLaSalida = new Estancia(RESERVADA.fechaSalida(), RESERVADA.fechaSalida().plusDays(2));
        Estancia saleElDiaDeLaEntrada = new Estancia(RESERVADA.fechaEntrada().minusDays(2), RESERVADA.fechaEntrada());

        assertTrue(escenario.alojamiento.permiteEntradaElMismoDiaDeUnaSalida());
        assertDoesNotThrow(() -> disponibilidad.validarDisponibilidad(escenario.apartamento, entraElDiaDeLaSalida, 1, null));
        assertTrue(disponibilidad.estaDisponible(escenario.apartamento, saleElDiaDeLaEntrada, 1, null));
    }

    @Test
    @DisplayName("RN-20: si la preparación (5 h) excede la ventana de 4 h no se entra el mismo día de una salida")
    void rechazarEntradaElMismoDiaSiLaPreparacionExcedeLaVentana() {
        configurarCoral(new TiempoPreparacion(5));
        escenario.reservaPendiente(1, RESERVADA, LocalTime.of(16, 0));
        Estancia entraElDiaDeLaSalida = new Estancia(RESERVADA.fechaSalida(), RESERVADA.fechaSalida().plusDays(2));

        assertFalse(escenario.alojamiento.permiteEntradaElMismoDiaDeUnaSalida());
        assertFalse(disponibilidad.estaDisponible(escenario.apartamento, entraElDiaDeLaSalida, 1, null));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> disponibilidad.validarDisponibilidad(escenario.apartamento, entraElDiaDeLaSalida, 1, null));
        assertTrue(error.getMessage().startsWith("RN-20"));
        // Un día después de la salida ya hay tiempo de prepararlo
        Estancia unDiaDespues = new Estancia(RESERVADA.fechaSalida().plusDays(1), RESERVADA.fechaSalida().plusDays(3));
        assertTrue(disponibilidad.estaDisponible(escenario.apartamento, unDiaDespues, 1, null));
    }

    // ==================== RP-01: estancia mínima de fin de semana ====================

    /** Viernes 9 al sábado 10 de octubre de 2026, dentro de la temporada alta del escenario (5 al 14 de octubre). */
    private static final Estancia VIERNES_ALTA_UNA_NOCHE = new Estancia(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10));

    @Test
    @DisplayName("RP-01: una sola noche de viernes en temporada Alta se rechaza")
    void rechazarUnaNocheDeViernesEnTemporadaAlta() {
        ReglaDominioException enBusqueda = assertThrows(ReglaDominioException.class,
                () -> disponibilidad.buscarDisponibles(VIERNES_ALTA_UNA_NOCHE, 1));
        ReglaDominioException enValidacion = assertThrows(ReglaDominioException.class,
                () -> disponibilidad.validarDisponibilidad(escenario.apartamento, VIERNES_ALTA_UNA_NOCHE, 1, null));

        assertTrue(enBusqueda.getMessage().startsWith("RP-01"));
        assertTrue(enValidacion.getMessage().startsWith("RP-01"));
    }

    @Test
    @DisplayName("RP-01: una sola noche de sábado en temporada Media se rechaza")
    void rechazarUnaNocheDeSabadoEnTemporadaMedia() {
        agregarTemporadaMediaDeDiciembre();
        Estancia sabadoUnaNoche = new Estancia(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 6));

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> disponibilidad.validarDisponibilidad(escenario.apartamento, sabadoUnaNoche, 1, null));
        assertTrue(error.getMessage().startsWith("RP-01"));
    }

    @Test
    @DisplayName("RP-01: con dos noches de fin de semana en temporada Alta o Media la estancia se permite")
    void permitirDosNochesDeFinDeSemanaEnTemporadaAltaOMedia() {
        agregarTemporadaMediaDeDiciembre();
        Estancia viernesYSabadoAlta = new Estancia(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 11));
        Estancia viernesYSabadoMedia = new Estancia(LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 6));

        assertEquals(1, disponibilidad.buscarDisponibles(viernesYSabadoAlta, 1).size());
        assertDoesNotThrow(() -> disponibilidad.validarDisponibilidad(escenario.apartamento, viernesYSabadoAlta, 1, null));
        assertDoesNotThrow(() -> disponibilidad.validarDisponibilidad(escenario.apartamento, viernesYSabadoMedia, 1, null));
    }

    @Test
    @DisplayName("RP-01: en temporada Base se permite una sola noche de viernes")
    void permitirUnaNocheDeViernesEnTemporadaBase() {
        Estancia viernesBase = new Estancia(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 28));

        assertEquals(1, disponibilidad.buscarDisponibles(viernesBase, 1).size());
        assertDoesNotThrow(() -> disponibilidad.validarDisponibilidad(escenario.apartamento, viernesBase, 1, null));
    }

    /** Mismos parámetros del Coral (sección 6) cambiando solo el tiempo de preparación. */
    private void configurarCoral(TiempoPreparacion tiempoPreparacion) {
        escenario.alojamiento.configurarParametros(new UmbralEdadFacturable(6), tiempoPreparacion,
                new PlazoConfirmacion(12), LocalTime.of(22, 0), LocalTime.of(20, 0), new EstanciaMinima(2, true),
                true, 50);
        escenario.alojamientos.guardar(escenario.alojamiento);
    }

    /** Temporada media del 1 al 7 de diciembre de 2026 (viernes 4 y sábado 5 dentro). */
    private void agregarTemporadaMediaDeDiciembre() {
        escenario.alojamiento.agregarTemporada(Temporada.crear("T-MEDIA", "Media", TipoTemporada.MEDIA,
                List.of(new RangoFechas(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 8)))));
        escenario.alojamientos.guardar(escenario.alojamiento);
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
