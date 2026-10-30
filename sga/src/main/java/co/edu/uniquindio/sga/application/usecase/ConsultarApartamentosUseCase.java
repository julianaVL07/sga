package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-20. GET /api/apartamentos?pagina= y GET /api/apartamentos/{id}
 * Consulta los apartamentos del alojamiento para la administración (7.3). Los eliminados lógicamente no salen
 * en el listado ni en el detalle (12.4). Solo consulta: no modifica datos.
 * Actor: administrador.
 */

@Service
public class ConsultarApartamentosUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final int tamanoPagina;

    public ConsultarApartamentosUseCase(ApartamentoRepository apartamentoRepository,
                                        @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.tamanoPagina = tamanoPagina;
    }

    @Transactional(readOnly = true)
    public Pagina<Apartamento> listar(int pagina) {
        return apartamentoRepository.listarNoEliminados(pagina, tamanoPagina);
    }

    /** 404 si no existe o si fue retirado de la venta (eliminado lógicamente). */
    @Transactional(readOnly = true)
    public Apartamento detalle(IdentificacionApartamento identificacion) {
        return apartamentoRepository.buscarPorIdentificacion(identificacion)
                .filter(apartamento -> !apartamento.isEliminado())
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
    }
}
