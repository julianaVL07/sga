package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TemporadaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla temporada. */
public interface TemporadaJpaDataRepository extends JpaRepository<TemporadaEntity, Long> {

    Optional<TemporadaEntity> findByTemporadaId(String temporadaId);

    Optional<TemporadaEntity> findFirstByTipo(TipoTemporada tipo);

    List<TemporadaEntity> findAllByOrderByIdAsc();

    List<TemporadaEntity> findAllByOrderByNombreAsc();
}
