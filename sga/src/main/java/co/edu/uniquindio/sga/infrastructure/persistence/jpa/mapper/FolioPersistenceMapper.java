package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AutorizacionCierreEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CargoEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.DepositoGarantiaEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PagoEmbedable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Traduce a mano Folio y FolioEntity (Guía 09, 6.6). Al leer usa Folio.reconstruir: recrea cargos y pagos tal como
 * se guardaron, sin volver a asentar el cargo de alojamiento ni el depósito como haría abrir.
 */
@Component
public class FolioPersistenceMapper {

    public FolioEntity aEntidad(Folio f) {
        FolioEntity e = new FolioEntity();
        e.setFolioId(f.getId());
        e.setCodigoReserva(f.getReserva().valor());
        DepositoGarantia deposito = f.getDepositoGarantia();
        if (deposito != null) {
            e.setDepositoGarantia(new DepositoGarantiaEmbedable(deposito.concepto(), deposito.monto().monto(),
                    deposito.reembolsado()));
        }
        e.setFechaApertura(f.getFechaApertura());
        e.setCargos(new ArrayList<>(f.getCargos().stream()
                .map(c -> new CargoEmbedable(c.tipo(), c.concepto(), c.valor().monto(), c.fecha()))
                .toList()));
        e.setPagos(new ArrayList<>(f.getPagos().stream()
                .map(p -> new PagoEmbedable(p.valor().monto(), p.medio(), p.fecha()))
                .toList()));
        e.setCerrado(f.isCerrado());
        e.setFechaCierre(f.getFechaCierre());
        AutorizacionCierre autorizacion = f.getAutorizacionCierre();
        if (autorizacion != null) {
            e.setAutorizacionCierre(new AutorizacionCierreEmbedable(autorizacion.autorizadoPor(), autorizacion.motivo(),
                    autorizacion.fecha()));
        }
        return e;
    }

    public Folio aDominio(FolioEntity e) {
        // Hibernate deja el @Embedded en null cuando todas sus columnas son nulas (folio sin autorización)
        DepositoGarantiaEmbedable deposito = e.getDepositoGarantia();
        AutorizacionCierreEmbedable autorizacion = e.getAutorizacionCierre();
        return Folio.reconstruir(e.getFolioId(), new CodigoReserva(e.getCodigoReserva()),
                deposito == null || deposito.getDepositoMonto() == null ? null
                        : new DepositoGarantia(deposito.getDepositoConcepto(), new Dinero(deposito.getDepositoMonto()),
                        deposito.isDepositoReembolsado()),
                e.getFechaApertura(),
                e.getCargos().stream()
                        .map(c -> new Cargo(c.getTipo(), c.getConcepto(), new Dinero(c.getValor()), c.getFecha()))
                        .toList(),
                e.getPagos().stream()
                        .map(p -> new Pago(new Dinero(p.getValor()), p.getMedio(), p.getFecha()))
                        .toList(),
                e.isCerrado(), e.getFechaCierre(),
                autorizacion == null || autorizacion.getAutorizadoPor() == null ? null
                        : new AutorizacionCierre(autorizacion.getAutorizadoPor(), autorizacion.getMotivoAutorizacion(),
                        autorizacion.getFechaAutorizacion()));
    }
}