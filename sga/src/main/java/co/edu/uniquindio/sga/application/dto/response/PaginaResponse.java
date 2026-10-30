package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.repository.Pagina;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados tal como la ve el cliente (7.5, 12.4: 10 resultados por página en todos los listados).
 * Envoltorio genérico de cualquier respuesta de listado; la numeración de páginas empieza en 0.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {

    /** Convierte una Pagina del dominio aplicando el conversor a cada elemento. */
    public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, Function<D, T> conversor) {
        return new PaginaResponse<>(
                pagina.contenido().stream().map(conversor).toList(),
                pagina.numero(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas()
        );
    }
}
