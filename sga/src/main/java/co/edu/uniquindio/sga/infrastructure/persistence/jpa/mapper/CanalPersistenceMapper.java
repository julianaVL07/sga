package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ConflictoCanalEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EventoCanalEntity;
import org.springframework.stereotype.Component;

/** Traduce a mano EventoCanal y ConflictoCanal con sus filas (Guía 09, 6.6). Al leer usa las fábricas reconstruir. */
@Component
public class CanalPersistenceMapper {

    // ==================== EVENTO DE CANAL ====================

    public EventoCanalEntity aEntidad(EventoCanal ev) {
        EventoCanalEntity e = new EventoCanalEntity();
        e.setEventoId(ev.getId());
        e.setTipoEvento(ev.getTipoEvento());
        e.setCanalOrigen(ev.getCanalOrigen());
        e.setIdentificadorExterno(ev.getIdentificadorExterno());
        e.setResultado(ev.getResultado());
        e.setFecha(ev.getFecha());
        return e;
    }

    public EventoCanal aDominio(EventoCanalEntity e) {
        return EventoCanal.reconstruir(e.getEventoId(), e.getTipoEvento(), e.getCanalOrigen(),
                e.getIdentificadorExterno(), e.getResultado(), e.getFecha());
    }

    // ==================== CONFLICTO DE CANAL ====================

    public ConflictoCanalEntity aEntidad(ConflictoCanal c) {
        ConflictoCanalEntity e = new ConflictoCanalEntity();
        e.setConflictoId(c.getId());
        e.setCanalOrigen(c.getCanalOrigen());
        e.setIdentificadorExterno(c.getIdentificadorExterno());
        e.setDetalle(c.getDetalle());
        e.setFecha(c.getFecha());
        e.setRevisado(c.isRevisado());
        e.setNotasAdministrador(c.getNotasAdministrador());
        e.setFechaRevision(c.getFechaRevision());
        return e;
    }

    public ConflictoCanal aDominio(ConflictoCanalEntity e) {
        return ConflictoCanal.reconstruir(e.getConflictoId(), e.getCanalOrigen(), e.getIdentificadorExterno(),
                e.getDetalle(), e.getFecha(), e.isRevisado(), e.getNotasAdministrador(), e.getFechaRevision());
    }
}