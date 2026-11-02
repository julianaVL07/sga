package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.CalendarioOcupacionService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-27. GET /api/reservas/calendario?apartamento=&desde=&hasta=
 * Calendario de ocupación del apartamento noche por noche: LIBRE, RESERVADA o BLOQUEADA (7.1). Las reglas del
 * rango (desde anterior a hasta, máximo 62 noches) las aplica CalendarioOcupacionService. Solo lee.
 * Actor: recepcionista.
 */

@Service
public class ConsultarCalendarioOcupacionUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final CalendarioOcupacionService calendarioService;

    public ConsultarCalendarioOcupacionUseCase(ApartamentoRepository apartamentoRepository,
                                               CalendarioOcupacionService calendarioService) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.calendarioService = Objects.requireNonNull(calendarioService);
    }

    /** @param hasta primera noche que ya no se muestra (rango semiabierto, 3.1). */
    @Transactional(readOnly = true)
    public List<NocheCalendario> ejecutar(IdentificacionApartamento apartamento, LocalDate desde, LocalDate hasta) {
        if (apartamento == null || !apartamentoRepository.existe(apartamento)) {
            throw new ApartamentoNoEncontradoException(apartamento);
        }
        return calendarioService.calendario(apartamento, desde, hasta);
    }
}
