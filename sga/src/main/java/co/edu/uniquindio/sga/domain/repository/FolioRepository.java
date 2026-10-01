package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.util.Optional;

/** Puerto de persistencia de folios. Un folio pertenece a una sola reserva (7.7). */
public interface FolioRepository {

    Folio guardar(Folio folio);

    Optional<Folio> buscarPorId(String id);

    Optional<Folio> buscarPorReserva(CodigoReserva codigoReserva);
}
