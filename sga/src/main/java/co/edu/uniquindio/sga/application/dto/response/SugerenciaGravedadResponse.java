package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;

/**
 * Sugerencia de gravedad de la IA (10.2). Si no hay sugerencia, disponible es false y gravedad y explicacion
 * vienen en null; el formulario sigue funcionando igual.
 */
public record SugerenciaGravedadResponse(
        boolean disponible,
        GravedadNovedad gravedad,
        String explicacion
) {

    public static SugerenciaGravedadResponse de(SugerenciaGravedad sugerencia) {
        return new SugerenciaGravedadResponse(true, sugerencia.gravedad(), sugerencia.explicacion());
    }

    public static SugerenciaGravedadResponse noDisponible() {
        return new SugerenciaGravedadResponse(false, null, null);
    }
}
