package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de bloqueos. Un bloqueo levantado se conserva (eliminación lógica).
 */
public interface BloqueoRepository {

    Bloqueo guardar(Bloqueo bloqueo);

    Optional<Bloqueo> buscarPorId(String id);

    List<Bloqueo> listarVigentesPorApartamento(IdentificacionApartamento apartamento);

    /** RN-07: bloqueos vigentes del apartamento que afectan alguna noche de la estancia. */
    List<Bloqueo> listarVigentesQueSeSolapan(IdentificacionApartamento apartamento, Estancia estancia);
}
