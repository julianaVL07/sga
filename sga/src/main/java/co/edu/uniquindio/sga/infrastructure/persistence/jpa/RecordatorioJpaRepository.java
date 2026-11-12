package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.RecordatorioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import co.edu.uniquindio.sga.domain.valueobject.RecordatorioEnviado;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.RecordatorioEnviadoEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto RecordatorioRepository (7.8): una fila por reserva y motivo, que se actualiza. */
@Transactional
public class RecordatorioJpaRepository implements RecordatorioRepository {

    private final RecordatorioEnviadoJpaDataRepository dataRepository;

    public RecordatorioJpaRepository(RecordatorioEnviadoJpaDataRepository dataRepository) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecordatorioEnviado> buscarUltimo(CodigoReserva codigo, MotivoRecordatorio motivo) {
        if (codigo == null || motivo == null) {
            return Optional.empty();
        }
        return dataRepository.findByCodigoReservaAndMotivo(codigo.valor(), motivo).map(this::aDominio);
    }

    @Override
    public RecordatorioEnviado guardar(RecordatorioEnviado recordatorio) {
        if (recordatorio == null) {
            throw new IllegalArgumentException("El recordatorio a guardar es obligatorio");
        }
        RecordatorioEnviadoEntity fila = dataRepository
                .findByCodigoReservaAndMotivo(recordatorio.codigo().valor(), recordatorio.motivo())
                .orElseGet(RecordatorioEnviadoEntity::new);
        fila.setCodigoReserva(recordatorio.codigo().valor());
        fila.setMotivo(recordatorio.motivo());
        fila.setFechaEvento(recordatorio.fechaEvento());
        fila.setEnviadoEn(recordatorio.enviadoEn());
        return aDominio(dataRepository.save(fila));
    }

    private RecordatorioEnviado aDominio(RecordatorioEnviadoEntity fila) {
        return new RecordatorioEnviado(new CodigoReserva(fila.getCodigoReserva()), fila.getMotivo(),
                fila.getFechaEvento(), fila.getEnviadoEn());
    }
}
