package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: 7.5 y 12.4 (10 resultados por página), implementados en Pagina. */
class PaginaTest {

    private static final int TAMANO = 10;

    private static List<Integer> numeros(int cantidad) {
        return IntStream.rangeClosed(1, cantidad).boxed().toList();
    }

    @Test
    @DisplayName("12.4: una lista vacía produce una página vacía con 0 páginas en total")
    void listaVacia() {
        Pagina<Integer> pagina = Pagina.de(List.of(), 0, TAMANO);

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(0, pagina.totalElementos());
        assertEquals(0, pagina.totalPaginas());
    }

    @Test
    @DisplayName("12.4: la última página trae solo los elementos restantes")
    void ultimaPaginaIncompleta() {
        Pagina<Integer> primera = Pagina.de(numeros(23), 0, TAMANO);
        Pagina<Integer> ultima = Pagina.de(numeros(23), 2, TAMANO);

        assertEquals(numeros(10), primera.contenido());
        assertEquals(List.of(21, 22, 23), ultima.contenido());
        assertEquals(23, ultima.totalElementos());
        assertEquals(3, ultima.totalPaginas());
    }

    @Test
    @DisplayName("12.4: una página fuera de rango devuelve contenido vacío y conserva el total")
    void paginaFueraDeRango() {
        Pagina<Integer> pagina = Pagina.de(numeros(23), 5, TAMANO);

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(23, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
    }

    @Test
    @DisplayName("12.4: se rechazan tamaño 0, número negativo y contenido nulo")
    void rechazarParametrosInvalidos() {
        ReglaDominioException tamanoCero = assertThrows(ReglaDominioException.class,
                () -> Pagina.de(numeros(5), 0, 0));
        assertTrue(tamanoCero.getMessage().startsWith("12.4"));
        assertThrows(ReglaDominioException.class, () -> Pagina.de(numeros(5), -1, TAMANO));
        assertThrows(ReglaDominioException.class, () -> new Pagina<Integer>(null, 0, TAMANO, 0));
    }

    @Test
    @DisplayName("12.4: el contenido es una copia inmutable de la lista original")
    void contenidoInmutable() {
        List<Integer> origen = new ArrayList<>(numeros(3));
        Pagina<Integer> pagina = new Pagina<>(origen, 0, TAMANO, 3);

        origen.add(4);

        assertEquals(numeros(3), pagina.contenido());
        assertThrows(UnsupportedOperationException.class, () -> pagina.contenido().add(5));
    }
}
