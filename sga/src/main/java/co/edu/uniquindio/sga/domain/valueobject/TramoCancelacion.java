package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Tramo de la política de cancelación (L-10): a partir de cierta antelación en días se retiene un porcentaje
 * del valor total de la estancia. Anexo A: 16 días o más → 0 %, de 7 a 15 → 25 %, menos de 7 o no-show → 50 %.
 */
public record TramoCancelacion(int antelacionMinimaDias, int porcentajeRetencion) {

    public TramoCancelacion {
        if (antelacionMinimaDias < 0) {
            throw new ReglaDominioException("L-10: la antelación mínima no puede ser negativa");
        }
        if (porcentajeRetencion < 0 || porcentajeRetencion > 100) {
            throw new ReglaDominioException("L-10: el porcentaje de retención debe estar entre 0 y 100");
        }
    }

    public boolean aplicaPara(int diasAntelacion) {
        return diasAntelacion >= antelacionMinimaDias;
    }

    public Dinero calcularRetencion(Dinero valorTotal) {
        if (valorTotal == null) {
            throw new ReglaDominioException("RN-13: el valor total es obligatorio");
        }
        return valorTotal.porcentaje(porcentajeRetencion);
    }
}
