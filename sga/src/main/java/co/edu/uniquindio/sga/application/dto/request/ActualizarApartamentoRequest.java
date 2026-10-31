package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CU-23. PUT /api/apartamentos/{id}
 * Reemplaza nombre, descripción, dormitorios, dotación y capacidad. Las imágenes, tarifas y estado no cambian aquí.
 * Este DTO solo revisa la forma (400); los mínimos de capacidad, dormitorios y dotación los valida el dominio (409).
 */
public record ActualizarApartamentoRequest(
        @NotBlank(message = "El nombre del apartamento es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El número de dormitorios es obligatorio")
        Integer dormitorios,

        @NotNull(message = "La capacidad máxima es obligatoria")
        Integer capacidadMaxima,

        @NotNull(message = "La dotación es obligatoria")
        List<String> dotacion
) {
}
