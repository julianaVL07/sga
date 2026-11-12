package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.port.NotificadorTitular;
import co.edu.uniquindio.sga.domain.repository.RecordatorioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import co.edu.uniquindio.sga.domain.valueobject.RecordatorioEnviado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Recordatorios automáticos del día anterior (7.8): avisa a los titulares de las reservas CONFIRMADAS que llegan
 * mañana y de las EN_CURSO que salen mañana. Lo dispara una tarea programada, sin intervención manual.
 * Un mismo recordatorio no se envía dos veces: se registra el último envío por reserva y motivo, con la fecha que
 * se recordó. Si un envío falla se registra en el log, no se marca como enviado y se sigue con las demás.
 * No abre una transacción: cada lectura y cada registro usan la de su repositorio, y el correo no la alarga.
 */
@Service
public class EnviarRecordatoriosUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnviarRecordatoriosUseCase.class);

    /** Cuántos recordatorios se enviaron y cuántos fallaron en una ejecución. */
    public record Resultado(int enviados, int fallidos) {
    }

    private final ReservaRepository reservas;
    private final RecordatorioRepository recordatorios;
    private final NotificadorTitular notificador;
    private final Clock reloj;

    public EnviarRecordatoriosUseCase(ReservaRepository reservas, RecordatorioRepository recordatorios,
                                      NotificadorTitular notificador, Clock reloj) {
        this.reservas = Objects.requireNonNull(reservas);
        this.recordatorios = Objects.requireNonNull(recordatorios);
        this.notificador = Objects.requireNonNull(notificador);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Resultado ejecutar() {
        LocalDate manana = LocalDate.now(reloj).plusDays(1);
        Resultado llegadas = enviar(reservas.listarLlegadasPrevistas(manana), MotivoRecordatorio.LLEGADA, manana);
        Resultado salidas = enviar(reservas.listarSalidasPrevistas(manana), MotivoRecordatorio.SALIDA, manana);
        return new Resultado(llegadas.enviados() + salidas.enviados(), llegadas.fallidos() + salidas.fallidos());
    }

    private Resultado enviar(List<Reserva> candidatas, MotivoRecordatorio motivo, LocalDate fecha) {
        int enviados = 0;
        int fallidos = 0;
        for (Reserva reserva : candidatas) {
            boolean yaEnviado = recordatorios.buscarUltimo(reserva.getCodigo(), motivo)
                    .map(ultimo -> ultimo.cubre(fecha))
                    .orElse(false);
            if (yaEnviado) {
                continue;
            }
            try {
                notificador.recordatorio(reserva, motivo);
                recordatorios.guardar(new RecordatorioEnviado(reserva.getCodigo(), motivo, fecha,
                        LocalDateTime.now(reloj)));
                enviados++;
            } catch (RuntimeException e) {
                fallidos++;
                log.warn("Notificaciones: no se pudo enviar el recordatorio de {} de la reserva {} ({}): {}",
                        motivo, reserva.getCodigo().valor(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return new Resultado(enviados, fallidos);
    }
}
