package co.edu.uniquindio.sga;

import co.edu.uniquindio.sga.infrastructure.config.setup.DefaultUserProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // CU-14 y 7.8: activa VencimientoReservasScheduler y RecordatoriosScheduler
@EnableConfigurationProperties(DefaultUserProperties.class)   // Usuarios iniciales (Guía 12)
public class SgaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SgaApplication.class, args);
	}

}