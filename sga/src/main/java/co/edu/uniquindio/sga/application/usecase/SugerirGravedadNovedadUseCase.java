package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.port.SugeridorGravedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * POST /api/apartamentos/novedades/sugerir-gravedad
 * Pide al modelo de IA una sugerencia de gravedad para la descripción de una novedad (10.2, L-21). No guarda nada:
 * la gravedad final la elige la persona al registrar la novedad (CU-17, 7.6). Si el modelo falla, la respuesta
 * es vacía y el registro de la novedad sigue funcionando igual.
 * Actores: personal de servicio y administrador.
 */
@Service
public class SugerirGravedadNovedadUseCase {

    private static final Logger log = LoggerFactory.getLogger(SugerirGravedadNovedadUseCase.class);

    private final SugeridorGravedad sugeridor;

    public SugerirGravedadNovedadUseCase(SugeridorGravedad sugeridor) {
        this.sugeridor = Objects.requireNonNull(sugeridor);
    }

    public Optional<SugerenciaGravedad> ejecutar(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return Optional.empty();
        }
        try {
            return sugeridor.sugerir(descripcion);
        } catch (RuntimeException e) {
            // El puerto no debería lanzar; si un adaptador lo hace, la operación sigue sin sugerencia
            log.warn("IA: el sugeridor de gravedad falló: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
