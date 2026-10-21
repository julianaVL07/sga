package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.service.RegistrarBloqueoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-13. POST /api/apartamentos/{id}/bloqueos
 * Registra un bloqueo administrativo por rango de fechas y motivo; lo rechaza si hay reservas activas en esas
 * noches (7.3, RN-01). Desde ese momento las noches no aparecen como disponibles (RN-07).
 * Actor: administrador.
 */
public class RegistrarBloqueoUseCase {

    private final RegistrarBloqueoService registrarBloqueoService;

    public RegistrarBloqueoUseCase(RegistrarBloqueoService registrarBloqueoService) {
        this.registrarBloqueoService = Objects.requireNonNull(registrarBloqueoService);
    }

    /** @param fechaFin primera noche que ya no queda bloqueada (rango semiabierto, 3.1). */
    public Bloqueo ejecutar(IdentificacionApartamento apartamento, LocalDate fechaInicio, LocalDate fechaFin,
                            String motivo) {
        return registrarBloqueoService.registrarBloqueo(apartamento, new RangoFechas(fechaInicio, fechaFin), motivo);
    }
}
