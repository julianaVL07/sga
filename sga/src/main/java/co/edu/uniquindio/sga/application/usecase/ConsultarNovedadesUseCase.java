package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.NovedadNoEncontradaException;
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
 *
 * CU-17. GET /api/apartamentos/{id}/novedades/{idNovedad}
 * Consulta una novedad del apartamento; es el recurso al que apunta el Location de POST .../novedades.
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
        validarQueExiste(apartamento);
        return novedadRepository.listarPorApartamento(apartamento, pagina, tamanoPagina);
    }

    /** 404 si el apartamento no existe, si la novedad no existe o si la novedad es de otro apartamento. */
    @Transactional(readOnly = true)
    public Novedad consultar(IdentificacionApartamento apartamento, String idNovedad) {
        validarQueExiste(apartamento);
        return novedadRepository.buscarPorId(idNovedad)
                .filter(novedad -> novedad.perteneceA(apartamento))
                .orElseThrow(() -> new NovedadNoEncontradaException(apartamento, idNovedad));
    }

    private void validarQueExiste(IdentificacionApartamento apartamento) {
        if (apartamento == null || !apartamentoRepository.existe(apartamento)) {
            throw new ApartamentoNoEncontradoException(apartamento);
        }
    }
}
