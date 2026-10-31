package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * CU-22. Una imagen del apartamento (7.3). Que la URL empiece por http(s), que haya entre 1 y 10 imágenes y que
 * exactamente una sea la principal lo valida el dominio (409).
 */
public record ImagenRequest(
        @NotBlank(message = "La URL de la imagen es obligatoria")
        String url,

        boolean principal
) {
}
