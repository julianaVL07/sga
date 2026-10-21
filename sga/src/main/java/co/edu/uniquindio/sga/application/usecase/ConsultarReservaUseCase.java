package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-18. GET /api/reservas/{codigo}
 * Consulta el detalle de una reserva por su código (7.5): estado, estancia, titular, ocupantes y el valor y la
 * política congelados (RN-22). Solo lee; no cambia el estado de nada.
 * Actores: recepcionista o administrador.
 */

@Service
public class ConsultarReservaUseCase {

    private final ReservaRepository reservaRepository;

    public ConsultarReservaUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
    }

    @Transactional(readOnly = true)
    public Reserva ejecutar(CodigoReserva codigo) {
        if (codigo == null) {
            throw new ReservaNoEncontradaException(null);
        }
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
    }
}