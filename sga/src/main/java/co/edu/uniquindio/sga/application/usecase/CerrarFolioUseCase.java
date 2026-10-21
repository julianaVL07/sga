package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-06. PUT /api/reservas/{codigo}/folio/cerrar
 * Cierra la cuenta de la reserva solo si el saldo es exactamente cero (RN-17). Con saldo distinto de cero
 * se usa CU-07 (AutorizarCierreFolioUseCase), que solo puede ejecutar el administrador.
 * Actores: recepcionista o administrador.
 */
public class CerrarFolioUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final Clock reloj;

    public CerrarFolioUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository, Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Folio ejecutar(CodigoReserva codigo) {
        verificarQueExiste(codigo);
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));
        folio.cerrar(LocalDate.now(reloj));
        return folioRepository.guardar(folio);
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
