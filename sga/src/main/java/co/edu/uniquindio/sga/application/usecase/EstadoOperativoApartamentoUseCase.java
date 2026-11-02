package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * CU-16. PUT /api/apartamentos/{id}/iniciar-preparacion, /terminar-preparacion, /poner-fuera-de-servicio y
 * /restablecer-servicio.
 * Cada operación busca el apartamento, llama al método de negocio correspondiente y guarda. Las transiciones
 * permitidas (7.6) las valida Apartamento. El personal de servicio mueve PENDIENTE_PREPARACION → EN_PREPARACION →
 * PREPARADO; FUERA_DE_SERVICIO es decisión del administrador (7.1), restricción de rol que verifica la
 * infraestructura. Un cambio de estado no crea ni levanta bloqueos (3.3).
 * Actores: personal de servicio y administrador.
 */
@Service
public class EstadoOperativoApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public EstadoOperativoApartamentoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
    }

    /** PENDIENTE_PREPARACION → EN_PREPARACION. */
    @Transactional
    public Apartamento iniciarPreparacion(IdentificacionApartamento identificacion) {
        return aplicar(identificacion, Apartamento::iniciarPreparacion);
    }

    /** EN_PREPARACION → PREPARADO. */
    @Transactional
    public Apartamento terminarPreparacion(IdentificacionApartamento identificacion) {
        return aplicar(identificacion, Apartamento::terminarPreparacion);
    }

    /** Cualquier estado no ocupado → FUERA_DE_SERVICIO. */
    @Transactional
    public Apartamento ponerFueraDeServicio(IdentificacionApartamento identificacion) {
        return aplicar(identificacion, Apartamento::ponerFueraDeServicio);
    }

    /** FUERA_DE_SERVICIO → PENDIENTE_PREPARACION. */
    @Transactional
    public Apartamento restablecerServicio(IdentificacionApartamento identificacion) {
        return aplicar(identificacion, Apartamento::restablecerServicio);
    }

    private Apartamento aplicar(IdentificacionApartamento identificacion, Consumer<Apartamento> operacion) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        operacion.accept(apartamento);
        return apartamentoRepository.guardar(apartamento);
    }
}
