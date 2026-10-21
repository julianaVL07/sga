package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/** Respuesta de CU-13: el bloqueo registrado. fechaFin es la primera noche libre (rango semiabierto, 3.1). */
public record BloqueoResponse(
        String id,
        String identificacionApartamento,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String motivo,
        boolean vigente
) {
}