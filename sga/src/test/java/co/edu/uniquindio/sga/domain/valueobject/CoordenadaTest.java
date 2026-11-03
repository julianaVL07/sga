package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: 7.3 (ubicación exacta del alojamiento, latitud entre -90 y 90 y longitud entre -180 y 180),
 * implementada en el constructor de Coordenada.
 */
class CoordenadaTest {

    @Test
    @DisplayName("7.3: la latitud -90 y 90 y la longitud -180 y 180 se aceptan (los límites están incluidos)")
    void aceptarLimites() {
        Coordenada surOeste = new Coordenada(-90, -180);
        Coordenada norteEste = new Coordenada(90, 180);

        assertEquals(-90, surOeste.latitud());
        assertEquals(-180, surOeste.longitud());
        assertEquals(90, norteEste.latitud());
        assertEquals(180, norteEste.longitud());
    }

    @Test
    @DisplayName("7.3: la latitud -90.1 o 90.1 se rechaza")
    void rechazarLatitudFueraDeRango() {
        for (double latitud : new double[] {-90.1, 90.1}) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class,
                    () -> new Coordenada(latitud, -75.559));
            assertTrue(error.getMessage().startsWith("7.3: la latitud"), String.valueOf(latitud));
        }
    }

    @Test
    @DisplayName("7.3: la longitud -180.1 o 180.1 se rechaza")
    void rechazarLongitudFueraDeRango() {
        for (double longitud : new double[] {-180.1, 180.1}) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class,
                    () -> new Coordenada(10.3975, longitud));
            assertTrue(error.getMessage().startsWith("7.3: la longitud"), String.valueOf(longitud));
        }
    }
}
