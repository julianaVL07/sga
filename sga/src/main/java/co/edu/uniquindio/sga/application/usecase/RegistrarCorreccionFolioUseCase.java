package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-33. POST /api/reservas/{codigo}/folio/correcciones
 * (RN-16: toda corrección es un movimiento inverso)
 * Corrige un cargo del folio sin editarlo ni borrarlo: Folio.registrarMovimientoInverso asienta un cargo del mismo
 * tipo con el valor opuesto, con el motivo y la fecha de hoy. El cargo se identifica por su posición en la lista de
 * cargos del folio (desde 0). Si no hay un cargo en esa posición o el folio está cerrado, 409 (RN-16). Si no existe la
 * reserva o su folio, 404.
 * Actor: recepcionista.
 */
@Service
public class RegistrarCorreccionFolioUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final Clock reloj;

    public RegistrarCorreccionFolioUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                           Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** @return el folio con el movimiento inverso asentado, para mostrar el saldo corregido. */
    @Transactional
    public Folio ejecutar(CodigoReserva codigo, int indiceCargo, String motivo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
        folio.registrarMovimientoInverso(folio.cargoEn(indiceCargo), motivo, LocalDate.now(reloj));
        return folioRepository.guardar(folio);
    }
}
