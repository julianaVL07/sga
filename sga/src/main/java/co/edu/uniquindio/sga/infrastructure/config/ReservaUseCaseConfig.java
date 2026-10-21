package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.application.usecase.CancelarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.DeclararNoShowUseCase;
import co.edu.uniquindio.sga.application.usecase.ModificarReservaUseCase;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CancelarReservaService;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.CreacionReservaService;
import co.edu.uniquindio.sga.domain.service.DeclararNoShowService;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Casos de uso del agregado Reserva como beans de Spring: CU-03, CU-09, CU-10, CU-11, CU-12 y CU-18.
 * Cada integrante registra los suyos en su propia clase de configuración para no pisarse en Git.
 * Los servicios, los repositorios, el Clock y la política vigente los proveen DomainServiceConfig y RepositoryConfig.
 */
@Configuration
public class ReservaUseCaseConfig {

    @Bean
    public CrearReservaUseCase crearReservaUseCase(CreacionReservaService creacion,
                                                   PoliticaCancelacion politicaVigente, Clock reloj) {
        return new CrearReservaUseCase(creacion, politicaVigente, reloj);
    }

    @Bean
    public CancelarReservaUseCase cancelarReservaUseCase(ReservaRepository reservas,
                                                         CancelarReservaService cancelacion, Clock reloj) {
        return new CancelarReservaUseCase(reservas, cancelacion, reloj);
    }

    @Bean
    public ConfirmarReservaUseCase confirmarReservaUseCase(ReservaRepository reservas,
                                                           ConfirmacionReservaService confirmacion, Clock reloj) {
        return new ConfirmarReservaUseCase(reservas, confirmacion, reloj);
    }

    @Bean
    public ModificarReservaUseCase modificarReservaUseCase(ReservaRepository reservas,
                                                           ModificacionReservaService modificacion, Clock reloj) {
        return new ModificarReservaUseCase(reservas, modificacion, reloj);
    }

    @Bean
    public DeclararNoShowUseCase declararNoShowUseCase(ReservaRepository reservas,
                                                       DeclararNoShowService noShow, Clock reloj) {
        return new DeclararNoShowUseCase(reservas, noShow, reloj);
    }

    @Bean
    public ConsultarReservaUseCase consultarReservaUseCase(ReservaRepository reservas) {
        return new ConsultarReservaUseCase(reservas);
    }
}
