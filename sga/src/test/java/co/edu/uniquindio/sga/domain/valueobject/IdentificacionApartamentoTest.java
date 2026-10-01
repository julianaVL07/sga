package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: L-04, implementada en el constructor compacto de IdentificacionApartamento. */
class IdentificacionApartamentoTest {

    @Test
    @DisplayName("L-04: la identificación se normaliza a mayúsculas y se rechaza si está en blanco")
    void normalizarAMayusculasYRechazarBlanco() {
        IdentificacionApartamento identificacion = new IdentificacionApartamento("  apt-101 ");

        assertEquals("APT-101", identificacion.codigo());
        assertEquals(new IdentificacionApartamento("APT-101"), identificacion);

        ReglaDominioException blanco = assertThrows(ReglaDominioException.class,
                () -> new IdentificacionApartamento("   "));
        assertTrue(blanco.getMessage().startsWith("L-04"));
        assertThrows(ReglaDominioException.class, () -> new IdentificacionApartamento(null));
    }
}
