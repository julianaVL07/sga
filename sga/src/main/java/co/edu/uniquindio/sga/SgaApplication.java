package co.edu.uniquindio.sga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // CU-14: activa la tarea programada VencimientoReservasScheduler
public class SgaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SgaApplication.class, args);
	}

}