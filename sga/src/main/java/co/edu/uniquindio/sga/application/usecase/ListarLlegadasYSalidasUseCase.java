package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-26. GET /api/reservas/llegadas?fecha=&pagina= y GET /api/reservas/salidas?fecha=&pagina=
 * Llegadas y salidas previstas del día (7.1, 7.6): llegadas son las reservas CONFIRMADA que entran ese día y
 * salidas las EN_CURSO que salen ese día. Sin fecha se usa hoy en la zona horaria del alojamiento (el reloj del
 * SGA). 10 resultados por página (12.4), ordenados por apartamento. Solo lee; no tiene reglas de negocio.
 * Actor: recepcionista.
 */

@Service
public class ListarLlegadasYSalidasUseCase {

    private final ReservaRepository reservaRepository;
    private final Clock reloj;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public ListarLlegadasYSalidasUseCase(ReservaRepository reservaRepository, Clock reloj,
                                         @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> llegadas(LocalDate fecha, int pagina) {
        return reservaRepository.listarLlegadasPrevistas(fechaODeHoy(fecha), pagina, tamanoPagina);
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> salidas(LocalDate fecha, int pagina) {
        return reservaRepository.listarSalidasPrevistas(fechaODeHoy(fecha), pagina, tamanoPagina);
    }

    private LocalDate fechaODeHoy(LocalDate fecha) {
        return fecha == null ? LocalDate.now(reloj) : fecha;
    }
}
