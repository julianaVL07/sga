package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * CU-30. Una versión de la política de cancelación (F-07, 7.4), con sus tramos de la mayor a la menor antelación.
 * Mientras está vigente es la versión que se congela en cada reserva nueva (RN-22).
 */
public record PoliticaCancelacionResponse(
        int version,
        LocalDate vigenteDesde,
        String consecuenciaNoShow,
        List<TramoResponse> tramos
) {

    /** L-10: desde cuántos días de antelación se retiene qué porcentaje del valor total. */
    public record TramoResponse(int antelacionMinimaDias, int porcentajeRetencion) {
    }
}
