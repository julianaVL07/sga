package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.util.List;

/**
 * Página de resultados que devuelven los puertos de listado (7.5, 12.4: 10 resultados por página).
 * Reemplaza a org.springframework.data.domain.Page para que el dominio no dependa de Spring.
 * La numeración de páginas empieza en 0.
 */
public record Pagina<T>(List<T> contenido, int numero, int tamano, long totalElementos) {

    public Pagina {
        if (contenido == null) {
            throw new ReglaDominioException("12.4: el contenido de la página es obligatorio");
        }
        if (numero < 0) {
            throw new ReglaDominioException("12.4: el número de página no puede ser negativo");
        }
        if (tamano <= 0) {
            throw new ReglaDominioException("12.4: el tamaño de página debe ser mayor que cero");
        }
        if (totalElementos < 0) {
            throw new ReglaDominioException("12.4: el total de elementos no puede ser negativo");
        }
        contenido = List.copyOf(contenido);
    }

    /**
     * Pagina una lista ya calculada en memoria (p. ej. la búsqueda de disponibilidad, que se decide en el dominio).
     * Una página fuera de rango devuelve contenido vacío, conservando el total de elementos.
     */
    public static <T> Pagina<T> de(List<T> todos, int numero, int tamano) {
        if (todos == null) {
            throw new ReglaDominioException("12.4: la lista a paginar es obligatoria");
        }
        if (numero < 0 || tamano <= 0) {
            // El constructor informa la regla violada.
            return new Pagina<>(List.of(), numero, tamano, todos.size());
        }
        long desde = (long) numero * tamano;
        if (desde >= todos.size()) {
            return new Pagina<>(List.of(), numero, tamano, todos.size());
        }
        int hasta = (int) Math.min(desde + tamano, todos.size());
        return new Pagina<>(todos.subList((int) desde, hasta), numero, tamano, todos.size());
    }

    /** Cantidad de páginas necesarias para mostrar todos los elementos; 0 si no hay elementos. */
    public int totalPaginas() {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }
}
