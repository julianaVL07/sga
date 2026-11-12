package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.port.SugeridorGravedad;
import co.edu.uniquindio.sga.infrastructure.ia.SugeridorGravedadDeshabilitado;
import co.edu.uniquindio.sga.infrastructure.ia.SugeridorGravedadGemini;
import co.edu.uniquindio.sga.infrastructure.ia.SugeridorGravedadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Locale;

/**
 * Único lugar donde se elige el adaptador del puerto SugeridorGravedad (10.2), según sga.ia.proveedor:
 * gemini (API de Google AI Studio), local (reglas por palabras clave, el de por defecto y el de las pruebas) o
 * deshabilitado (nunca sugiere). Un valor desconocido se registra en el log y deja el adaptador local.
 */
@Configuration
public class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    @Bean
    public SugeridorGravedad sugeridorGravedad(
            @Value("${sga.ia.proveedor:local}") String proveedor,
            @Value("${sga.ia.gemini.url}") String url,
            @Value("${sga.ia.gemini.modelo}") String modelo,
            @Value("${sga.ia.gemini.clave:}") String clave,
            @Value("${sga.ia.gemini.tiempo-espera}") Duration tiempoEspera,
            ObjectProvider<JsonMapper> jsonMapper) {
        String elegido = proveedor == null ? "" : proveedor.trim().toLowerCase(Locale.ROOT);
        SugeridorGravedad sugeridor = switch (elegido) {
            case "gemini" -> new SugeridorGravedadGemini(
                    jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()), url, modelo, clave, tiempoEspera);
            case "deshabilitado" -> new SugeridorGravedadDeshabilitado();
            case "local" -> new SugeridorGravedadLocal();
            default -> {
                log.warn("IA: proveedor desconocido '{}' en sga.ia.proveedor; se usa el local", proveedor);
                yield new SugeridorGravedadLocal();
            }
        };
        log.info("IA: sugerencia de gravedad de novedades con {}", sugeridor.getClass().getSimpleName());
        return sugeridor;
    }
}
