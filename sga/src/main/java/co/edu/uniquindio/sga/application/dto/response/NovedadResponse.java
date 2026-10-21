// src/main/java/co/edu/uniquindio/sga/application/dto/response/NovedadResponse.java
package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;

import java.time.LocalDate;

/** Respuesta de CU-17: la novedad registrada. fechaAtencion es null mientras no se atienda. */
public record NovedadResponse(
        String id,
        String identificacionApartamento,
        LocalDate fecha,
        String autor,
        String descripcion,
        GravedadNovedad gravedad,
        LocalDate fechaAtencion
) {
}