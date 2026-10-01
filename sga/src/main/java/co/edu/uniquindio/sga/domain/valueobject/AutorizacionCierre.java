package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Autorización explícita del administrador para cerrar un folio con saldo distinto de cero.
 * Reglas: RN-17 (queda registrada con autor y motivo).
 */
public record AutorizacionCierre(String autorizadoPor, String motivo, LocalDate fecha) {

    public AutorizacionCierre {
        if (autorizadoPor == null || autorizadoPor.isBlank()) {
            throw new ReglaDominioException("RN-17: la autorización debe indicar quién autoriza");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("RN-17: la autorización debe indicar el motivo");
        }
        if (fecha == null) {
            throw new ReglaDominioException("RN-17: la autorización debe indicar la fecha");
        }
        autorizadoPor = autorizadoPor.trim();
        motivo = motivo.trim();
    }
}
