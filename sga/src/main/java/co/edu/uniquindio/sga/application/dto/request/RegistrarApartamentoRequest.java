package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CU-22. POST /api/apartamentos
 * Datos del apartamento nuevo; las tarifas son opcionales. No trae el estado: nace PREPARADO e INACTIVO (7.4).
 * Este DTO solo revisa la forma (400). La identificación única (L-04), la capacidad y los dormitorios mínimos,
 * la dotación no vacía, las imágenes 1..10 con una principal (7.3) y las tarifas (7.4) las valida el dominio (409).
 */
public record RegistrarApartamentoRequest(
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacion,

        @NotBlank(message = "El nombre del apartamento es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El número de dormitorios es obligatorio")
        Integer dormitorios,

        @NotNull(message = "La capacidad máxima es obligatoria")
        Integer capacidadMaxima,

        @NotNull(message = "La dotación es obligatoria")
        List<String> dotacion,

        @NotNull(message = "Las imágenes son obligatorias")
        List<@Valid @NotNull(message = "La imagen no puede ser nula") ImagenRequest> imagenes,

        List<@Valid @NotNull(message = "La tarifa no puede ser nula") TarifaRequest> tarifas
) {
}
