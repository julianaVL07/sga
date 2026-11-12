package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CanalVentaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data para la tabla canal_venta. */
public interface CanalVentaJpaDataRepository extends JpaRepository<CanalVentaEntity, Long> {

    Optional<CanalVentaEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    /** El orden llega en el Pageable. */
    Page<CanalVentaEntity> findAllBy(Pageable pageable);
}
