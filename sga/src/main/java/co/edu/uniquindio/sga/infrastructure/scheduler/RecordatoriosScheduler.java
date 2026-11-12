package co.edu.uniquindio.sga.infrastructure.scheduler;

import co.edu.uniquindio.sga.application.usecase.EnviarRecordatoriosUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de los recordatorios automáticos (7.8): no hay endpoint, lo dispara el reloj una vez al día
 * (sga.recordatorios.cron, en la zona horaria del alojamiento). Se enciende y se apaga con sga.recordatorios.activo;
 * apagado, este bean ni siquiera se crea. Requiere @EnableScheduling en SgaApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sga.recordatorios.activo", havingValue = "true")
public class RecordatoriosScheduler {

    private final EnviarRecordatoriosUseCase enviarRecordatoriosUseCase;

    @Scheduled(cron = "${sga.recordatorios.cron}", zone = "${sga.alojamiento.zona-horaria}")
    public void enviarRecordatorios() {
        EnviarRecordatoriosUseCase.Resultado resultado = enviarRecordatoriosUseCase.ejecutar();
        log.info("Recordatorios: {} enviado(s), {} fallido(s)", resultado.enviados(), resultado.fallidos());
    }
}
