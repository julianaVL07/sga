package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Filtros opcionales de la búsqueda de apartamentos disponibles (7.5). Un campo null significa "sin ese filtro".
 * Los precios se comparan con el valor estimado de toda la estancia, no con la tarifa de una noche.
 *
 * Los "servicios" que menciona 7.5 son del alojamiento (ServicioAdicional), no de cada apartamento, por eso no
 * hay filtro de servicios: lo que distingue a un apartamento de otro es su dotación (característica).
 */
public record CriterioBusqueda(Integer capacidadMinima, Dinero precioMinimo, Dinero precioMaximo,
                               String caracteristica) {

    public CriterioBusqueda {
        if (capacidadMinima != null && capacidadMinima <= 0) {
            throw new ReglaDominioException("7.5: la capacidad mínima debe ser mayor que cero");
        }
        if (precioMinimo != null && precioMinimo.esNegativo()) {
            throw new ReglaDominioException("7.5: el precio mínimo no puede ser negativo");
        }
        if (precioMaximo != null && precioMaximo.esNegativo()) {
            throw new ReglaDominioException("7.5: el precio máximo no puede ser negativo");
        }
        if (precioMinimo != null && precioMaximo != null && !precioMaximo.esMayorOIgualQue(precioMinimo)) {
            throw new ReglaDominioException("7.5: el precio mínimo no puede ser mayor que el precio máximo");
        }
        caracteristica = caracteristica == null || caracteristica.isBlank() ? null : caracteristica.trim();
    }

    /** Búsqueda sin filtros adicionales: solo estancia y ocupantes. */
    public static CriterioBusqueda sinFiltros() {
        return new CriterioBusqueda(null, null, null, null);
    }

    public boolean cumpleCapacidad(Capacidad capacidad) {
        return capacidadMinima == null || capacidad.maximo() >= capacidadMinima;
    }

    public boolean cumpleCaracteristica(Dotacion dotacion) {
        return caracteristica == null || dotacion.mencionaCaracteristica(caracteristica);
    }

    /** El valor estimado de la estancia debe quedar dentro del rango [precioMinimo, precioMaximo]. */
    public boolean cumplePrecio(Dinero valorEstimado) {
        boolean sobreElMinimo = precioMinimo == null || valorEstimado.esMayorOIgualQue(precioMinimo);
        boolean bajoElMaximo = precioMaximo == null || precioMaximo.esMayorOIgualQue(valorEstimado);
        return sobreElMinimo && bajoElMaximo;
    }

    public boolean filtraPorPrecio() {
        return precioMinimo != null || precioMaximo != null;
    }
}
