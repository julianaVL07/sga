package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.service.PoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-30. GET /api/politica-cancelacion, GET /api/politica-cancelacion/versiones?pagina= y
 * PUT /api/politica-cancelacion
 * Consulta la política vigente y sus versiones, y publica una versión nueva vigente desde hoy (7.4, F-07). La versión
 * anterior se conserva y las reservas existentes siguen con la versión que congelaron al crearse (RN-22).
 * L-10 lo valida el dominio (409).
 * Actor: administrador.
 */
@Service
public class PoliticaCancelacionUseCase {

    private final PoliticaCancelacionService politicaCancelacionService;
    private final PoliticaCancelacionRepository politicaCancelacionRepository;
    private final Clock reloj;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public PoliticaCancelacionUseCase(PoliticaCancelacionService politicaCancelacionService,
                                      PoliticaCancelacionRepository politicaCancelacionRepository, Clock reloj,
                                      @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.politicaCancelacionService = Objects.requireNonNull(politicaCancelacionService);
        this.politicaCancelacionRepository = Objects.requireNonNull(politicaCancelacionRepository);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    @Transactional(readOnly = true)
    public PoliticaCancelacion vigente() {
        return politicaCancelacionService.vigente();
    }

    /** Versiones de la más reciente a la más antigua, 10 por página (12.4). */
    @Transactional(readOnly = true)
    public Pagina<PoliticaCancelacion> versiones(int pagina) {
        return politicaCancelacionRepository.listarVersiones(pagina, tamanoPagina);
    }

    @Transactional
    public PoliticaCancelacion actualizar(String consecuenciaNoShow, List<TramoCancelacion> tramos) {
        return politicaCancelacionService.actualizar(consecuenciaNoShow, tramos, LocalDate.now(reloj));
    }
}
