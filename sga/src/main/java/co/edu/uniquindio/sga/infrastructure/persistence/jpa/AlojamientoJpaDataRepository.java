package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AlojamientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data para la tabla alojamiento. */
public interface AlojamientoJpaDataRepository extends JpaRepository<AlojamientoEntity, Long> {

    Optional<AlojamientoEntity> findByAlojamientoId(String alojamientoId);

    /** F-13: hay un único alojamiento; si hubiera más, se toma el primero que se guardó. */
    Optional<AlojamientoEntity> findFirstByOrderByIdAsc();
}
