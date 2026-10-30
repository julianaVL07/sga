package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data genera la implementación en ejecución (Guía 09, 6.4): por eso es una interfaz.
 * Los nombres siguen la convención findBy...; "EstanciaFechaEntrada" entra al @Embedded estancia.
 */
public interface ReservaJpaDataRepository extends JpaRepository<ReservaEntity, Long> {

    Optional<ReservaEntity> findByCodigo(String codigo);

    Optional<ReservaEntity> findByCanalOrigenAndIdentificadorCanalExterno(CanalOrigen canal, String identificadorExterno);

    /** RN-01: comparten al menos una noche (3.1). La noche de salida no se ocupa. */
    @Query("""
            select r from ReservaEntity r
            where r.apartamentoId = :apartamentoId
              and r.estado in :estados
              and r.estancia.fechaEntrada < :fechaSalida
              and r.estancia.fechaSalida > :fechaEntrada
            """)
    List<ReservaEntity> buscarQueSeSolapan(@Param("apartamentoId") String apartamentoId,
                                           @Param("estados") Collection<EstadoReserva> estados,
                                           @Param("fechaEntrada") LocalDate fechaEntrada,
                                           @Param("fechaSalida") LocalDate fechaSalida);

    List<ReservaEntity> findByApartamentoIdAndEstadoInOrderByEstanciaFechaEntradaAsc(
            String apartamentoId, Collection<EstadoReserva> estados);

    List<ReservaEntity> findByApartamentoIdAndEstadoInAndEstanciaFechaSalidaGreaterThanEqual(
            String apartamentoId, Collection<EstadoReserva> estados, LocalDate desde);

    List<ReservaEntity> findByEstadoOrderByFechaCreacionDesc(EstadoReserva estado);

    List<ReservaEntity> findByEstadoAndEstanciaFechaEntrada(EstadoReserva estado, LocalDate fecha);

    List<ReservaEntity> findByEstadoAndEstanciaFechaSalida(EstadoReserva estado, LocalDate fecha);

    List<ReservaEntity> findAllByOrderByFechaCreacionDesc();

    /**
     * CU-19 (7.5, 12.4). Cada criterio es opcional: si el parámetro llega en null, "(:x is null or ...)" lo anula.
     * desde y hasta acotan la fecha de entrada de la estancia (ambas incluidas). El orden y el tamaño de la
     * página llegan en el Pageable; Spring Data deriva solo la consulta count para el total de elementos.
     */
    @Query("""
            select r from ReservaEntity r
            where (:estado is null or r.estado = :estado)
              and (:apartamentoId is null or r.apartamentoId = :apartamentoId)
              and (:canal is null or r.canalOrigen = :canal)
              and (:documentoTitular is null or r.titular.numeroDocumento = :documentoTitular)
              and (:desde is null or r.estancia.fechaEntrada >= :desde)
              and (:hasta is null or r.estancia.fechaEntrada <= :hasta)
            """)
    Page<ReservaEntity> buscarConFiltro(@Param("estado") EstadoReserva estado,
                                        @Param("apartamentoId") String apartamentoId,
                                        @Param("canal") CanalOrigen canal,
                                        @Param("documentoTitular") String documentoTitular,
                                        @Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta,
                                        Pageable pageable);
}