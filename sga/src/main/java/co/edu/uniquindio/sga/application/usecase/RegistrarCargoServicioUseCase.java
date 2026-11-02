package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * POST /api/reservas/{codigo}/folio/cargos (7.1: registrar cargos en el folio)
 * Asienta en el folio el cargo de un servicio adicional del alojamiento (L-17), con el valor y el nombre del servicio
 * y la fecha de hoy. El cargo lo genera ServicioAdicional.generarCargoPara: el servicio debe existir en el alojamiento,
 * estar activo y generar cargo, y el folio debe estar abierto (RN-16); si no, 409. Si no existe la reserva o su folio,
 * 404. El cargo no se edita ni se borra: se corrige con un movimiento inverso.
 * Actor: recepcionista.
 */
@Service
public class RegistrarCargoServicioUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final Clock reloj;

    public RegistrarCargoServicioUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                         AlojamientoRepository alojamientoRepository, Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** @return el folio con el cargo asentado, para mostrar el nuevo saldo. */
    @Transactional
    public Folio ejecutar(CodigoReserva codigo, String idServicio) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
        alojamiento.obtenerServicioAdicional(idServicio).generarCargoPara(folio, LocalDate.now(reloj));
        return folioRepository.guardar(folio);
    }
}
