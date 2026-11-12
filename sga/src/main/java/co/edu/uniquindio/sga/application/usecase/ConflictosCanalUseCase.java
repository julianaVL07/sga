package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ConflictoCanalNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.EstadoConflictoCanal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Revisión de los conflictos de canal (RN-18, 7.1). GET /api/canales/conflictos?estado=&pagina= y
 * PUT /api/canales/conflictos/{id}/resolver. Resolver solo cambia el conflicto: la reserva vigente nunca se toca.
 * Actor: administrador.
 */
@Service
public class ConflictosCanalUseCase {

    private final ConflictoCanalRepository conflictoCanalRepository;
    private final Clock reloj;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public ConflictosCanalUseCase(ConflictoCanalRepository conflictoCanalRepository, Clock reloj,
                                  @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.conflictoCanalRepository = Objects.requireNonNull(conflictoCanalRepository);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    /** Del más reciente al más antiguo; sin estado trae todos. */
    @Transactional(readOnly = true)
    public Pagina<ConflictoCanal> listar(EstadoConflictoCanal estado, int pagina) {
        List<ConflictoCanal> conflictos = estado == EstadoConflictoCanal.PENDIENTE
                ? conflictoCanalRepository.listarPendientes()
                : conflictoCanalRepository.listarTodos().stream()
                        .filter(c -> estado == null || c.estado() == estado)
                        .toList();
        return Pagina.de(conflictos, pagina, tamanoPagina);
    }

    /** @param administrador correo del administrador que lo resuelve (el sub del token). */
    @Transactional
    public ConflictoCanal resolver(String id, String motivo, String administrador) {
        ConflictoCanal conflicto = conflictoCanalRepository.buscarPorId(id)
                .orElseThrow(() -> new ConflictoCanalNoEncontradoException(id));
        conflicto.resolver(motivo, administrador, LocalDate.now(reloj));
        return conflictoCanalRepository.guardar(conflicto);
    }
}
