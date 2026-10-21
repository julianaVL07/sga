package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.EntregaApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-04. PUT /api/reservas/{codigo}/registrar-llegada
 * Registro del grupo: la reserva pasa a EN_CURSO y el apartamento a OCUPADO.
 * Reglas RN-10 (reserva CONFIRMADA y fecha de entrada alcanzada) y RN-11 (apartamento PREPARADO).
 * Actor: recepcionista.
 */
public class RegistrarLlegadaUseCase {

    private final ReservaRepository reservaRepository;
    private final EntregaApartamentoService entregaService;
    private final Clock reloj;

    public RegistrarLlegadaUseCase(ReservaRepository reservaRepository, EntregaApartamentoService entregaService,
                                   Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.entregaService = Objects.requireNonNull(entregaService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public void ejecutar(CodigoReserva codigo) {
        verificarQueExiste(codigo);
        entregaService.autorizarEntrega(codigo, LocalDate.now(reloj));
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
