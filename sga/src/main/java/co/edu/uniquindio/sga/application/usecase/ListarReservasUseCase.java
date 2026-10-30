package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-19. GET /api/reservas?estado=&apartamento=&canal=&titular=&desde=&hasta=&pagina=
 * Lista las reservas (7.5 "Listados") de la más reciente a la más antigua, con filtros opcionales y 10 resultados
 * por página (12.4). Solo lee; no tiene reglas de negocio.
 * Actores: recepcionista o administrador.
 */

@Service
public class ListarReservasUseCase {

    private final ReservaRepository reservaRepository;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public ListarReservasUseCase(ReservaRepository reservaRepository,
                                 @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.tamanoPagina = tamanoPagina;
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> ejecutar(FiltroReservas filtro, int pagina) {
        return reservaRepository.listar(filtro, pagina, tamanoPagina);
    }
}
