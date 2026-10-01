package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Bloqueo de noches de un apartamento por mantenimiento, uso propio u otro motivo (7.3).
 *
 * Reglas: RN-07 (las noches bloqueadas dejan de estar disponibles) y RN-01 (no se bloquean noches que ya
 * tienen reservas activas; lo valida Apartamento con las estancias que le entrega este servicio).
 */
public class RegistrarBloqueoService {

    private final BloqueoRepository bloqueoRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final ReservaRepository reservaRepository;

    public RegistrarBloqueoService(BloqueoRepository bloqueoRepository, ApartamentoRepository apartamentoRepository,
                                   ReservaRepository reservaRepository) {
        this.bloqueoRepository = Objects.requireNonNull(bloqueoRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
    }

    public Bloqueo registrarBloqueo(IdentificacionApartamento aptoId, RangoFechas rango, String motivo) {
        Apartamento apartamento = buscarApartamento(aptoId);
        Bloqueo bloqueo = Bloqueo.registrar("BLQ-" + UUID.randomUUID(), aptoId, rango, motivo);

        List<Estancia> estanciasActivas = reservaRepository.listarActivasPorApartamento(aptoId).stream()
                .map(Reserva::getEstancia)
                .toList();
        apartamento.registrarBloqueo(bloqueo, estanciasActivas);

        bloqueoRepository.guardar(bloqueo);
        apartamentoRepository.guardar(apartamento);
        return bloqueo;
    }

    /** Levanta un bloqueo vigente; sus noches vuelven a estar disponibles. */
    public void levantarBloqueo(IdentificacionApartamento aptoId, String idBloqueo, LocalDate fecha) {
        Apartamento apartamento = buscarApartamento(aptoId);
        apartamento.levantarBloqueo(idBloqueo, fecha);
        apartamento.getBloqueos().stream()
                .filter(b -> b.getId().equals(idBloqueo))
                .findFirst()
                .ifPresent(bloqueoRepository::guardar);
        apartamentoRepository.guardar(apartamento);
    }

    private Apartamento buscarApartamento(IdentificacionApartamento aptoId) {
        if (aptoId == null) throw new ReglaDominioException("El apartamento es obligatorio");
        return apartamentoRepository.buscarPorIdentificacion(aptoId)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + aptoId.codigo()));
    }
}
