package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Proceso automático (lo invoca un planificador de infraestructura): cancela las reservas PENDIENTES que
 * superaron el plazo de confirmación.
 *
 * Reglas: RN-21 (vencimiento por plazo, 12 horas en el Anexo A), RN-12 (liberan sus noches de inmediato)
 * y RN-16 (los cargos del folio se revierten; un vencimiento no genera penalidad).
 */
public class VencimientoReservasService {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final AlojamientoRepository alojamientoRepository;

    public VencimientoReservasService(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                      AlojamientoRepository alojamientoRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
    }

    /** @return cuántas reservas se cancelaron. */
    public int cancelarReservasExpiradas(LocalDateTime fechaHoraActual) {
        if (fechaHoraActual == null) throw new ReglaDominioException("La fecha actual es obligatoria");
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
        PlazoConfirmacion plazo = alojamiento.getPlazoConfirmacion();

        List<Reserva> vencidas = reservaRepository.listarPorEstado(EstadoReserva.PENDIENTE).stream()
                .filter(reserva -> reserva.estaVencida(fechaHoraActual, plazo))
                .toList();

        for (Reserva reserva : vencidas) {
            reserva.vencerPorPlazoDeConfirmacion(fechaHoraActual, plazo);
            folioRepository.buscarPorReserva(reserva.getCodigo())
                    .filter(folio -> !folio.isCerrado())
                    .ifPresent(folio -> {
                        folio.anularCargos("RN-21: vencimiento del plazo de confirmación", fechaHoraActual.toLocalDate());
                        folioRepository.guardar(folio);
                    });
            reservaRepository.guardar(reserva);
        }
        return vencidas.size();
    }
}
