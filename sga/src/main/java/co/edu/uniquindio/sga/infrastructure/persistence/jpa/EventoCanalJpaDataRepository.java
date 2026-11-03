package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EventoCanalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data para la tabla evento_canal. */
public interface EventoCanalJpaDataRepository extends JpaRepository<EventoCanalEntity, Long> {

    boolean existsByEventoId(String eventoId);

    List<EventoCanalEntity> findAllByOrderByFechaDescIdDesc();

    List<EventoCanalEntity> findByCanalOrigenAndIdentificadorExternoOrderByFechaDescIdDesc(CanalOrigen canalOrigen,
                                                                                           String identificadorExterno);
}
