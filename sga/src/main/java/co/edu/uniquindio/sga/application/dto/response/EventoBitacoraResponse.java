package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import java.time.LocalDateTime;

/**
 * Entrada de la bitácora de eventos con los canales (7.8), para el administrador. identificadorExterno es null en
 * las consultas de disponibilidad y en los intentos rechazados antes de leer el mensaje.
 */
public record EventoBitacoraResponse(
        String id,
        LocalDateTime fecha,
        String canal,
        TipoEventoCanal tipoEvento,
        String identificadorExterno,
        ResultadoEventoCanal resultado
) {
}
