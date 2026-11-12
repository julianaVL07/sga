package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Consulta de la bitácora de eventos con los canales (7.8). GET /api/canales/eventos?canal=&desde=&hasta=&pagina=.
 * Incluye los mensajes aceptados, los rechazados y los intentos que no se autenticaron. Actor: administrador.
 */
@Service
public class BitacoraCanalUseCase {

    private final EventoCanalRepository eventoCanalRepository;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public BitacoraCanalUseCase(EventoCanalRepository eventoCanalRepository,
                                @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.eventoCanalRepository = Objects.requireNonNull(eventoCanalRepository);
        this.tamanoPagina = tamanoPagina;
    }

    /** Del evento más reciente al más antiguo. Los filtros son opcionales y las fechas incluyen los extremos. */
    @Transactional(readOnly = true)
    public Pagina<EventoCanal> listar(String canal, LocalDate desde, LocalDate hasta, int pagina) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaDominioException("7.8: la fecha desde (" + desde + ") no puede ser posterior a la fecha "
                    + "hasta (" + hasta + ")");
        }
        return Pagina.de(eventoCanalRepository.listarBitacora().stream()
                .filter(e -> e.cumpleFiltro(canal, desde, hasta))
                .toList(), pagina, tamanoPagina);
    }
}
