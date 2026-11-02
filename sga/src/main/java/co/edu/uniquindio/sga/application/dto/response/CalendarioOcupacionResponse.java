package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Calendario de ocupación de un apartamento (CU-27, 7.1): una entrada por noche, desde la fecha desde hasta la
 * noche anterior a hasta (rango semiabierto, 3.1).
 */
public record CalendarioOcupacionResponse(
        String apartamento,                   // "APT-201"
        LocalDate desde,
        LocalDate hasta,
        List<NocheCalendarioResponse> noches
) {
}
