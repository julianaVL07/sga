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
 * PENDIENTE → CONFIRMADA (7.5).
 *
 * Reglas: L-11 (si el alojamiento exige anticipo, los pagos del folio deben cubrirlo), RN-09 (hora estimada
 * de llegada registrada) y RN-21 (no se confirma una reserva vencida). RN-09 y RN-21 las valida Reserva.
 */
public class ConfirmacionReservaService {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final AlojamientoRepository alojamientoRepository;

    public ConfirmacionReservaService(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                      AlojamientoRepository alojamientoRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
    }

    /** @param ahora fecha y hora, porque el plazo de confirmación se mide en horas (RN-21). */
    public void confirmar(CodigoReserva codigo, LocalDateTime ahora) {
        Reserva reserva = buscarReserva(codigo);
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));

        if (alojamiento.isExigeAnticipo()) {
            Folio folio = folioRepository.buscarPorReserva(codigo)
                    .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));
            Dinero anticipo = reserva.valorTotal().porcentaje(alojamiento.getPorcentajeAnticipo());
            if (!folio.cubreAnticipo(anticipo)) {
                throw new ReglaDominioException("L-11: Para confirmar la reserva " + codigo.valor()
                        + " se exige un anticipo de " + anticipo.monto() + " y se han pagado "
                        + folio.totalPagado().monto());
            }
        }

        reserva.confirmar(ahora, alojamiento.getPlazoConfirmacion());
        reservaRepository.guardar(reserva);
    }

    private Reserva buscarReserva(CodigoReserva codigo) {
        if (codigo == null) throw new ReglaDominioException("El código de la reserva es obligatorio");
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaDominioException("No existe la reserva " + codigo.valor()));
    }
}
