package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla bloqueo. La usan BloqueoJpaRepository y ApartamentoJpaRepository. */
public interface BloqueoJpaDataRepository extends JpaRepository<BloqueoEntity, Long> {

    Optional<BloqueoEntity> findByBloqueoId(String bloqueoId);

    List<BloqueoEntity> findByApartamentoIdOrderByIdAsc(String apartamentoId);

    List<BloqueoEntity> findByApartamentoIdAndVigenteTrueOrderByRangoFechaInicioAsc(String apartamentoId);
}
