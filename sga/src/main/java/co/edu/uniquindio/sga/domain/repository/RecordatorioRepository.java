package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import co.edu.uniquindio.sga.domain.valueobject.RecordatorioEnviado;

import java.util.Optional;

/** Puerto de persistencia del último recordatorio enviado por reserva y motivo (7.8). */
public interface RecordatorioRepository {

    Optional<RecordatorioEnviado> buscarUltimo(CodigoReserva codigo, MotivoRecordatorio motivo);

    /** Guarda el envío; si ya había uno para la misma reserva y motivo, lo reemplaza. */
    RecordatorioEnviado guardar(RecordatorioEnviado recordatorio);
}
