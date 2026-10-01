package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de reservas. */
public interface ReservaRepository {

    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorCodigo(CodigoReserva codigo);

    /** RN-19: la combinación canal + identificador externo es única. */
    Optional<Reserva> buscarPorCanalEIdentificadorExterno(CanalOrigen canal, String identificadorExterno);

    /** RN-01: reservas activas del apartamento que comparten al menos una noche con la estancia. */
    List<Reserva> listarActivasQueSeSolapan(IdentificacionApartamento apartamento, Estancia estancia);

    /** Reservas activas del apartamento (para bloqueos, 7.3, y tiempo de preparación, RN-20). */
    List<Reserva> listarActivasPorApartamento(IdentificacionApartamento apartamento);

    /** 7.3: reservas activas o con entrada desde la fecha dada, para retirar un apartamento de la venta. */
    List<Reserva> listarActivasOFuturasPorApartamento(IdentificacionApartamento apartamento, LocalDate desde);

    /** RN-21: candidatas al vencimiento automático. */
    List<Reserva> listarPorEstado(EstadoReserva estado);

    /** 7.6: llegadas previstas del día (reservas CONFIRMADA con esa fecha de entrada). */
    List<Reserva> listarLlegadasPrevistas(LocalDate fecha);

    /** 7.6: salidas previstas del día (reservas EN_CURSO con esa fecha de salida). */
    List<Reserva> listarSalidasPrevistas(LocalDate fecha);

    /** 7.5: todas las reservas, de la más reciente a la más antigua. */
    List<Reserva> listarTodas();
}
