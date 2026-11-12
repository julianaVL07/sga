package co.edu.uniquindio.sga.infraestructure.ia;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;
import co.edu.uniquindio.sga.infrastructure.ia.SugeridorGravedadDeshabilitado;
import co.edu.uniquindio.sga.infrastructure.ia.SugeridorGravedadLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: 10.2 (sugerencia de gravedad no bloqueante), adaptador local por palabras clave. */
class SugeridorGravedadLocalTest {

    private final SugeridorGravedadLocal sugeridor = new SugeridorGravedadLocal();

    @ParameterizedTest
    @ValueSource(strings = {
            "Huele a gas en la cocina",
            "Hay una FUGA de agua debajo del lavaplatos",
            "El tomacorriente eléctrico del balcón echa chispas",
            "Inundación en el baño principal",
            "Sale humo del extractor",
            "Cortocircuito en la sala"})
    @DisplayName("10.2: gas, fuga, eléctrico, inundación y otros riesgos se sugieren ALTA")
    void riesgosSonAlta(String descripcion) {
        SugerenciaGravedad sugerencia = sugeridor.sugerir(descripcion).orElseThrow();

        assertEquals(GravedadNovedad.ALTA, sugerencia.gravedad());
        assertFalse(sugerencia.explicacion().isBlank());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Copa de cristal rota en la cocina",
            "El aire acondicionado no enciende",
            "La ducha gotea toda la noche",
            "El sanitario está tapado",
            "La cerradura de la puerta del balcón está dañada"})
    @DisplayName("10.2: un daño o algo que no funciona, sin riesgo, se sugiere MEDIA")
    void danosSonMedia(String descripcion) {
        assertEquals(GravedadNovedad.MEDIA, sugeridor.sugerir(descripcion).orElseThrow().gravedad());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Falta una toalla de piscina", "Mancha en el cojín del sofá", "Faltan dos copas de vino"})
    @DisplayName("10.2: faltantes, aseo y detalles menores se sugieren BAJA")
    void detallesSonBaja(String descripcion) {
        assertEquals(GravedadNovedad.BAJA, sugeridor.sugerir(descripcion).orElseThrow().gravedad());
    }

    @Test
    @DisplayName("10.2: la explicación cita la palabra encontrada, sin tildes ni mayúsculas")
    void explicacionCitaLaPalabra() {
        SugerenciaGravedad sugerencia = sugeridor.sugerir("Problema ELÉCTRICO en la nevera").orElseThrow();

        assertEquals(GravedadNovedad.ALTA, sugerencia.gravedad());
        assertTrue(sugerencia.explicacion().contains("electrico"));
    }

    @Test
    @DisplayName("10.2: las palabras se comparan completas: 'gaseosa' no es 'gas'")
    void palabrasCompletas() {
        assertEquals(GravedadNovedad.BAJA,
                sugeridor.sugerir("Se regó una gaseosa en la alfombra").orElseThrow().gravedad());
    }

    @Test
    @DisplayName("10.2: si aparece un riesgo y un daño, gana el riesgo (ALTA)")
    void riesgoGanaSobreDano() {
        assertEquals(GravedadNovedad.ALTA,
                sugeridor.sugerir("La estufa está dañada y huele a gas").orElseThrow().gravedad());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("10.2: sin descripción no hay sugerencia")
    void sinDescripcionNoHaySugerencia(String descripcion) {
        assertEquals(Optional.empty(), sugeridor.sugerir(descripcion));
    }

    @Test
    @DisplayName("10.2: con la IA deshabilitada nunca hay sugerencia")
    void deshabilitadoSiempreVacio() {
        assertEquals(Optional.empty(), new SugeridorGravedadDeshabilitado().sugerir("Huele a gas en la cocina"));
    }
}
