package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Implementación en memoria (I-02), indexada por el número de versión. Solo agrega versiones. */
public class PoliticaCancelacionRepositoryInMemory implements PoliticaCancelacionRepository {

    private final Map<Integer, PoliticaCancelacion> almacen = new ConcurrentHashMap<>();

    @Override
    public synchronized PoliticaCancelacion guardar(PoliticaCancelacion politica) {
        if (politica == null) {
            throw new IllegalArgumentException("La política a guardar es obligatoria");
        }
        if (almacen.containsKey(politica.version())) {
            throw new ReglaDominioException("F-07: La versión " + politica.version()
                    + " de la política de cancelación ya existe y no se modifica");
        }
        almacen.put(politica.version(), politica);
        return politica;
    }

    @Override
    public Optional<PoliticaCancelacion> obtenerVigente() {
        return almacen.values().stream().max(Comparator.comparingInt(PoliticaCancelacion::version));
    }

    @Override
    public Pagina<PoliticaCancelacion> listarVersiones(int pagina, int tamano) {
        return Pagina.de(almacen.values().stream()
                .sorted(Comparator.comparingInt(PoliticaCancelacion::version).reversed())
                .toList(), pagina, tamano);
    }
}
