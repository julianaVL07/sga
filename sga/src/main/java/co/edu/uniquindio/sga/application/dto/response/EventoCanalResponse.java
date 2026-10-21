package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

/**
 * Respuesta de CU-08 al canal externo para una creación o una cancelación: qué pasó con su mensaje (7.8).
 * ACEPTADO, RECHAZADO_CONFLICTO (RN-18), RECHAZADO_INVALIDO o DUPLICADO_IGNORADO (RN-19).
 * La consulta de disponibilidad del canal responde con la lista de ApartamentoResponse.
 */
public record EventoCanalResponse(
        String identificadorExterno,
        TipoEventoCanal tipoEvento,
        ResultadoEventoCanal resultado
) {
}