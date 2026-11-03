package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.NovedadEntity;
import org.springframework.stereotype.Component;

/** Traduce a mano Novedad y NovedadEntity, al leer usa Novedad.reconstruir. */
@Component
public class NovedadPersistenceMapper {

    public NovedadEntity aEntidad(Novedad n) {
        NovedadEntity e = new NovedadEntity();
        e.setNovedadId(n.getId());
        e.setApartamentoId(n.getApartamento().codigo());
        e.setFecha(n.getFecha());
        e.setAutor(n.getAutor());
        e.setDescripcion(n.getDescripcion());
        e.setGravedad(n.getGravedad());
        e.setAtendida(n.isAtendida());
        e.setFechaAtencion(n.getFechaAtencion());
        return e;
    }

    public Novedad aDominio(NovedadEntity e) {
        return Novedad.reconstruir(e.getNovedadId(), new IdentificacionApartamento(e.getApartamentoId()),
                e.getFecha(), e.getAutor(), e.getDescripcion(), e.getGravedad(), e.isAtendida(), e.getFechaAtencion());
    }
}