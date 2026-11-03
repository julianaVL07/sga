package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-31. GET /api/reservas/{codigo}/folio
 * Consulta el folio de una reserva: cargos, pagos, saldo y cierre (7.7). Es la URL que devuelve la cabecera
 * Location al registrar un pago (CU-15). Solo lee; si no existe la reserva o su folio responde 404.
 * Actor: recepcionista.
 */
@Service
public class ConsultarFolioUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;

    public ConsultarFolioUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
    }

    @Transactional(readOnly = true)
    public Folio ejecutar(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
        return folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
    }
}
