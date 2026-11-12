package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla folio. */
public interface FolioJpaDataRepository extends JpaRepository<FolioEntity, Long> {

    Optional<FolioEntity> findByFolioId(String folioId);

    Optional<FolioEntity> findByCodigoReserva(String codigoReserva);

    /** 7.9: folios con al menos un pago en el periodo, ambas fechas incluidas. */
    @Query("select distinct f from FolioEntity f join f.pagos p where p.fecha between :desde and :hasta")
    List<FolioEntity> buscarConPagosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}