package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.repository.RecordatorioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import co.edu.uniquindio.sga.domain.valueobject.RecordatorioEnviado;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador en memoria del puerto RecordatorioRepository, para las pruebas (7.8). */
public class RecordatorioRepositoryInMemory implements RecordatorioRepository {

    private record Llave(CodigoReserva codigo, MotivoRecordatorio motivo) {
    }

    private final Map<Llave, RecordatorioEnviado> almacen = new ConcurrentHashMap<>();

    @Override
    public Optional<RecordatorioEnviado> buscarUltimo(CodigoReserva codigo, MotivoRecordatorio motivo) {
        return Optional.ofNullable(almacen.get(new Llave(codigo, motivo)));
    }

    @Override
    public RecordatorioEnviado guardar(RecordatorioEnviado recordatorio) {
        if (recordatorio == null) {
            throw new IllegalArgumentException("El recordatorio a guardar es obligatorio");
        }
        almacen.put(new Llave(recordatorio.codigo(), recordatorio.motivo()), recordatorio);
        return recordatorio;
    }
}
