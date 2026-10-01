package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Cancelación de una reserva que aún no ha iniciado (7.5).
 *
 * Reglas: RN-12 (la reserva cancelada libera sus noches), RN-13 (la retención sale de la política congelada
 * en la reserva, no de la vigente), RN-16 (los cargos no se borran: se revierten con movimientos inversos
 * y la retención queda como cargo de penalidad).
 */
public class CancelarReservaService {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;

    public CancelarReservaService(ReservaRepository reservaRepository, FolioRepository folioRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
    }

    /**
     * @param motivo el catálogo usa un objeto MotivoCancelacion; como no está entre los objetos de valor,
     *               se recibe como texto.
     * @return la retención asentada en el folio (0 si se cancela con la antelación suficiente).
     */
    public Dinero cancelar(CodigoReserva codigo, LocalDate fechaCancelacion, String motivo) {
        Reserva reserva = buscarReserva(codigo);
        Folio folio = folioRepository.buscarPorReserva(reserva.getCodigo())
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));

        Dinero retencion = reserva.calcularRetencionPorCancelacion(fechaCancelacion);
        reserva.cancelar(motivo, fechaCancelacion);

        folio.anularCargos("Cancelación de la reserva " + codigo.valor(), fechaCancelacion);
        folio.registrarPenalidad(retencion, "Retención por cancelación (política versión "
                + reserva.getPoliticaCancelacion().version() + ")", fechaCancelacion);

        reservaRepository.guardar(reserva);
        folioRepository.guardar(folio);
        return retencion;
    }

    private Reserva buscarReserva(CodigoReserva codigo) {
        if (codigo == null) throw new ReglaDominioException("El código de la reserva es obligatorio");
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaDominioException("No existe la reserva " + codigo.valor()));
    }
}
