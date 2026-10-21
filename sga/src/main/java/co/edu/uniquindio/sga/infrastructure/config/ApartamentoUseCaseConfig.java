package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.application.usecase.BuscarApartamentoDisponibleUseCase;
import co.edu.uniquindio.sga.application.usecase.CambiarEstadoOperativoUseCase;
import co.edu.uniquindio.sga.application.usecase.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarNovedadUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CotizadorEstanciaService;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.service.EntregaApartamentoService;
import co.edu.uniquindio.sga.domain.service.LiquidacionSalidaService;
import co.edu.uniquindio.sga.domain.service.RegistrarBloqueoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Casos de uso de apartamentos y operación como beans de Spring: CU-01, CU-02, CU-04, CU-05, CU-13, CU-16 y CU-17.
 * Cada integrante registra los suyos en su propia clase de configuración para no pisarse en Git.
 * Los servicios, los repositorios y el Clock los proveen DomainServiceConfig y RepositoryConfig.
 */
@Configuration
public class ApartamentoUseCaseConfig {

    @Bean
    public BuscarApartamentoDisponibleUseCase buscarApartamentoDisponibleUseCase(
            DisponibilidadApartamentoService disponibilidad) {
        return new BuscarApartamentoDisponibleUseCase(disponibilidad);
    }

    @Bean
    public CotizarEstanciaUseCase cotizarEstanciaUseCase(CotizadorEstanciaService cotizador) {
        return new CotizarEstanciaUseCase(cotizador);
    }

    @Bean
    public RegistrarLlegadaUseCase registrarLlegadaUseCase(ReservaRepository reservas,
                                                           EntregaApartamentoService entrega, Clock reloj) {
        return new RegistrarLlegadaUseCase(reservas, entrega, reloj);
    }

    @Bean
    public RegistrarSalidaUseCase registrarSalidaUseCase(ReservaRepository reservas,
                                                         LiquidacionSalidaService liquidacion, Clock reloj) {
        return new RegistrarSalidaUseCase(reservas, liquidacion, reloj);
    }

    @Bean
    public RegistrarBloqueoUseCase registrarBloqueoUseCase(RegistrarBloqueoService registrarBloqueo) {
        return new RegistrarBloqueoUseCase(registrarBloqueo);
    }

    @Bean
    public CambiarEstadoOperativoUseCase cambiarEstadoOperativoUseCase(ApartamentoRepository apartamentos) {
        return new CambiarEstadoOperativoUseCase(apartamentos);
    }

    @Bean
    public RegistrarNovedadUseCase registrarNovedadUseCase(ApartamentoRepository apartamentos,
                                                           NovedadRepository novedades, Clock reloj) {
        return new RegistrarNovedadUseCase(apartamentos, novedades, reloj);
    }
}