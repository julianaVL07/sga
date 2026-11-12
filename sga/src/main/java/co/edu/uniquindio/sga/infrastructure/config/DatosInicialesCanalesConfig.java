package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.port.CredencialesCanal;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.service.CanalVentaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Datos de prueba de los canales de venta (12.3, 10.3.1): el canal externo "booking", el mismo que envió la
 * reserva externa de los datos iniciales (BK-DEMO-000123). No hace nada si el canal ya existe.
 *
 * Su credencial sale de sga.canales.credencial-demo, que va solo en application-secrets.properties. Si falta, se
 * genera una al azar al arrancar y se escribe en el log, para poder usar el simulador del canal
 * (docs/api/canal-externo.http) sin configurar nada.
 */
@Configuration
public class DatosInicialesCanalesConfig {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesCanalesConfig.class);

    /** Canal de la reserva externa de DatosInicialesReservasConfig. */
    static final String CANAL_DEMO = "booking";

    @Bean
    @Order(3)
    public CommandLineRunner cargarCanalesDeVenta(CanalVentaService canalVentaService,
                                                  CanalVentaRepository canalVentaRepository,
                                                  CredencialesCanal credenciales,
                                                  @Value("${sga.canales.credencial-demo:}") String credencialDemo,
                                                  Clock reloj) {
        return args -> {
            if (canalVentaRepository.existePorNombre(CANAL_DEMO)) {
                return;
            }
            boolean configurada = credencialDemo != null && !credencialDemo.isBlank();
            String credencial = configurada ? credencialDemo.trim() : credenciales.generar();
            canalVentaService.registrar(CANAL_DEMO, credencial, LocalDateTime.now(reloj));
            if (configurada) {
                log.info("Canal externo '{}' creado con la credencial de sga.canales.credencial-demo", CANAL_DEMO);
            } else {
                log.warn("Falta sga.canales.credencial-demo en application-secrets.properties. Credencial generada "
                        + "para el canal externo '{}' (encabezado X-Canal-Credencial): {}", CANAL_DEMO, credencial);
            }
        };
    }
}
