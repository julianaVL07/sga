package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CU-07. POST /api/reservas/{codigo}/folio/autorizacion-cierre
 * El administrador autoriza cerrar el folio aunque tenga saldo; queda registrado quién autoriza y por qué (RN-17).
 * Extiende a CU-06. Que solo lo ejecute el administrador lo controla la seguridad de infraestructura:
 * el dominio no conoce usuarios (glosario, "Persona y usuario").
 */
public class AutorizarCierreFolioUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final Clock reloj;

    public AutorizarCierreFolioUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                       Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Folio ejecutar(CodigoReserva codigo, String autorizadoPor, String motivo) {
        verificarQueExiste(codigo);
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));
        folio.cerrarConAutorizacion(new AutorizacionCierre(autorizadoPor, motivo, LocalDate.now(reloj)));
        return folioRepository.guardar(folio);
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
