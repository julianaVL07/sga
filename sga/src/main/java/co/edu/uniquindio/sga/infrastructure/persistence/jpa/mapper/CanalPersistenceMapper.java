package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CanalVentaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ConflictoCanalEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EventoCanalEntity;
import org.springframework.stereotype.Component;

/**
 * Traduce a mano CanalVenta, EventoCanal y ConflictoCanal con sus filas (Guía 09, 6.6). Al leer usa las fábricas
 * reconstruir.
 */
@Component
public class CanalPersistenceMapper {

    // ==================== CANAL DE VENTA ====================

    public CanalVentaEntity aEntidad(CanalVenta c) {
        CanalVentaEntity e = new CanalVentaEntity();
        e.setNombre(c.getNombre());
        e.setTipo(c.getTipo());
        e.setHashCredencial(c.getHashCredencial());
        e.setActivo(c.isActivo());
        e.setFechaCreacion(c.getFechaCreacion());
        return e;
    }

    public CanalVenta aDominio(CanalVentaEntity e) {
        return CanalVenta.reconstruir(e.getNombre(), e.getTipo(), e.getHashCredencial(), e.isActivo(),
                e.getFechaCreacion());
    }

    // ==================== EVENTO DE CANAL ====================

    public EventoCanalEntity aEntidad(EventoCanal ev) {
        EventoCanalEntity e = new EventoCanalEntity();
        e.setEventoId(ev.getId());
        e.setTipoEvento(ev.getTipoEvento());
        e.setCanalOrigen(ev.getCanalOrigen());
        e.setNombreCanal(ev.getNombreCanal());
        e.setIdentificadorExterno(ev.getIdentificadorExterno());
        e.setResultado(ev.getResultado());
        e.setFecha(ev.getFecha());
        return e;
    }

    public EventoCanal aDominio(EventoCanalEntity e) {
        return EventoCanal.reconstruir(e.getEventoId(), e.getTipoEvento(), e.getCanalOrigen(), e.getNombreCanal(),
                e.getIdentificadorExterno(), e.getResultado(), e.getFecha());
    }

    // ==================== CONFLICTO DE CANAL ====================

    public ConflictoCanalEntity aEntidad(ConflictoCanal c) {
        ConflictoCanalEntity e = new ConflictoCanalEntity();
        e.setConflictoId(c.getId());
        e.setCanalOrigen(c.getCanalOrigen());
        e.setNombreCanal(c.getNombreCanal());
        e.setIdentificadorExterno(c.getIdentificadorExterno());
        e.setDetalle(c.getDetalle());
        e.setFecha(c.getFecha());
        e.setResuelto(c.isResuelto());
        e.setMotivoResolucion(c.getMotivoResolucion());
        e.setResueltoPor(c.getResueltoPor());
        e.setFechaResolucion(c.getFechaResolucion());
        return e;
    }

    public ConflictoCanal aDominio(ConflictoCanalEntity e) {
        return ConflictoCanal.reconstruir(e.getConflictoId(), e.getCanalOrigen(), e.getNombreCanal(),
                e.getIdentificadorExterno(), e.getDetalle(), e.getFecha(), e.isResuelto(), e.getMotivoResolucion(),
                e.getResueltoPor(), e.getFechaResolucion());
    }
}
