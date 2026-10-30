package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: 7.5 (filtros de la búsqueda), implementada en CriterioBusqueda y Dotacion. */
class CriterioBusquedaTest {

    @Test
    @DisplayName("7.5: el precio mínimo no puede superar al máximo y la capacidad mínima debe ser positiva")
    void rechazarFiltrosInconsistentes() {
        ReglaDominioException precios = assertThrows(ReglaDominioException.class,
                () -> new CriterioBusqueda(null, Dinero.de(900_000), Dinero.de(500_000), null));
        assertTrue(precios.getMessage().startsWith("7.5"));
        assertThrows(ReglaDominioException.class, () -> new CriterioBusqueda(0, null, null, null));
        assertThrows(ReglaDominioException.class, () -> new CriterioBusqueda(null, Dinero.de(-1), null, null));
    }

    @Test
    @DisplayName("7.5: los filtros vacíos no excluyen nada y el rango de precio incluye sus extremos")
    void aplicarFiltros() {
        CriterioBusqueda sinFiltros = new CriterioBusqueda(null, null, null, "  ");
        assertNull(sinFiltros.caracteristica());
        assertTrue(sinFiltros.cumpleCapacidad(new Capacidad(1)));
        assertTrue(sinFiltros.cumplePrecio(Dinero.de(5_000_000)));

        CriterioBusqueda rango = new CriterioBusqueda(4, Dinero.de(500_000), Dinero.de(900_000), null);
        assertTrue(rango.cumpleCapacidad(new Capacidad(4)));
        assertFalse(rango.cumpleCapacidad(new Capacidad(2)));
        assertTrue(rango.cumplePrecio(Dinero.de(500_000)));
        assertTrue(rango.cumplePrecio(Dinero.de(900_000)));
        assertFalse(rango.cumplePrecio(Dinero.de(900_001)));
    }

    @Test
    @DisplayName("7.5: la característica se busca dentro de la dotación sin distinguir mayúsculas ni tildes")
    void buscarCaracteristicaEnLaDotacion() {
        Dotacion dotacion = new Dotacion(List.of("Balcón con tina de hidromasaje privada", "Cocina integral"));

        assertTrue(new CriterioBusqueda(null, null, null, "HIDROMASAJE").cumpleCaracteristica(dotacion));
        assertTrue(new CriterioBusqueda(null, null, null, "balcon").cumpleCaracteristica(dotacion));
        assertFalse(new CriterioBusqueda(null, null, null, "piscina").cumpleCaracteristica(dotacion));
    }
}
