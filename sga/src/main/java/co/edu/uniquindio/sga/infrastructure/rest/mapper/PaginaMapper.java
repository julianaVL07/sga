package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.domain.repository.Pagina;

import java.util.function.Function;

/**
 * Convierte la Pagina del dominio en el PaginaResponse que ve el cliente (7.5, 12.4). Vive en infraestructura para
 * que los DTO no dependan de tipos del dominio. El JSON resultante es el mismo de siempre:
 * contenido, pagina, tamano, totalElementos y totalPaginas.
 */
public final class PaginaMapper {

    private PaginaMapper() {
    }

    /** Aplica el conversor a cada elemento y copia los datos de paginación. */
    public static <D, T> PaginaResponse<T> aResponse(Pagina<D> pagina, Function<D, T> conversor) {
        return new PaginaResponse<>(
                pagina.contenido().stream().map(conversor).toList(),
                pagina.numero(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas()
        );
    }
}
