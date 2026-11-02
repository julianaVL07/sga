package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 7.6. GET /api/apartamentos/{id}/novedades?pagina=
 * Historial de novedades de un apartamento, de la más reciente a la más antigua, 10 por página (12.4).
 * Incluye las de apartamentos retirados de la venta, porque el historial se conserva. Solo consulta.
 * Actores: personal de servicio, recepcionista y administrador.
 */
@Service
public class ConsultarNovedadesUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final NovedadRepository novedadRepository;
    private final int tamanoPagina;

    public ConsultarNovedadesUseCase(ApartamentoRepository apartamentoRepository,
                                     NovedadRepository novedadRepository,
                                     @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.novedadRepository = Objects.requireNonNull(novedadRepository);
        this.tamanoPagina = tamanoPagina;
    }

    /** 404 si el apartamento no existe. */
    @Transactional(readOnly = true)
    public Pagina<Novedad> historial(IdentificacionApartamento apartamento, int pagina) {
        if (apartamento == null || !apartamentoRepository.existe(apartamento)) {
            throw new ApartamentoNoEncontradoException(apartamento);
        }
        return novedadRepository.listarPorApartamento(apartamento, pagina, tamanoPagina);
    }
}
