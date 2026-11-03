package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Versionado de la política de cancelación (CU-30, F-07, 7.4): el alojamiento tiene una sola política vigente; al
 * actualizarla se crea la versión siguiente y la anterior se conserva, porque cada reserva queda con la versión
 * vigente al crearse (RN-22). L-10 (al menos dos tramos y uno desde 0 días) lo valida PoliticaCancelacion.
 */
public class PoliticaCancelacionService {

    private final PoliticaCancelacionRepository politicaRepository;

    public PoliticaCancelacionService(PoliticaCancelacionRepository politicaRepository) {
        this.politicaRepository = Objects.requireNonNull(politicaRepository);
    }

    /** RN-22: la versión que se congela en cada reserva nueva. */
    public PoliticaCancelacion vigente() {
        return politicaRepository.obtenerVigente()
                .orElseThrow(() -> new ReglaDominioException("F-07: El alojamiento no tiene una política de "
                        + "cancelación vigente"));
    }

    /**
     * Carga inicial: si todavía no hay ninguna versión, guarda la dada (la versión 1 configurada). Si ya hay una,
     * no hace nada y devuelve la vigente, para no pisar las versiones creadas después.
     */
    public PoliticaCancelacion registrarInicialSiNoExiste(PoliticaCancelacion inicial) {
        if (inicial == null) {
            throw new ReglaDominioException("F-07: La política inicial es obligatoria");
        }
        return politicaRepository.obtenerVigente().orElseGet(() -> politicaRepository.guardar(inicial));
    }

    /**
     * CU-30 (7.4): crea la versión N+1 con los tramos y la consecuencia del no-show dados, vigente desde hoy.
     * La versión N se conserva sin cambios; las reservas existentes siguen con la versión que congelaron.
     */
    public PoliticaCancelacion actualizar(String consecuenciaNoShow, List<TramoCancelacion> tramos, LocalDate hoy) {
        PoliticaCancelacion nueva = vigente().nuevaVersion(consecuenciaNoShow, tramos, hoy);
        return politicaRepository.guardar(nueva);
    }
}
