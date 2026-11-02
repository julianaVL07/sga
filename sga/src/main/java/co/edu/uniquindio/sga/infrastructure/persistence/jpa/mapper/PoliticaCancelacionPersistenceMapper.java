package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PoliticaCancelacionEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TramoCancelacionEmbedable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Traduce a mano cada versión de la política de cancelación (Guía 09, 6.6). PoliticaCancelacion es un record que
 * valida L-10 en su constructor, así que al leer se vuelve a validar lo que se guardó válido.
 */
@Component
public class PoliticaCancelacionPersistenceMapper {

    public PoliticaCancelacionEntity aEntidad(PoliticaCancelacion p) {
        PoliticaCancelacionEntity e = new PoliticaCancelacionEntity();
        e.setVersion(p.version());
        e.setFechaVigencia(p.fechaVigencia());
        e.setConsecuenciaNoShow(p.consecuenciaNoShow());
        e.setTramos(new ArrayList<>(p.tramos().stream()
                .map(t -> new TramoCancelacionEmbedable(t.antelacionMinimaDias(), t.porcentajeRetencion()))
                .toList()));
        return e;
    }

    public PoliticaCancelacion aDominio(PoliticaCancelacionEntity e) {
        return new PoliticaCancelacion(e.getVersion(), e.getFechaVigencia(), e.getConsecuenciaNoShow(),
                e.getTramos().stream()
                        .map(t -> new TramoCancelacion(t.getAntelacionMinimaDias(), t.getPorcentajeRetencion()))
                        .toList());
    }

    public Pagina<PoliticaCancelacion> aPagina(Page<PoliticaCancelacionEntity> pagina) {
        return new Pagina<>(pagina.getContent().stream().map(this::aDominio).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements());
    }
}
