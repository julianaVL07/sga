package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.AdministracionApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-25. PUT /api/apartamentos/{id}/activar
 * Pone a la venta un apartamento. El dominio exige tarifa en todas las temporadas del alojamiento (7.4) y rechaza
 * activar uno retirado de la venta (7.3): ambos casos responden 409. Si no existe, 404.
 * Actor: administrador.
 */
@Service
public class ActivarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final AdministracionApartamentoService administracionService;

    public ActivarApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                     AdministracionApartamentoService administracionService) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.administracionService = Objects.requireNonNull(administracionService);
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        if (identificacion == null || !apartamentoRepository.existe(identificacion)) {
            throw new ApartamentoNoEncontradoException(identificacion);
        }
        return administracionService.activar(identificacion);
    }
}
