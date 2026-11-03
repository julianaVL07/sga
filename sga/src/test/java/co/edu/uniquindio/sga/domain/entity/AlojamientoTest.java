package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: CU-29, 7.4 (las temporadas no se solapan) y F-05 (la base no tiene rangos), implementadas
 * en Alojamiento.agregarRangoATemporada y Temporada.agregarRango; L-03 (máximo 15 apartamentos,
 * Alojamiento.agregarApartamento), L-12 (horario con entrada y salida, Alojamiento.definirHorario) y L-17 (solo se
 * cobran los servicios que ofrece el alojamiento, Alojamiento.obtenerServicioAdicional).
 */
class AlojamientoTest {

    private final Alojamiento alojamiento = Alojamiento.crear("ALO-1", "Coral", null, "Cartagena", "Bocagrande",
            new Coordenada(10.40, -75.55), LocalTime.of(15, 0), LocalTime.of(11, 0), null);

    AlojamientoTest() {
        alojamiento.agregarTemporada(Temporada.crearBase("T-BASE", "Base"));
        alojamiento.agregarTemporada(Temporada.crear("T-MEDIA", "Media", TipoTemporada.MEDIA,
                List.of(rango(2026, 11, 6, 2026, 11, 17))));
        alojamiento.agregarTemporada(Temporada.crear("T-ALTA", "Alta", TipoTemporada.ALTA,
                List.of(rango(2026, 12, 10, 2027, 1, 21))));
    }

    @Test
    @DisplayName("CU-29: un rango libre se agrega a la temporada y define la temporada de esas noches (RN-05)")
    void agregarRangoLibre() {
        alojamiento.agregarRangoATemporada("T-MEDIA", rango(2027, 2, 1, 2027, 2, 5));

        Temporada media = alojamiento.buscarTemporada("T-MEDIA").orElseThrow();
        assertEquals(2, media.getRangos().size());
        assertEquals(media, alojamiento.obtenerTemporadaPara(LocalDate.of(2027, 2, 3)));
    }

    @Test
    @DisplayName("7.4: el rango no puede solaparse con otra temporada")
    void rangoSolapadoConOtraTemporada() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> alojamiento.agregarRangoATemporada("T-MEDIA", rango(2026, 12, 20, 2026, 12, 24)));

        assertTrue(error.getMessage().startsWith("7.4"));
        assertTrue(error.getMessage().contains("Alta"));
        assertEquals(1, alojamiento.buscarTemporada("T-MEDIA").orElseThrow().getRangos().size());
    }

    @Test
    @DisplayName("7.4: el rango no puede solaparse con otro rango de la misma temporada")
    void rangoSolapadoConLaMismaTemporada() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> alojamiento.agregarRangoATemporada("T-MEDIA", rango(2026, 11, 10, 2026, 11, 20)));

        assertTrue(error.getMessage().startsWith("7.4"));
    }

    @Test
    @DisplayName("F-05: la temporada base no tiene rangos; una temporada inexistente se rechaza")
    void baseEInexistente() {
        ReglaDominioException base = assertThrows(ReglaDominioException.class,
                () -> alojamiento.agregarRangoATemporada("T-BASE", rango(2027, 3, 1, 2027, 3, 5)));
        assertTrue(base.getMessage().startsWith("F-05"));

        ReglaDominioException inexistente = assertThrows(ReglaDominioException.class,
                () -> alojamiento.agregarRangoATemporada("T-NO-EXISTE", rango(2027, 3, 1, 2027, 3, 5)));
        assertTrue(inexistente.getMessage().startsWith("7.4"));
    }

    // ---------------------------------------------------------------- L-03

    @Test
    @DisplayName("L-03: el alojamiento acepta hasta 15 apartamentos")
    void aceptarHastaQuinceApartamentos() {
        for (int numero = 1; numero <= 15; numero++) {
            alojamiento.agregarApartamento(apartamento(numero));
        }

        assertEquals(15, alojamiento.getApartamentos().size());
        assertTrue(alojamiento.contieneApartamento(new IdentificacionApartamento("APT-115")));
    }

    @Test
    @DisplayName("L-03: el apartamento 16 se rechaza y el alojamiento se queda con 15")
    void rechazarMasDeQuinceApartamentos() {
        for (int numero = 1; numero <= 15; numero++) {
            alojamiento.agregarApartamento(apartamento(numero));
        }

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> alojamiento.agregarApartamento(apartamento(16)));

        assertTrue(error.getMessage().startsWith("L-03"));
        assertEquals(15, alojamiento.getApartamentos().size());
    }

    // ---------------------------------------------------------------- L-12

    @Test
    @DisplayName("L-12: un horario con hora de entrada y de salida se guarda")
    void definirHorarioCompleto() {
        alojamiento.definirHorario(LocalTime.of(14, 0), LocalTime.of(10, 0));

        assertEquals(LocalTime.of(14, 0), alojamiento.getHoraEntrada());
        assertEquals(LocalTime.of(10, 0), alojamiento.getHoraSalida());
    }

    @Test
    @DisplayName("L-12: un horario sin hora de entrada o sin hora de salida se rechaza y no cambia el horario")
    void rechazarHorarioSinHoraDeEntradaOSalida() {
        ReglaDominioException sinEntrada = assertThrows(ReglaDominioException.class,
                () -> alojamiento.definirHorario(null, LocalTime.of(10, 0)));
        assertTrue(sinEntrada.getMessage().contains("L-12"));
        assertTrue(sinEntrada.getMessage().contains("entrada"));

        ReglaDominioException sinSalida = assertThrows(ReglaDominioException.class,
                () -> alojamiento.definirHorario(LocalTime.of(14, 0), null));
        assertTrue(sinSalida.getMessage().contains("L-12"));
        assertTrue(sinSalida.getMessage().contains("salida"));

        assertEquals(LocalTime.of(15, 0), alojamiento.getHoraEntrada());
        assertEquals(LocalTime.of(11, 0), alojamiento.getHoraSalida());
    }

    // ---------------------------------------------------------------- L-17

    @Test
    @DisplayName("L-17: un servicio que el alojamiento no ofrece se rechaza; uno que ofrece se obtiene")
    void rechazarServicioQueElAlojamientoNoOfrece() {
        ServicioAdicional recepcion = ServicioAdicional.crearConCargo("SRV-1", "Recepción nocturna", null,
                Dinero.de(70_000));
        alojamiento.agregarServicioAdicional(recepcion);

        assertSame(recepcion, alojamiento.obtenerServicioAdicional("SRV-1"));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> alojamiento.obtenerServicioAdicional("SRV-NO-EXISTE"));
        assertTrue(error.getMessage().startsWith("L-17"));
        assertTrue(error.getMessage().contains("SRV-NO-EXISTE"));
    }

    private static Apartamento apartamento(int numero) {
        String codigo = "APT-" + (100 + numero);
        return Apartamento.crear(new IdentificacionApartamento(codigo), "Apartamento " + codigo, null, 1,
                new Capacidad(3), new Dotacion(List.of("Aire acondicionado")),
                List.of(new Imagen("https://coral.co/" + codigo + ".jpg", true)));
    }

    private static RangoFechas rango(int anioInicio, int mesInicio, int diaInicio, int anioFin, int mesFin, int diaFin) {
        return new RangoFechas(LocalDate.of(anioInicio, mesInicio, diaInicio), LocalDate.of(anioFin, mesFin, diaFin));
    }
}
