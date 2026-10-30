package co.edu.uniquindio.sga.application.dto.response;

/**
 * Tarjeta de un apartamento en el resultado de la búsqueda (CU-01, 7.5): identificación, nombre, imagen
 * principal, capacidad, valor estimado de la estancia en pesos enteros y la ubicación del alojamiento.
 */
public record ApartamentoDisponibleResponse(
        String identificacion,
        String nombre,
        String imagenPrincipal,
        int capacidadMaxima,
        long valorEstimado,
        double latitud,
        double longitud
) {
}
