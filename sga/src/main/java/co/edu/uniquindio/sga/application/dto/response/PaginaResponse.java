package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;

/**
 * Página de resultados tal como la ve el cliente (7.5, 12.4: 10 resultados por página en todos los listados).
 * Envoltorio genérico de cualquier respuesta de listado; la numeración de páginas empieza en 0.
 * No conoce la Pagina del dominio: la conversión la hace PaginaMapper en infrastructure/rest/mapper.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
