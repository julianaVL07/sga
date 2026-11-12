package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.response.CanalVentaResponse;
import co.edu.uniquindio.sga.application.dto.response.ConflictoCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.CredencialCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoBitacoraResponse;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.service.CanalVentaService.CredencialEmitida;
import org.springframework.stereotype.Component;

/** Convierte los canales de venta, sus conflictos y la bitácora en las respuestas de la API (10.3.1, 7.8). */
@Component
public class CanalVentaMapper {

    /** Sin credencial ni hash: es lo que muestra el listado. */
    public CanalVentaResponse aResponse(CanalVenta canal) {
        return new CanalVentaResponse(canal.getNombre(), canal.getTipo(), canal.isActivo(), canal.getFechaCreacion());
    }

    /** La única respuesta que lleva la credencial en claro. */
    public CredencialCanalResponse aResponse(CredencialEmitida emitida) {
        CanalVenta canal = emitida.canal();
        return new CredencialCanalResponse(canal.getNombre(), canal.getTipo(), canal.isActivo(),
                canal.getFechaCreacion(), emitida.credencial());
    }

    public ConflictoCanalResponse aResponse(ConflictoCanal conflicto) {
        return new ConflictoCanalResponse(conflicto.getId(), conflicto.getNombreCanal(),
                conflicto.getIdentificadorExterno(), conflicto.getDetalle(), conflicto.getFecha(), conflicto.estado(),
                conflicto.getMotivoResolucion(), conflicto.getResueltoPor(), conflicto.getFechaResolucion());
    }

    public EventoBitacoraResponse aResponse(EventoCanal evento) {
        return new EventoBitacoraResponse(evento.getId(), evento.getFecha(), evento.getNombreCanal(),
                evento.getTipoEvento(), evento.getIdentificadorExterno(), evento.getResultado());
    }
}
