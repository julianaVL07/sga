package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/**
 * CU-24 (7.4): una entrada del histórico de tarifas. vigenteHasta es la fecha en que se reemplazó; es null (y
 * vigente es true) si es la tarifa que rige hoy. vigenteDesde es null solo para tarifas cargadas sin fecha.
 */
public record RegistroTarifaResponse(
        String idTemporada,
        long valorPorOcupante,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean vigente
) {
}
