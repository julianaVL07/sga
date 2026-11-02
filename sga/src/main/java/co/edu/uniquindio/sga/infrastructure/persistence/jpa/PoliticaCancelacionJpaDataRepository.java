package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PoliticaCancelacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data para la tabla politica_cancelacion. */
public interface PoliticaCancelacionJpaDataRepository extends JpaRepository<PoliticaCancelacionEntity, Long> {

    /** 7.4: la vigente es la versión de mayor número. */
    Optional<PoliticaCancelacionEntity> findFirstByOrderByVersionDesc();

    boolean existsByVersion(int version);

    Page<PoliticaCancelacionEntity> findAllByOrderByVersionDesc(Pageable pageable);
}
