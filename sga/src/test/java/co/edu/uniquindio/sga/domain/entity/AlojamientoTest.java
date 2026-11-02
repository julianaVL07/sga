package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: CU-29, 7.4 (las temporadas no se solapan) y F-05 (la base no tiene rangos), implementadas
 * en Alojamiento.agregarRangoATemporada y Temporada.agregarRango.
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

    private static RangoFechas rango(int anioInicio, int mesInicio, int diaInicio, int anioFin, int mesFin, int diaFin) {
        return new RangoFechas(LocalDate.of(anioInicio, mesInicio, diaInicio), LocalDate.of(anioFin, mesFin, diaFin));
    }
}
