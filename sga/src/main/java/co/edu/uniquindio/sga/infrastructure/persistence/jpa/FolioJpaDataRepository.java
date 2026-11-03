package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data para la tabla folio. */
public interface FolioJpaDataRepository extends JpaRepository<FolioEntity, Long> {

    Optional<FolioEntity> findByFolioId(String folioId);

    Optional<FolioEntity> findByCodigoReserva(String codigoReserva);
}