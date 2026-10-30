package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla apartamento. */
public interface ApartamentoJpaDataRepository extends JpaRepository<ApartamentoEntity, Long> {

    Optional<ApartamentoEntity> findByIdentificacion(String identificacion);

    boolean existsByIdentificacion(String identificacion);

    List<ApartamentoEntity> findByEliminadoFalseOrderByIdentificacionAsc();

    /** CU-20: página de apartamentos no eliminados; el orden llega en el Pageable. */
    Page<ApartamentoEntity> findByEliminadoFalse(Pageable pageable);
}
