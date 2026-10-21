package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/**
 * Una noche de la cotización (RN-06): su temporada, la tarifa por ocupante aplicada, cuántos ocupantes pagan
 * (RN-05) y el subtotal. Los valores van en pesos enteros.
 */
public record DetalleNocheResponse(
        LocalDate fecha,
        String temporada,
        long tarifaAplicada,
        int ocupantesFacturables,
        long subtotal
) {
}