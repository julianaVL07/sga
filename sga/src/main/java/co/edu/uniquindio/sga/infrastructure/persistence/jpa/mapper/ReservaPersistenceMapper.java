package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DetalleNoche;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.Titular;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.DetalleNocheEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EstanciaEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.OcupanteEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TitularEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TramoCancelacionEmbedable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Traduce entre Reserva (dominio) y ReservaEntity (tabla), a mano (Guía 09, 6.6): Reserva solo se construye con
 * Reserva.reconstruir(...) y varios objetos de valor validan en su constructor, algo que MapStruct no maneja bien.
 * Al leer usa reconstruir, que recrea el estado guardado sin repetir las validaciones de creación
 * (RN-04 rechazaría una reserva antigua con fecha de entrada pasada).
 */
@Component
public class ReservaPersistenceMapper {

    // ==================== DOMINIO → TABLA ====================

    public ReservaEntity aEntidad(Reserva r) {
        ReservaEntity e = new ReservaEntity();
        e.setCodigo(r.getCodigo().valor());
        e.setApartamentoId(r.getApartamento().codigo());
        e.setEstancia(new EstanciaEmbedable(r.getEstancia().fechaEntrada(), r.getEstancia().fechaSalida()));
        e.setTitular(aEmbedable(r.getTitular()));
        e.setOcupantes(new ArrayList<>(r.getOcupantes().stream().map(this::aEmbedable).toList()));
        e.setCanalOrigen(r.getCanalOrigen());
        e.setIdentificadorCanalExterno(r.getIdentificadorCanalExterno());
        e.setHoraEstimadaLlegada(r.getHoraEstimadaLlegada());

        e.setDetallesCotizacion(new ArrayList<>(r.getCotizacion().detalles().stream().map(this::aEmbedable).toList()));
        e.setValorTotal(r.getCotizacion().valorTotal().monto());

        PoliticaCancelacion politica = r.getPoliticaCancelacion();
        e.setPoliticaVersion(politica.version());
        e.setPoliticaFechaVigencia(politica.fechaVigencia());
        e.setPoliticaConsecuenciaNoShow(politica.consecuenciaNoShow());
        e.setPoliticaTramos(new ArrayList<>(politica.tramos().stream()
                .map(t -> new TramoCancelacionEmbedable(t.antelacionMinimaDias(), t.porcentajeRetencion()))
                .toList()));

        e.setEstado(r.getEstado());
        e.setFechaCreacion(r.getFechaCreacion());
        e.setFechaConfirmacion(r.getFechaConfirmacion());
        e.setFechaRegistro(r.getFechaRegistro());
        e.setFechaSalidaEfectiva(r.getFechaSalidaEfectiva());
        e.setFechaCancelacion(r.getFechaCancelacion());
        e.setMotivoCancelacion(r.getMotivoCancelacion());
        return e;
    }

    private TitularEmbedable aEmbedable(Titular t) {
        return new TitularEmbedable(t.nombre(), t.documento().tipo(), t.documento().numero(), t.email(), t.telefono());
    }

    private OcupanteEmbedable aEmbedable(Ocupante o) {
        DocumentoIdentidad documento = o.documento();
        return new OcupanteEmbedable(o.nombre(), documento == null ? null : documento.tipo(),
                documento == null ? null : documento.numero(), o.fechaNacimiento());
    }

    private DetalleNocheEmbedable aEmbedable(DetalleNoche d) {
        return new DetalleNocheEmbedable(d.fecha(), d.temporada(), d.tarifaAplicada().monto(),
                d.ocupantesFacturables(), d.subtotal().monto());
    }

    // ==================== TABLA → DOMINIO ====================

    /** CU-19: la Page de Spring Data se vuelve Pagina del dominio, que no depende de Spring. */
    public Pagina<Reserva> aPagina(Page<ReservaEntity> pagina) {
        return new Pagina<>(pagina.getContent().stream().map(this::aDominio).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements());
    }

    public Reserva aDominio(ReservaEntity e) {
        return Reserva.reconstruir(
                new CodigoReserva(e.getCodigo()),
                new IdentificacionApartamento(e.getApartamentoId()),
                new Estancia(e.getEstancia().fechaEntrada(), e.getEstancia().fechaSalida()),
                aTitular(e.getTitular()),
                e.getOcupantes().stream().map(this::aOcupante).toList(),
                e.getCanalOrigen(), e.getIdentificadorCanalExterno(), e.getHoraEstimadaLlegada(),
                aCotizacion(e), aPolitica(e),
                e.getEstado(), e.getFechaCreacion(), e.getFechaConfirmacion(), e.getFechaRegistro(),
                e.getFechaSalidaEfectiva(), e.getFechaCancelacion(), e.getMotivoCancelacion());
    }

    private Titular aTitular(TitularEmbedable t) {
        return new Titular(t.getNombre(), new DocumentoIdentidad(t.getTipoDocumento(), t.getNumeroDocumento()),
                t.getEmail(), t.getTelefono());
    }

    private Ocupante aOcupante(OcupanteEmbedable o) {
        DocumentoIdentidad documento = o.getNumeroDocumento() == null ? null
                : new DocumentoIdentidad(o.getTipoDocumento(), o.getNumeroDocumento());
        return new Ocupante(o.getNombre(), documento, o.getFechaNacimiento());
    }

    /** RN-22: se recrea la cotización congelada; su constructor comprueba que el total sea la suma de las noches. */
    private Cotizacion aCotizacion(ReservaEntity e) {
        return new Cotizacion(e.getDetallesCotizacion().stream()
                .map(d -> new DetalleNoche(d.getFecha(), d.getTemporada(), new Dinero(d.getTarifaAplicada()),
                        d.getOcupantesFacturables(), new Dinero(d.getSubtotal())))
                .toList(),
                new Dinero(e.getValorTotal()));
    }

    private PoliticaCancelacion aPolitica(ReservaEntity e) {
        return new PoliticaCancelacion(e.getPoliticaVersion(), e.getPoliticaFechaVigencia(),
                e.getPoliticaConsecuenciaNoShow(),
                e.getPoliticaTramos().stream()
                        .map(t -> new TramoCancelacion(t.getAntelacionMinimaDias(), t.getPorcentajeRetencion()))
                        .toList());
    }
}
