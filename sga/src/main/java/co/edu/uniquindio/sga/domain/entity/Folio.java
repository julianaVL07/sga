package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cuenta de la reserva: acumula cargos y pagos y determina el saldo (glosario, 7.7).
 * Un folio pertenece a una sola reserva y se abre al crearla.
 *
 * Reglas: RN-15 (pago con medio y fecha; saldo = cargos - pagos), RN-16 (cargos y pagos inmutables,
 * corrección por movimiento inverso), RN-17 (no se cierra con saldo distinto de cero sin autorización),
 * RN-14 y RN-22 (la diferencia de una modificación se asienta como ajuste),
 * RP-02 (depósito de garantía obligatorio al abrir).
 */
public class Folio {

    private final String id;
    private final CodigoReserva reserva;
    private DepositoGarantia depositoGarantia;
    private final LocalDate fechaApertura;
    private final List<Cargo> cargos;
    private final List<Pago> pagos;
    private boolean cerrado;
    private LocalDate fechaCierre;
    private AutorizacionCierre autorizacionCierre;

    private Folio(String id, CodigoReserva reserva, DepositoGarantia depositoGarantia, LocalDate fechaApertura, List<Cargo> cargos, List<Pago> pagos,
                  boolean cerrado, LocalDate fechaCierre, AutorizacionCierre autorizacionCierre) {
        this.id = id;
        this.reserva = reserva;
        this.depositoGarantia = depositoGarantia;
        this.fechaApertura = fechaApertura;
        this.cargos = new ArrayList<>(cargos);
        this.pagos = new ArrayList<>(pagos);
        this.cerrado = cerrado;
        this.fechaCierre = fechaCierre;
        this.autorizacionCierre = autorizacionCierre;
    }

    /**
     * 7.7: el folio se abre al crear la reserva con el cargo de alojamiento calculado.
     * RP-02: en la apertura se asienta, de manera obligatoria, el cargo de depósito de garantía.
     */
    public static Folio abrir(String id, CodigoReserva reserva, Cargo cargoAlojamiento,
                              DepositoGarantia depositoGarantia, LocalDate fechaApertura) {
        textoRequerido(id, "El id del folio es obligatorio");
        requerido(reserva, "El folio debe pertenecer a una reserva");
        requerido(cargoAlojamiento, "El folio se abre con el cargo de alojamiento");
        requerido(depositoGarantia, "RP-02: el depósito de garantía es obligatorio al abrir el folio");
        requerido(fechaApertura, "La fecha de apertura es obligatoria");
        if (!cargoAlojamiento.esDeAlojamiento()) {
            throw new ReglaDominioException("El cargo inicial del folio debe ser de tipo ALOJAMIENTO");
        }
        Folio folio = new Folio(id, reserva, depositoGarantia, fechaApertura, List.of(), List.of(), false, null, null);
        folio.agregarCargo(cargoAlojamiento);
        folio.agregarCargo(depositoGarantia.generarCargoAjuste(fechaApertura));
        return folio;
    }

    public static Folio reconstruir(String id, CodigoReserva reserva, DepositoGarantia depositoGarantia,
                                    LocalDate fechaApertura, List<Cargo> cargos, List<Pago> pagos, boolean cerrado, LocalDate fechaCierre,
                                    AutorizacionCierre autorizacionCierre) {
        return new Folio(id, reserva, depositoGarantia, fechaApertura, cargos == null ? List.of() : cargos,
                pagos == null ? List.of() : pagos, cerrado, fechaCierre, autorizacionCierre);
    }

    // ---------------------------------------------------------------- Movimientos

    /** Agrega un cargo (servicio adicional, recargo RP-03, etc.). Nunca se modifica ni elimina (RN-16). */
    public void agregarCargo(Cargo nuevoCargo) {
        validarAbierto();
        requerido(nuevoCargo, "El cargo es obligatorio");
        requerido(nuevoCargo.tipo(), "El cargo debe tener tipo");
        requerido(nuevoCargo.valor(), "El cargo debe tener valor");
        requerido(nuevoCargo.fecha(), "El cargo debe tener fecha");
        textoRequerido(nuevoCargo.concepto(), "El cargo debe tener concepto");
        cargos.add(nuevoCargo);
    }

    /** RN-15: todo pago se registra con su medio y su fecha. Un folio admite pagos parciales y medios distintos. */
    public void registrarPago(Pago pago) {
        validarAbierto();
        requerido(pago, "El pago es obligatorio");
        if (pago.medio() == null) {
            throw new ReglaDominioException("RN-15: Todo pago debe indicar su medio de pago");
        }
        if (pago.fecha() == null) {
            throw new ReglaDominioException("RN-15: Todo pago debe indicar su fecha");
        }
        if (pago.valor() == null || pago.valor().esCero() || pago.valor().esNegativo()) {
            throw new ReglaDominioException("El valor del pago debe ser mayor que cero");
        }
        pagos.add(pago);
    }

    /**
     * RN-16: una corrección no edita el cargo original, registra un cargo del mismo tipo con valor opuesto.
     */
    public void registrarMovimientoInverso(Cargo cargoACorregir, String motivo, LocalDate fecha) {
        validarAbierto();
        requerido(cargoACorregir, "El cargo a corregir es obligatorio");
        textoRequerido(motivo, "La corrección debe indicar un motivo");
        requerido(fecha, "La fecha de la corrección es obligatoria");
        if (!cargos.contains(cargoACorregir)) {
            throw new ReglaDominioException("RN-16: Solo se pueden corregir cargos registrados en este folio");
        }
        cargos.add(cargoACorregir.inverso(motivo, fecha));
    }

    /**
     * RN-14: la diferencia de valor de una modificación se asienta como ajuste, positivo o negativo.
     * Si no hay diferencia no se registra nada.
     */
    public void registrarAjustePorModificacion(Dinero diferencia, String concepto, LocalDate fecha) {
        requerido(diferencia, "La diferencia es obligatoria");
        if (diferencia.esCero()) {
            return;
        }
        agregarCargo(new Cargo(TipoCargo.AJUSTE_MODIFICACION, concepto, diferencia, fecha));
    }

    /** RN-13: la retención calculada con la política congelada se registra como penalidad. */
    public void registrarPenalidad(Dinero retencion, String concepto, LocalDate fecha) {
        requerido(retencion, "La retención es obligatoria");
        if (retencion.esNegativo()) {
            throw new ReglaDominioException("La penalidad no puede ser negativa");
        }
        if (retencion.esCero()) {
            return;
        }
        agregarCargo(new Cargo(TipoCargo.PENALIDAD_CANCELACION_NOSHOW, concepto, retencion, fecha));
    }

    /**
     * Cancelación, no-show o vencimiento: revierte con movimientos inversos todos los cargos registrados
     * (RN-16), sin borrar ninguno. Después el servicio asienta la penalidad, si la hay, y los pagos
     * que sobren quedan como saldo a favor del titular (7.5).
     */
    public void anularCargos(String motivo, LocalDate fecha) {
        validarAbierto();
        textoRequerido(motivo, "La anulación debe indicar un motivo");
        requerido(fecha, "La fecha de la anulación es obligatoria");
        List<Cargo> registrados = List.copyOf(cargos);
        registrados.forEach(cargo -> cargos.add(cargo.inverso(motivo, fecha)));
    }

    // ---------------------------------------------------------------- Saldo (RN-15)

    public Dinero totalCargos() {
        return cargos.stream().map(Cargo::valor).reduce(Dinero.cero(), Dinero::sumar);
    }

    public Dinero totalPagado() {
        return pagos.stream().map(Pago::valor).reduce(Dinero.cero(), Dinero::sumar);
    }

    /** RN-15: positivo = debe el huésped, cero, negativo = saldo a favor. */
    public Dinero saldo() {
        return totalCargos().restar(totalPagado());
    }

    /** L-11: lo usa ConfirmacionReservaService para verificar que el anticipo exigido está cubierto. */
    public boolean cubreAnticipo(Dinero anticipoExigido) {
        requerido(anticipoExigido, "El anticipo exigido es obligatorio");
        return !totalPagado().restar(anticipoExigido).esNegativo();
    }

    // ---------------------------------------------------------------- Cierre (RN-17)

    /** RN-17: el folio solo se cierra sin autorización cuando el saldo es exactamente cero. */
    public void cerrar(LocalDate fecha) {
        validarAbierto();
        requerido(fecha, "La fecha de cierre es obligatoria");
        if (!saldo().esCero()) {
            throw new ReglaDominioException("RN-17: El folio " + id + " tiene saldo " + saldo().monto()
                    + " y no puede cerrarse sin autorización del administrador");
        }
        this.cerrado = true;
        this.fechaCierre = fecha;
    }

    /** RN-17: cierre con saldo distinto de cero; la autorización queda registrada con autor y motivo. */
    public void cerrarConAutorizacion(AutorizacionCierre autorizacion) {
        validarAbierto();
        requerido(autorizacion, "La autorización de cierre es obligatoria");
        textoRequerido(autorizacion.autorizadoPor(), "RN-17: la autorización debe indicar quién autoriza");
        textoRequerido(autorizacion.motivo(), "RN-17: la autorización debe indicar el motivo");
        requerido(autorizacion.fecha(), "RN-17: la autorización debe indicar la fecha");
        this.autorizacionCierre = autorizacion;
        this.cerrado = true;
        this.fechaCierre = autorizacion.fecha();
    }

    private void validarAbierto() {
        if (cerrado) {
            throw new ReglaDominioException("RN-16: El folio " + id + " está cerrado y no admite movimientos");
        }
    }

    // ---------------------------------------------------------------- Getters (listas de solo lectura, RN-16)

    public String getId() { return id; }
    public CodigoReserva getReserva() { return reserva; }
    public DepositoGarantia getDepositoGarantia() { return depositoGarantia; }
    public LocalDate getFechaApertura() { return fechaApertura; }
    public List<Cargo> getCargos() { return Collections.unmodifiableList(cargos); }
    public List<Pago> getPagos() { return Collections.unmodifiableList(pagos); }
    public boolean isCerrado() { return cerrado; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public AutorizacionCierre getAutorizacionCierre() { return autorizacionCierre; }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Folio otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
