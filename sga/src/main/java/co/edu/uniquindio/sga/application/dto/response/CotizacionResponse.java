package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de CU-02: el valor de la estancia noche a noche. Es solo informativa; la cotización que vale es la que
 * se congela al crear la reserva (RN-22).
 */
public record CotizacionResponse(
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        List<DetalleNocheResponse> detalles,
        long valorTotal                          // en pesos
) {
}