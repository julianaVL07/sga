package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de folios. Un folio pertenece a una sola reserva (7.7). */
public interface FolioRepository {

    Folio guardar(Folio folio);

    Optional<Folio> buscarPorId(String id);

    Optional<Folio> buscarPorReserva(CodigoReserva codigoReserva);

    /**
     * 7.9: folios con al menos un pago cuya fecha está entre desde y hasta, ambas incluidas. Cada folio trae todos
     * sus pagos; el reporte de ingresos filtra los del periodo.
     */
    List<Folio> listarConPagosEntre(LocalDate desde, LocalDate hasta);
}
