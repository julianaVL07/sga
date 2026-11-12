package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.CanalVentaNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.service.CanalVentaService;
import co.edu.uniquindio.sga.domain.service.CanalVentaService.CredencialEmitida;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Gestión de los canales de venta externos (10.3.1, 7.8). POST /api/canales, GET /api/canales,
 * PUT /api/canales/{nombre}/regenerar-credencial y PUT /api/canales/{nombre}/desactivar.
 * La credencial en claro solo sale al crear el canal o al regenerarla; el listado nunca la trae.
 * Actor: administrador.
 */
@Service
public class CanalesVentaUseCase {

    private final CanalVentaService canalVentaService;
    private final CanalVentaRepository canalVentaRepository;
    private final Clock reloj;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public CanalesVentaUseCase(CanalVentaService canalVentaService, CanalVentaRepository canalVentaRepository,
                               Clock reloj, @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.canalVentaService = Objects.requireNonNull(canalVentaService);
        this.canalVentaRepository = Objects.requireNonNull(canalVentaRepository);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    /** Crea el canal con una credencial aleatoria y la devuelve en claro una sola vez. */
    @Transactional
    public CredencialEmitida crear(String nombre) {
        return canalVentaService.registrar(nombre, LocalDateTime.now(reloj));
    }

    @Transactional(readOnly = true)
    public Pagina<CanalVenta> listar(int pagina) {
        return canalVentaRepository.listar(pagina, tamanoPagina);
    }

    /** La credencial anterior deja de servir; la nueva se devuelve en claro una sola vez. */
    @Transactional
    public CredencialEmitida regenerarCredencial(String nombre) {
        return canalVentaService.regenerarCredencial(buscar(nombre));
    }

    /** Eliminación lógica: el canal se conserva, pero sus mensajes se rechazan con 403. */
    @Transactional
    public CanalVenta desactivar(String nombre) {
        return canalVentaService.desactivar(buscar(nombre));
    }

    private CanalVenta buscar(String nombre) {
        return canalVentaRepository.buscarPorNombre(CanalVenta.normalizarNombre(nombre))
                .orElseThrow(() -> new CanalVentaNoEncontradoException(nombre));
    }
}
