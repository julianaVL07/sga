package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CU-18. Consultar Reserva.
 * GET /api/reservas/{codigo}
 * Caso de uso de consulta que recupera una reserva por su código único de dominio.
 * Utiliza @Transactional(readOnly = true) para optimizar la lectura y no alterar el estado.
 */
@Service
public class ConsultarReservaUseCase {

    private final ReservaRepository reservaRepository;

    public ConsultarReservaUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository, "El repositorio de reservas es obligatorio");
    }

    /**
     * Ejecuta la consulta de la reserva.
     *
     * @param codigo Código único de la reserva (ej. RES-2026-00001).
     * @return La entidad Reserva encontrada.
     * @throws ReservaNoEncontradaException Si la reserva no existe en el sistema (traducido a HTTP 404).
     */
    @Transactional(readOnly = true)
    public Reserva ejecutar(CodigoReserva codigo) {
        if (codigo == null) {
            throw new ReservaNoEncontradaException(null);
        }
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
    }
}