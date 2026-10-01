package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Imagen de un apartamento almacenada en un servicio externo (7.3).
 * El mínimo de 1, el máximo de 10 y la imagen principal única los valida Apartamento.
 */
public record Imagen(String url, boolean esPrincipal) {

    public Imagen {
        if (url == null || url.isBlank()) {
            throw new ReglaDominioException("7.3: la imagen debe tener una URL");
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ReglaDominioException("7.3: la URL de la imagen debe empezar con http:// o https://");
        }
    }
}
