package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Salida (7.6): la reserva pasa a FINALIZADA y el apartamento a PENDIENTE_PREPARACION.
 *
 * Reglas: RN-17 como requisito (la salida solo se registra con el folio cerrado: saldo cero o cierre
 * autorizado por el administrador) y RN-12 (al finalizar, la reserva libera sus noches).
 * Si el folio sigue abierto con saldo cero, este servicio lo cierra; con saldo distinto de cero lo rechaza.
 */
public class LiquidacionSalidaService {

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final FolioRepository folioRepository;

    public LiquidacionSalidaService(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository,
                                    FolioRepository folioRepository) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
    }

    public void registrarSalida(CodigoReserva codigo, LocalDate fechaActual) {
        if (fechaActual == null) throw new ReglaDominioException("La fecha actual es obligatoria");
        Reserva reserva = buscarReserva(codigo);
        Folio folio = folioRepository.buscarPorReserva(reserva.getCodigo())
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(reserva.getApartamento())
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento "
                        + reserva.getApartamento().codigo()));

        if (!folio.isCerrado() && !folio.saldo().esCero()) {
            throw new ReglaDominioException("RN-17: No se puede registrar la salida de la reserva "
                    + codigo.valor() + ": el folio tiene saldo " + folio.saldo().monto()
                    + " y no tiene cierre autorizado");
        }

        reserva.registrarSalida(fechaActual);
        if (!folio.isCerrado()) {
            folio.cerrar(fechaActual);
        }
        apartamento.liberarTrasSalida();

        folioRepository.guardar(folio);
        reservaRepository.guardar(reserva);
        apartamentoRepository.guardar(apartamento);
    }

    private Reserva buscarReserva(CodigoReserva codigo) {
        if (codigo == null) throw new ReglaDominioException("El código de la reserva es obligatorio");
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaDominioException("No existe la reserva " + codigo.valor()));
    }
}
