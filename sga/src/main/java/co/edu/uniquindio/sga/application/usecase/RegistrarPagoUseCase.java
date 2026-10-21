package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-15. POST /api/reservas/{codigo}/folio/pagos
 * Registra un abono contra el folio, con su medio y su fecha (RN-15). Admite pagos parciales y medios
 * distintos. Un pago no se edita ni se borra: una corrección es un movimiento inverso (RN-16).
 * Sirve para cubrir el anticipo antes de confirmar (L-11) y para dejar el saldo en cero antes de la salida (RN-17).
 * El sistema registra el movimiento de dinero; no lo ejecuta (glosario, "Pago").
 * Actor: recepcionista.
 */
public class RegistrarPagoUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final Clock reloj;

    public RegistrarPagoUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository, Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** @return el folio con el pago registrado, para mostrar el nuevo saldo. */
    public Folio ejecutar(CodigoReserva codigo, Dinero valor, MedioPago medio) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));
        folio.registrarPago(new Pago(valor, medio, LocalDate.now(reloj)));
        return folioRepository.guardar(folio);
    }
}
