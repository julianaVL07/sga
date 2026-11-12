package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Gravedad que el modelo de IA sugiere para una novedad, con una explicación corta (10.2, L-21).
 * Es solo una sugerencia: la gravedad que se guarda la elige siempre la persona que registra la novedad (7.6).
 */
public record SugerenciaGravedad(GravedadNovedad gravedad, String explicacion) {

    /** Largo máximo de la explicación; si llega más larga se recorta. */
    public static final int LARGO_MAXIMO_EXPLICACION = 200;

    public SugerenciaGravedad {
        if (gravedad == null) {
            throw new ReglaDominioException("10.2: la sugerencia debe indicar la gravedad");
        }
        if (explicacion == null || explicacion.isBlank()) {
            throw new ReglaDominioException("10.2: la sugerencia debe traer una explicación");
        }
        explicacion = explicacion.trim().replaceAll("\\s+", " ");
        if (explicacion.length() > LARGO_MAXIMO_EXPLICACION) {
            explicacion = explicacion.substring(0, LARGO_MAXIMO_EXPLICACION - 1).trim() + "…";
        }
    }
}
