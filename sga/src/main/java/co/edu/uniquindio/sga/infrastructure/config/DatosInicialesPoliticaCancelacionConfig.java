package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.service.PoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Versión inicial de la política de cancelación (CU-30, Anexo A, A.4). Al arrancar, si el repositorio no tiene
 * ninguna versión, guarda la que está en sga.politica-cancelacion.* (L-13: configuración, nunca constante). Las
 * versiones siguientes solo se crean con PUT /api/politica-cancelacion; las propiedades no se cambian por API.
 * Corre con @Order(0), antes que los datos de reservas, que congelan la versión vigente (RN-22).
 */
@Configuration
public class DatosInicialesPoliticaCancelacionConfig {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesPoliticaCancelacionConfig.class);

    /** Si los valores configurados violan L-10, el arranque falla con ReglaDominioException. */
    @Bean
    @Order(0)
    public CommandLineRunner cargarPoliticaCancelacion(PoliticaCancelacionService politicaCancelacionService,
                                                       PoliticaCancelacionProperties propiedades) {
        return args -> {
            PoliticaCancelacion vigente = politicaCancelacionService.registrarInicialSiNoExiste(propiedades.aPolitica());
            log.info("Política de cancelación vigente: versión {} desde {}", vigente.version(), vigente.fechaVigencia());
        };
    }
}
