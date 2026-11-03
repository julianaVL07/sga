package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Resultado de la búsqueda de disponibilidad (7.5): el apartamento vendible, el valor estimado de la estancia
 * para el grupo y la ubicación del alojamiento, que la tarjeta del resultado muestra en el mapa.
 */
public record ApartamentoDisponible(Apartamento apartamento, Dinero valorEstimado, Coordenada ubicacion) {

    public ApartamentoDisponible {
        if (apartamento == null || valorEstimado == null || ubicacion == null) {
            throw new ReglaDominioException("7.5: el resultado de la búsqueda exige apartamento, valor y ubicación");
        }
    }
}
