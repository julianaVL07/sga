package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.RetiroApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-21. PUT /api/apartamentos/{id}/retirar
 * Retira un apartamento de la venta si no tiene reservas activas ni futuras (7.3). La eliminación es lógica (12.4):
 * no hay DELETE, el apartamento queda marcado como eliminado y las reservas históricas lo siguen referenciando.
 * Actor: administrador.
 */

@Service
public class RetirarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final RetiroApartamentoService retiroService;
    private final Clock reloj;

    public RetirarApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                     RetiroApartamentoService retiroService, Clock reloj) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.retiroService = Objects.requireNonNull(retiroService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        if (identificacion == null || !apartamentoRepository.existe(identificacion)) {
            throw new ApartamentoNoEncontradoException(identificacion);
        }
        return retiroService.retirar(identificacion, LocalDate.now(reloj));
    }
}
