package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.RecordatorioEnviadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio Spring Data de los recordatorios enviados (7.8). */
public interface RecordatorioEnviadoJpaDataRepository extends JpaRepository<RecordatorioEnviadoEntity, Long> {

    Optional<RecordatorioEnviadoEntity> findByCodigoReservaAndMotivo(String codigoReserva, MotivoRecordatorio motivo);
}
