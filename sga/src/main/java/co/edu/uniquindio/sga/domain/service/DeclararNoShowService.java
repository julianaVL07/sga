package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * El grupo no llegó: CONFIRMADA → NO_SHOW (7.5).
 *
 * Reglas: L-15 (solo desde la hora límite configurada del día de entrada, 22:00 en el Anexo A),
 * RN-12 (libera las noches), RN-13 (la consecuencia sale de la política congelada) y RN-16.
 */
public class DeclararNoShowService {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final AlojamientoRepository alojamientoRepository;

    public DeclararNoShowService(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                 AlojamientoRepository alojamientoRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
    }

    /**
     * @param ahora fecha y hora actuales; la hora sola no basta porque la hora límite es la del día de entrada.
     * @return la retención asentada en el folio.
     */
    public Dinero declararNoShow(CodigoReserva codigo, LocalDateTime ahora) {
        Reserva reserva = buscarReserva(codigo);
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
        Folio folio = folioRepository.buscarPorReserva(reserva.getCodigo())
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));

        Dinero retencion = reserva.calcularRetencionPorNoShow();
        reserva.declararNoShow(ahora, alojamiento.getHoraLimiteNoShow());

        folio.anularCargos("No-show de la reserva " + codigo.valor(), ahora.toLocalDate());
        folio.registrarPenalidad(retencion, "Retención por no-show: "
                + reserva.getPoliticaCancelacion().consecuenciaNoShow(), ahora.toLocalDate());

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
