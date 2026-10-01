package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.Objects;

/**
 * CU-16. PUT /api/apartamentos/{id}/estado-operativo
 * Cambia el estado operativo de un apartamento solo por las transiciones de 7.6. El personal de servicio mueve
 * PENDIENTE_PREPARACION → EN_PREPARACION → PREPARADO; FUERA_DE_SERVICIO es decisión del administrador (7.1),
 * restricción de rol que verifica la infraestructura. Un cambio de estado no crea ni levanta bloqueos (3.3).
 * Actores: personal de servicio y administrador.
 */
public class CambiarEstadoOperativoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public CambiarEstadoOperativoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
    }

    public Apartamento ejecutar(IdentificacionApartamento identificacion, EstadoOperativo nuevoEstado) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + identificacion.codigo()));
        apartamento.cambiarEstadoOperativo(nuevoEstado);
        return apartamentoRepository.guardar(apartamento);
    }
}
