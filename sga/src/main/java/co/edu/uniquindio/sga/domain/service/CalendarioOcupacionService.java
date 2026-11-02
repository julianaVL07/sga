package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * CU-27 (7.1): calendario de ocupación de un apartamento, noche por noche. Cada noche queda RESERVADA si la ocupa
 * una reserva activa (PENDIENTE, CONFIRMADA o EN_CURSO, RN-12), BLOQUEADA si la afecta un bloqueo vigente (RN-07)
 * o LIBRE. Una noche no puede tener las dos cosas (7.3 y RN-07 lo impiden); si llegara a pasar, gana la reserva,
 * que es la que tiene un grupo detrás.
 * Rango semiabierto [desde, hasta), como la estancia (3.1): hasta es la primera noche que no se muestra.
 */
public class CalendarioOcupacionService {

    /**
     * Decisión del equipo: el calendario abarca como máximo 62 noches (dos meses completos), suficiente para la
     * vista mensual de recepción con el mes siguiente, y acota el tamaño de la respuesta.
     */
    public static final int MAXIMO_NOCHES = 62;

    private final ReservaRepository reservaRepository;
    private final BloqueoRepository bloqueoRepository;

    public CalendarioOcupacionService(ReservaRepository reservaRepository, BloqueoRepository bloqueoRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.bloqueoRepository = Objects.requireNonNull(bloqueoRepository);
    }

    /** @param hasta primera noche que ya no se muestra (excluida). */
    public List<NocheCalendario> calendario(IdentificacionApartamento apartamento, LocalDate desde, LocalDate hasta) {
        if (apartamento == null) throw new ReglaDominioException("El apartamento es obligatorio");
        validarRango(desde, hasta);

        Estancia rango = new Estancia(desde, hasta);
        List<Reserva> reservas = reservaRepository.listarActivasQueSeSolapan(apartamento, rango);
        List<Bloqueo> bloqueos = bloqueoRepository.listarVigentesQueSeSolapan(apartamento, rango);
        return rango.fechasDeNoches().stream()
                .map(noche -> nocheDe(noche, reservas, bloqueos))
                .toList();
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaDominioException("7.1: el calendario debe tener fecha desde y fecha hasta");
        }
        if (!desde.isBefore(hasta)) {
            throw new ReglaDominioException("7.1: la fecha desde debe ser anterior a la fecha hasta");
        }
        long noches = ChronoUnit.DAYS.between(desde, hasta);
        if (noches > MAXIMO_NOCHES) {
            throw new ReglaDominioException("7.1: el calendario abarca máximo " + MAXIMO_NOCHES
                    + " noches y se pidieron " + noches);
        }
    }

    private static NocheCalendario nocheDe(LocalDate noche, List<Reserva> reservas, List<Bloqueo> bloqueos) {
        return reservas.stream()
                .filter(r -> r.estaActiva() && r.getEstancia().incluyeFecha(noche))
                .findFirst()
                .map(r -> NocheCalendario.reservada(noche, r))
                .or(() -> bloqueos.stream()
                        .filter(b -> b.afectaFecha(noche))
                        .findFirst()
                        .map(b -> NocheCalendario.bloqueada(noche, b)))
                .orElseGet(() -> NocheCalendario.libre(noche));
    }
}
