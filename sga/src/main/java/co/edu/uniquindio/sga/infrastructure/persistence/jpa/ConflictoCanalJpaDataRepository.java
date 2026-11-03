package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ConflictoCanalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla conflicto_canal. */
public interface ConflictoCanalJpaDataRepository extends JpaRepository<ConflictoCanalEntity, Long> {

    Optional<ConflictoCanalEntity> findByConflictoId(String conflictoId);

    List<ConflictoCanalEntity> findAllByOrderByFechaDescIdDesc();
}