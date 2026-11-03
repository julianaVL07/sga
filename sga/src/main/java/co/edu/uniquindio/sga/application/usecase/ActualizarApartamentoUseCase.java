package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.AdministracionApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoActualizacionApartamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-23. PUT /api/apartamentos/{id}
 * Actualiza nombre, descripción, dormitorios, dotación y capacidad. Cambiar la capacidad no afecta las reservas ya
 * creadas, pero el resultado lista las activas o futuras que quedan por encima de la nueva capacidad (7.3); ese
 * cálculo lo hace AdministracionApartamentoService. Un apartamento retirado de la venta responde 404.
 * Actor: administrador.
 */
@Service
public class ActualizarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final AdministracionApartamentoService administracionService;
    private final Clock reloj;

    public ActualizarApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                        AdministracionApartamentoService administracionService, Clock reloj) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.administracionService = Objects.requireNonNull(administracionService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Transactional
    public ResultadoActualizacionApartamento ejecutar(IdentificacionApartamento identificacion, String nombre,
                                                      String descripcion, int dormitorios, Dotacion dotacion,
                                                      Capacidad capacidad) {
        boolean vigente = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .filter(apartamento -> !apartamento.isEliminado())
                .isPresent();
        if (!vigente) {
            throw new ApartamentoNoEncontradoException(identificacion);
        }
        return administracionService.actualizar(identificacion, nombre, descripcion, dormitorios, dotacion,
                capacidad, LocalDate.now(reloj));
    }
}
