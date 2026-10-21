// src/main/java/co/edu/uniquindio/sga/application/dto/response/ApartamentoResponse.java
package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;

import java.util.List;

/**
 * Apartamento tal como lo ve el cliente. Respuesta de CU-01 (cada apartamento disponible) y CU-16.
 * Los objetos de valor se aplanan: IdentificacionApartamento sale como "APT-101", Capacidad como un entero,
 * Dotación como lista de textos y cada Imagen como su URL.
 */
public record ApartamentoResponse(
        String identificacion,
        String nombre,
        String descripcion,
        int dormitorios,
        int capacidadMaxima,
        List<String> dotacion,
        List<String> imagenes,
        EstadoOperativo estadoOperativo
) {
}