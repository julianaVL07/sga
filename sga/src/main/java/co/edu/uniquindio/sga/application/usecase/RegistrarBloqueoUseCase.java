package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.RegistrarBloqueoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-13. POST /api/apartamentos/{id}/bloqueos
 * Registra un bloqueo administrativo por rango de fechas y motivo; lo rechaza si hay reservas activas en esas
 * noches (7.3, RN-01). Desde ese momento las noches no aparecen como disponibles (RN-07).
 * Actor: administrador.
 */

@Service
public class RegistrarBloqueoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final RegistrarBloqueoService registrarBloqueoService;

    public RegistrarBloqueoUseCase(ApartamentoRepository apartamentoRepository,
                                   RegistrarBloqueoService registrarBloqueoService) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.registrarBloqueoService = Objects.requireNonNull(registrarBloqueoService);
    }

    /** @param fechaFin primera noche que ya no queda bloqueada (rango semiabierto, 3.1). */
    @Transactional
    public Bloqueo ejecutar(IdentificacionApartamento apartamento, LocalDate fechaInicio, LocalDate fechaFin,
                            String motivo) {
        if (apartamento == null || !apartamentoRepository.existe(apartamento)) {
            throw new ApartamentoNoEncontradoException(apartamento);
        }
        return registrarBloqueoService.registrarBloqueo(apartamento, new RangoFechas(fechaInicio, fechaFin), motivo);
    }
}