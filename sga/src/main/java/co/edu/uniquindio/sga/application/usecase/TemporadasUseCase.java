package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.TemporadaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.service.CreacionTemporadaService;
import co.edu.uniquindio.sga.domain.service.CreacionTemporadaService.TarifaApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-29. GET /api/temporadas?pagina=, GET /api/temporadas/{id}, POST /api/temporadas y
 * POST /api/temporadas/{id}/rangos
 * Lista las temporadas con sus rangos, crea una temporada con la tarifa de cada apartamento activo (7.4) y agrega
 * rangos a una temporada existente. Que no haya solapes ni una segunda base lo valida el agregado Alojamiento; que
 * ningún apartamento activo quede sin tarifa, CreacionTemporadaService (409). Una temporada inexistente es 404.
 * Actor: administrador.
 */
@Service
public class TemporadasUseCase {

    /** Datos de la temporada nueva. La base no lleva rangos; las tarifas rigen desde hoy (CU-24). */
    public record SolicitudTemporada(String id, String nombre, TipoTemporada tipo, List<RangoFechas> rangos,
                                     List<TarifaApartamento> tarifas) {
    }

    private final AlojamientoRepository alojamientoRepository;
    private final CreacionTemporadaService creacionTemporadaService;
    private final Clock reloj;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public TemporadasUseCase(AlojamientoRepository alojamientoRepository,
                             CreacionTemporadaService creacionTemporadaService, Clock reloj,
                             @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.creacionTemporadaService = Objects.requireNonNull(creacionTemporadaService);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    /** Temporadas en el orden en que se registraron, 10 por página (12.4). */
    @Transactional(readOnly = true)
    public Pagina<Temporada> listar(int pagina) {
        return Pagina.de(obtenerAlojamiento().getTemporadas(), pagina, tamanoPagina);
    }

    /** Una temporada con sus rangos; es la URL de la cabecera Location al crearla o al agregarle un rango. */
    @Transactional(readOnly = true)
    public Temporada consultar(String idTemporada) {
        return obtenerAlojamiento().buscarTemporada(idTemporada)
                .orElseThrow(() -> new TemporadaNoEncontradaException(idTemporada));
    }

    @Transactional
    public Temporada crear(SolicitudTemporada solicitud) {
        if (solicitud == null) throw new ReglaDominioException("Los datos de la temporada son obligatorios");
        return creacionTemporadaService.crear(solicitud.id(), solicitud.nombre(), solicitud.tipo(),
                solicitud.rangos(), solicitud.tarifas(), LocalDate.now(reloj));
    }

    @Transactional
    public Temporada agregarRango(String idTemporada, RangoFechas rango) {
        Alojamiento alojamiento = obtenerAlojamiento();
        Temporada temporada = alojamiento.buscarTemporada(idTemporada)
                .orElseThrow(() -> new TemporadaNoEncontradaException(idTemporada));
        alojamiento.agregarRangoATemporada(idTemporada, rango);
        alojamientoRepository.guardar(alojamiento);
        return temporada;
    }

    private Alojamiento obtenerAlojamiento() {
        return alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
    }
}
