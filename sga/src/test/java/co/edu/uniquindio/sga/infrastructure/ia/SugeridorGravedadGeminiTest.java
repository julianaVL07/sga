package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: 10.2 (sugerencia de gravedad no bloqueante), adaptador de Gemini SIN red: valida la
 * respuesta JSON del modelo y comprueba que sin clave o sin servidor responde vacío en lugar de fallar.
 * Está en el paquete del adaptador para probar interpretar(), que es de paquete.
 */
class SugeridorGravedadGeminiTest {

    /** Puerto local cerrado: la conexión se rechaza de inmediato, sin salir a internet. */
    private static final String SIN_SERVIDOR = "http://127.0.0.1:9/v1beta";

    private final SugeridorGravedadGemini gemini = nuevo("clave-de-prueba");

    @Test
    @DisplayName("10.2: un JSON con gravedad y explicación se acepta, sin importar mayúsculas ni el bloque ```json")
    void interpretaRespuestaValida() {
        assertEquals(Optional.of(new SugerenciaGravedad(GravedadNovedad.ALTA, "Posible fuga de gas")),
                gemini.interpretar("{\"gravedad\": \"ALTA\", \"explicacion\": \"Posible fuga de gas\"}"));
        assertEquals(GravedadNovedad.MEDIA,
                gemini.interpretar("```json\n{\"gravedad\":\"media\",\"explicacion\":\"Electrodoméstico dañado\"}\n```")
                        .orElseThrow().gravedad());
    }

    @Test
    @DisplayName("10.2: una gravedad desconocida, sin explicación, texto suelto o vacío se descarta")
    void descartaRespuestasInvalidas() {
        assertTrue(gemini.interpretar("{\"gravedad\": \"URGENTE\", \"explicacion\": \"x\"}").isEmpty());
        assertTrue(gemini.interpretar("{\"gravedad\": \"ALTA\", \"explicacion\": \"  \"}").isEmpty());
        assertTrue(gemini.interpretar("{\"gravedad\": \"ALTA\"}").isEmpty());
        assertTrue(gemini.interpretar("La gravedad es ALTA").isEmpty());
        assertTrue(gemini.interpretar("").isEmpty());
        assertTrue(gemini.interpretar(null).isEmpty());
    }

    @Test
    @DisplayName("10.2: una explicación larga se recorta a una explicación corta")
    void recortaExplicacionLarga() {
        String larga = "a".repeat(500);
        SugerenciaGravedad sugerencia = gemini.interpretar(
                "{\"gravedad\": \"BAJA\", \"explicacion\": \"" + larga + "\"}").orElseThrow();

        assertEquals(SugerenciaGravedad.LARGO_MAXIMO_EXPLICACION, sugerencia.explicacion().length());
    }

    @Test
    @DisplayName("10.2: sin clave no llama a Gemini y responde vacío")
    void sinClaveRespondeVacio() {
        assertEquals(Optional.empty(), nuevo("").sugerir("Huele a gas en la cocina"));
    }

    @Test
    @DisplayName("10.2: si Gemini no responde, la sugerencia es vacía y no se lanza ningún error")
    void sinServidorRespondeVacio() {
        assertEquals(Optional.empty(), gemini.sugerir("Huele a gas en la cocina"));
    }

    private static SugeridorGravedadGemini nuevo(String clave) {
        return new SugeridorGravedadGemini(JsonMapper.builder().build(), SIN_SERVIDOR, "gemini-2.5-flash", clave,
                Duration.ofSeconds(2));
    }
}
