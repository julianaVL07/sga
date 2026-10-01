package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.DeclararNoShowService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * CU-12. PUT /api/reservas/{codigo}/no-show
 * Desde la hora límite del día de entrada (22:00 en Coral, L-15) marca NO_SHOW, aplica la consecuencia de la
 * política congelada (RN-13) y libera las noches (RN-12). Actores: recepcionista o administrador.
 */
public class DeclararNoShowUseCase {

    private final ReservaRepository reservaRepository;
    private final DeclararNoShowService noShowService;
    private final Clock reloj;

    public DeclararNoShowUseCase(ReservaRepository reservaRepository, DeclararNoShowService noShowService, Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.noShowService = Objects.requireNonNull(noShowService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** @return la retención asentada en el folio. */
    public Dinero ejecutar(CodigoReserva codigo) {
        verificarQueExiste(codigo);
        return noShowService.declararNoShow(codigo, LocalDateTime.now(reloj));
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
