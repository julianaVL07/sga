package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria (I-02). Usa dos mapas: por id del folio y por código de reserva,
 * porque un folio pertenece a una sola reserva (7.7).
 */
public class FolioRepositoryInMemory implements FolioRepository {

    private final Map<String, Folio> porId = new ConcurrentHashMap<>();
    private final Map<CodigoReserva, String> idPorReserva = new ConcurrentHashMap<>();

    @Override
    public synchronized Folio guardar(Folio folio) {
        if (folio == null) {
            throw new IllegalArgumentException("El folio a guardar es obligatorio");
        }
        String idExistente = idPorReserva.get(folio.getReserva());
        if (idExistente != null && !idExistente.equals(folio.getId())) {
            throw new ReglaDominioException("7.7: la reserva " + folio.getReserva().valor() + " ya tiene un folio");
        }
        porId.put(folio.getId(), folio);
        idPorReserva.put(folio.getReserva(), folio.getId());
        return folio;
    }

    @Override
    public Optional<Folio> buscarPorId(String id) {
        return Optional.ofNullable(id).map(porId::get);
    }

    @Override
    public Optional<Folio> buscarPorReserva(CodigoReserva codigoReserva) {
        return Optional.ofNullable(codigoReserva).map(idPorReserva::get).map(porId::get);
    }
}
