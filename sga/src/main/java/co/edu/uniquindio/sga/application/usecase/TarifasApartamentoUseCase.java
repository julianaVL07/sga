package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.service.AdministracionApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RegistroTarifa;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-24. PUT /api/apartamentos/{id}/tarifas y GET /api/apartamentos/{id}/tarifas/historico?pagina=
 * Define una o varias tarifas por temporada desde hoy; la tarifa anterior se conserva en el histórico con la fecha
 * desde la que estuvo vigente y la fecha en que se reemplazó (7.4). Que la temporada exista y el valor sea mayor
 * que cero lo valida el dominio (409). Un apartamento retirado de la venta responde 404.
 * Actor: administrador.
 */
@Service
public class TarifasApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final AdministracionApartamentoService administracionService;
    private final Clock reloj;
    private final int tamanoPagina;

    public TarifasApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                     AdministracionApartamentoService administracionService, Clock reloj,
                                     @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.administracionService = Objects.requireNonNull(administracionService);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    @Transactional
    public Apartamento definir(IdentificacionApartamento identificacion, List<Tarifa> tarifas) {
        buscarVigente(identificacion);
        return administracionService.definirTarifas(identificacion, tarifas, LocalDate.now(reloj));
    }

    /** Histórico de la más reciente a la más antigua, 10 por página (12.4). */
    @Transactional(readOnly = true)
    public Pagina<RegistroTarifa> historico(IdentificacionApartamento identificacion, int pagina) {
        return Pagina.de(buscarVigente(identificacion).getHistoricoTarifas(), pagina, tamanoPagina);
    }

    private Apartamento buscarVigente(IdentificacionApartamento identificacion) {
        return apartamentoRepository.buscarPorIdentificacion(identificacion)
                .filter(apartamento -> !apartamento.isEliminado())
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
    }
}
