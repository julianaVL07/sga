package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.NovedadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla novedad. */
public interface NovedadJpaDataRepository extends JpaRepository<NovedadEntity, Long> {

    Optional<NovedadEntity> findByNovedadId(String novedadId);

    List<NovedadEntity> findByApartamentoIdOrderByFechaDescIdDesc(String apartamentoId);

    List<NovedadEntity> findByAtendidaFalseOrderByFechaDescIdDesc();
}
