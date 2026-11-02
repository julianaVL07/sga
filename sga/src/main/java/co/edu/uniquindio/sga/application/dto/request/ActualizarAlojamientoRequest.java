package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * CU-28. PUT /api/alojamiento
 * Reemplaza nombre, descripción, ciudad, dirección, ubicación y normas de convivencia del alojamiento. El horario
 * de entrada y salida es opcional: si no llega ninguna de las dos horas se conserva el vigente.
 * Los parámetros de la sección 6 no se cambian aquí: viven en application.properties.
 * Este DTO solo revisa la forma (400); los rangos de la coordenada (7.3) y el horario completo (L-12) los valida
 * el dominio (409).
 */
public record ActualizarAlojamientoRequest(
        @NotBlank(message = "El nombre del alojamiento es obligatorio")
        String nombre,

        String descripcion,

        @NotBlank(message = "La ciudad del alojamiento es obligatoria")
        String ciudad,

        String direccion,

        @NotNull(message = "La latitud es obligatoria")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        Double longitud,

        String normaConvivencia,

        LocalTime horaEntrada,

        LocalTime horaSalida
) {
}
