package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroLlegadaNocturna;
import co.edu.uniquindio.sga.domain.valueobject.Titular;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Compromiso de ocupar un apartamento durante una estancia, para un conjunto definido de ocupantes.
 * Solo conoce la identificación del apartamento, no el objeto completo (referencia entre agregados).
 *
 * Reglas que valida la propia reserva:
 * RN-02, RN-03, RN-04 y condición 3 de 7.5 (al crear y al modificar), RN-08 (ciclo de vida de la sección 8),
 * RN-09 (hora estimada antes de confirmar), RN-10 y RN-11 (registro), RN-12 (deja de retener noches al
 * salir de los estados activos), RN-13 (retención con la política congelada), RN-14 (modificación que
 * revalida y recalcula), RN-19 (canal externo con identificador), RN-21 (vencimiento), RN-22 (valor y
 * política congelados), 3.2 (el titular es un ocupante facturable).
 *
 * Reglas que dependen de otras reservas o agregados y por eso viven en servicios de dominio:
 * RN-01 y RN-07 y RN-20 (DisponibilidadApartamentoService), RP-01 (Alojamiento.validarEstanciaMinima),
 * RP-02 y RP-03 (Folio / CreacionReservaService), L-11 anticipo (ConfirmacionReservaService),
 * RN-17 antes de la salida (LiquidacionSalidaService), RN-18 (ConciliacionCanalExternoService).
 */
public class Reserva {

    private final CodigoReserva codigo;
    private IdentificacionApartamento apartamento;
    private Estancia estancia;
    private final Titular titular;
    private final List<Ocupante> ocupantes;
    private final CanalOrigen canalOrigen;
    private final String identificadorCanalExterno;
    private RegistroLlegadaNocturna registroLlegada;
    private Cotizacion cotizacion;
    private final PoliticaCancelacion politicaCancelacion;
    private EstadoReserva estado;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaConfirmacion;
    private LocalDate fechaRegistro;
    private LocalDate fechaSalidaEfectiva;
    private LocalDate fechaCancelacion;
    private String motivoCancelacion;

    private Reserva(CodigoReserva codigo, IdentificacionApartamento apartamento, Estancia estancia, Titular titular,
                    List<Ocupante> ocupantes, CanalOrigen canalOrigen, String identificadorCanalExterno,
                    LocalTime horaEstimadaLlegada, Cotizacion cotizacion, PoliticaCancelacion politicaCancelacion,
                    EstadoReserva estado, LocalDateTime fechaCreacion, LocalDateTime fechaConfirmacion,
                    LocalDate fechaRegistro, LocalDate fechaSalidaEfectiva, LocalDate fechaCancelacion,
                    String motivoCancelacion) {
        this.codigo = codigo;
        this.apartamento = apartamento;
        this.estancia = estancia;
        this.titular = titular;
        this.ocupantes = new ArrayList<>(ocupantes);
        this.canalOrigen = canalOrigen;
        this.identificadorCanalExterno = identificadorCanalExterno;
        this.registroLlegada = horaEstimadaLlegada == null ? null : new RegistroLlegadaNocturna(horaEstimadaLlegada);
        this.cotizacion = cotizacion;
        this.politicaCancelacion = politicaCancelacion;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaConfirmacion = fechaConfirmacion;
        this.fechaRegistro = fechaRegistro;
        this.fechaSalidaEfectiva = fechaSalidaEfectiva;
        this.fechaCancelacion = fechaCancelacion;
        this.motivoCancelacion = motivoCancelacion;
    }

    /**
     * Crea una reserva en estado PENDIENTE (7.5), congelando cotización y política (RN-22, 3.5).
     * Valida las condiciones 1 a 4 de 7.5 en ese orden; las condiciones 5 y 6 (solapamientos y tiempo
     * de preparación) las garantiza el servicio antes de llamar a este método.
     *
     * @param horaEstimadaLlegada puede venir vacía (por ejemplo desde un canal externo), pero RN-09 la exige
     *                            antes de confirmar.
     * @param ahora               fecha y hora actual en zona horaria de Colombia (3.1), la entrega el servicio.
     */
    public static Reserva crear(CodigoReserva codigo, Apartamento apartamento, Estancia estancia, Titular titular,
                                List<Ocupante> ocupantes, CanalOrigen canalOrigen, String identificadorCanalExterno,
                                LocalTime horaEstimadaLlegada, Cotizacion cotizacion,
                                PoliticaCancelacion politicaVigente, UmbralEdadFacturable umbral,
                                LocalDateTime ahora) {
        requerido(codigo, "El código de la reserva es obligatorio");
        requerido(titular, "La reserva debe tener un titular");
        requerido(canalOrigen, "El canal de origen es obligatorio");
        requerido(politicaVigente, "RN-22: la reserva debe congelar la política de cancelación vigente");
        requerido(ahora, "La fecha actual es obligatoria");
        validarCondicionesDeCreacion(apartamento, estancia, titular, ocupantes, cotizacion, umbral, ahora.toLocalDate());
        String idExterno = validarIdentificadorExterno(canalOrigen, identificadorCanalExterno);

        return new Reserva(codigo, apartamento.getIdentificacion(), estancia, titular, ocupantes, canalOrigen, idExterno,
                horaEstimadaLlegada, cotizacion, politicaVigente, EstadoReserva.PENDIENTE, ahora,
                null, null, null, null, null);
    }

    public static Reserva reconstruir(CodigoReserva codigo, IdentificacionApartamento apartamento, Estancia estancia,
                                      Titular titular, List<Ocupante> ocupantes, CanalOrigen canalOrigen,
                                      String identificadorCanalExterno, LocalTime horaEstimadaLlegada,
                                      Cotizacion cotizacion, PoliticaCancelacion politicaCancelacion,
                                      EstadoReserva estado, LocalDateTime fechaCreacion,
                                      LocalDateTime fechaConfirmacion, LocalDate fechaRegistro,
                                      LocalDate fechaSalidaEfectiva, LocalDate fechaCancelacion,
                                      String motivoCancelacion) {
        return new Reserva(codigo, apartamento, estancia, titular, ocupantes == null ? List.of() : ocupantes,
                canalOrigen, identificadorCanalExterno, horaEstimadaLlegada, cotizacion, politicaCancelacion, estado,
                fechaCreacion, fechaConfirmacion, fechaRegistro, fechaSalidaEfectiva, fechaCancelacion,
                motivoCancelacion);
    }

    // ---------------------------------------------------------------- Confirmación y vencimiento

    /** RN-09: la hora estimada de llegada se puede registrar o corregir mientras la reserva no haya iniciado. */
    public void registrarHoraEstimadaLlegada(LocalTime hora) {
        requerido(hora, "La hora estimada de llegada es obligatoria");
        validarNoIniciada("registrar la hora estimada de llegada");
        this.registroLlegada = new RegistroLlegadaNocturna(hora);
    }

    /**
     * PENDIENTE → CONFIRMADA. RN-09: exige hora estimada de llegada. RN-21: una reserva vencida no se confirma.
     * El anticipo exigido (L-11) lo verifica ConfirmacionReservaService contra el Folio.
     */
    public void confirmar(LocalDateTime ahora, PlazoConfirmacion plazo) {
        requerido(ahora, "La fecha actual es obligatoria");
        if (registroLlegada == null) {
            throw new ReglaDominioException("RN-09: La reserva " + codigo.valor()
                    + " debe registrar la hora estimada de llegada antes de confirmarse");
        }
        if (estaVencida(ahora, plazo)) {
            throw new ReglaDominioException("RN-21: La reserva " + codigo.valor()
                    + " superó el plazo de confirmación y debe cancelarse");
        }
        transitarA(EstadoReserva.CONFIRMADA);
        this.fechaConfirmacion = ahora;
    }

    /** RN-21: una reserva PENDIENTE que supera el plazo de confirmación está vencida. */
    public boolean estaVencida(LocalDateTime ahora, PlazoConfirmacion plazo) {
        requerido(ahora, "La fecha actual es obligatoria");
        requerido(plazo, "El plazo de confirmación es obligatorio");
        return estado == EstadoReserva.PENDIENTE && plazo.estaVencida(fechaCreacion, ahora);
    }

    /** RN-21 y RN-12: PENDIENTE vencida → CANCELADA; libera sus noches de inmediato. Lo invoca el proceso automático. */
    public void vencerPorPlazoDeConfirmacion(LocalDateTime ahora, PlazoConfirmacion plazo) {
        if (!estaVencida(ahora, plazo)) {
            throw new ReglaDominioException("RN-21: La reserva " + codigo.valor()
                    + " no ha superado el plazo de confirmación");
        }
        transitarA(EstadoReserva.CANCELADA);
        this.fechaCancelacion = ahora.toLocalDate();
        this.motivoCancelacion = "Vencimiento del plazo de confirmación";
    }

    // ---------------------------------------------------------------- Cancelación y no-show (RN-13)

    /**
     * RN-13: la retención se calcula con la versión de política congelada en la reserva,
     * no con la vigente al momento de cancelar.
     */
    public Dinero calcularRetencionPorCancelacion(LocalDate fechaCancelacion) {
        requerido(fechaCancelacion, "La fecha de cancelación es obligatoria");
        validarNoIniciada("cancelar");
        return politicaCancelacion.calcularRetencion(valorTotal(), estancia.fechaEntrada(), fechaCancelacion);
    }

    /** PENDIENTE o CONFIRMADA → CANCELADA, siempre antes del registro (7.5). Libera las noches (RN-12). */
    public void cancelar(String motivo, LocalDate fechaCancelacion) {
        textoRequerido(motivo, "La cancelación debe indicar un motivo");
        requerido(fechaCancelacion, "La fecha de cancelación es obligatoria");
        transitarA(EstadoReserva.CANCELADA);
        this.fechaCancelacion = fechaCancelacion;
        this.motivoCancelacion = motivo.trim();
    }

    /**
     * RN-13: la consecuencia del no-show también sale de la política congelada (tramo de 0 días de antelación,
     * que en el Anexo A es "menos de 7 días o no-show").
     */
    public Dinero calcularRetencionPorNoShow() {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("RN-08: Solo una reserva CONFIRMADA puede declararse no-show");
        }
        return politicaCancelacion.calcularRetencionNoShow(valorTotal());
    }

    /** CONFIRMADA → NO_SHOW, a partir de la hora límite del día de entrada (L-15). Libera las noches (RN-12). */
    public void declararNoShow(LocalDateTime ahora, LocalTime horaLimiteNoShow) {
        requerido(ahora, "La fecha actual es obligatoria");
        requerido(horaLimiteNoShow, "La hora límite de no-show es obligatoria (L-15)");
        LocalDateTime limite = estancia.fechaEntrada().atTime(horaLimiteNoShow);
        if (ahora.isBefore(limite)) {
            throw new ReglaDominioException("L-15: El no-show solo puede declararse a partir de " + limite);
        }
        transitarA(EstadoReserva.NO_SHOW);
    }

    // ---------------------------------------------------------------- Registro y salida

    /**
     * CONFIRMADA → EN_CURSO. RN-10: no antes de la fecha de entrada y solo sobre una reserva CONFIRMADA.
     * RN-11: el apartamento debe estar PREPARADO (el servicio también llama a Apartamento.recibirGrupo()).
     */
    public void registrarLlegada(LocalDate hoy, EstadoOperativo estadoApartamento) {
        requerido(hoy, "La fecha actual es obligatoria");
        requerido(estadoApartamento, "El estado operativo del apartamento es obligatorio");
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("RN-10: Solo se registra la llegada de una reserva CONFIRMADA (estado actual: "
                    + estado + ")");
        }
        if (hoy.isBefore(estancia.fechaEntrada())) {
            throw new ReglaDominioException("RN-10: No se permite el registro antes de la fecha de entrada "
                    + estancia.fechaEntrada());
        }
        if (!hoy.isBefore(estancia.fechaSalida())) {
            throw new ReglaDominioException("RN-10: La estancia ya terminó; no se puede registrar la llegada");
        }
        if (!estadoApartamento.permiteRegistro()) {
            throw new ReglaDominioException("RN-11: El apartamento no está PREPARADO (estado actual: "
                    + estadoApartamento + ")");
        }
        transitarA(EstadoReserva.EN_CURSO);
        this.fechaRegistro = hoy;
    }

    /**
     * EN_CURSO → FINALIZADA (RN-12: libera sus noches). Una salida anticipada es una salida, no una cancelación.
     * El cierre del folio como requisito (7.6, RN-17) lo verifica LiquidacionSalidaService.
     */
    public void registrarSalida(LocalDate hoy) {
        requerido(hoy, "La fecha actual es obligatoria");
        if (fechaRegistro != null && hoy.isBefore(fechaRegistro)) {
            throw new ReglaDominioException("La salida no puede ser anterior al registro");
        }
        transitarA(EstadoReserva.FINALIZADA);
        this.fechaSalidaEfectiva = hoy;
    }

    // ---------------------------------------------------------------- Modificación (RN-14, RN-22)

    /**
     * RN-14: cambia fechas, ocupantes o apartamento de una reserva no iniciada (PENDIENTE o CONFIRMADA),
     * revalidando las condiciones de creación 1 a 4 y reemplazando la cotización por una calculada con las
     * tarifas vigentes. La política congelada no se recalcula (7.5).
     * Las condiciones 5 y 6 (solapamiento y tiempo de preparación) las revalida ModificacionReservaService.
     *
     * @return diferencia entre el valor nuevo y el anterior; el servicio la asienta como ajuste en el Folio.
     */
    public Dinero modificar(Apartamento nuevoApartamento, Estancia nuevaEstancia, List<Ocupante> nuevosOcupantes,
                            Cotizacion nuevaCotizacion, UmbralEdadFacturable umbral, LocalDateTime ahora) {
        requerido(ahora, "La fecha actual es obligatoria");
        validarNoIniciada("modificar");
        validarCondicionesDeCreacion(nuevoApartamento, nuevaEstancia, titular, nuevosOcupantes, nuevaCotizacion,
                umbral, ahora.toLocalDate());

        Dinero diferencia = nuevaCotizacion.valorTotal().restar(cotizacion.valorTotal());
        this.apartamento = nuevoApartamento.getIdentificacion();
        this.estancia = nuevaEstancia;
        this.ocupantes.clear();
        this.ocupantes.addAll(nuevosOcupantes);
        this.cotizacion = nuevaCotizacion;
        return diferencia;
    }

    // ---------------------------------------------------------------- Consultas de disponibilidad

    /** RN-12 y sección 8: activas = PENDIENTE, CONFIRMADA o EN_CURSO. */
    public boolean estaActiva() {
        return estado.retieneDisponibilidad();
    }

    /** RN-01: una reserva activa ocupa las noches de su estancia, sin importar el canal. */
    public boolean ocupaNochesDe(Estancia otraEstancia) {
        requerido(otraEstancia, "La estancia es obligatoria");
        return estaActiva() && estancia.seSolapaCon(otraEstancia);
    }

    public boolean esDelApartamento(IdentificacionApartamento identificacion) {
        return apartamento.equals(identificacion);
    }

    /** RN-19: la combinación canal + identificador externo identifica un mismo mensaje. */
    public boolean correspondeA(CanalOrigen canal, String identificadorExterno) {
        return canalOrigen == canal && identificadorCanalExterno != null
                && identificadorCanalExterno.equals(identificadorExterno);
    }

    public int totalOcupantes() {
        return ocupantes.size();
    }

    /** RN-06: los no facturables cuentan para la capacidad pero no generan cargo. */
    public long ocupantesFacturables(UmbralEdadFacturable umbral) {
        requerido(umbral, "El umbral de edad facturable es obligatorio");
        return ocupantes.stream().filter(o -> o.esFacturableEn(estancia, umbral)).count();
    }

    /** RN-22: valor congelado al crear la reserva (o en la última modificación explícita). */
    public Dinero valorTotal() {
        return cotizacion.valorTotal();
    }

    // ---------------------------------------------------------------- Ciclo de vida (RN-08)

    private void transitarA(EstadoReserva destino) {
        if (!esTransicionValida(estado, destino)) {
            throw new ReglaDominioException("RN-08: Transición no permitida para la reserva " + codigo.valor()
                    + ": " + estado + " → " + destino);
        }
        this.estado = destino;
    }

    /** Sección 8: toda transición no listada se rechaza. */
    private static boolean esTransicionValida(EstadoReserva desde, EstadoReserva hacia) {
        return switch (desde) {
            case PENDIENTE -> hacia == EstadoReserva.CONFIRMADA || hacia == EstadoReserva.CANCELADA;
            case CONFIRMADA -> hacia == EstadoReserva.EN_CURSO || hacia == EstadoReserva.CANCELADA
                    || hacia == EstadoReserva.NO_SHOW;
            case EN_CURSO -> hacia == EstadoReserva.FINALIZADA;
            case FINALIZADA, CANCELADA, NO_SHOW -> false;
        };
    }

    private void validarNoIniciada(String accion) {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new ReglaDominioException("RN-08: No se puede " + accion + " la reserva " + codigo.valor()
                    + " en estado " + estado);
        }
    }

    // ---------------------------------------------------------------- Validaciones de creación (7.5)

    private static void validarCondicionesDeCreacion(Apartamento apartamento, Estancia estancia, Titular titular,
                                                     List<Ocupante> ocupantes, Cotizacion cotizacion,
                                                     UmbralEdadFacturable umbral, LocalDate hoy) {
        requerido(apartamento, "El apartamento es obligatorio");
        requerido(estancia, "La estancia es obligatoria");
        requerido(cotizacion, "RN-22: la reserva debe congelar la cotización");
        requerido(umbral, "El umbral de edad facturable es obligatorio (L-09)");
        if (ocupantes == null || ocupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante");
        }

        // 1. RN-03
        if (estancia.noches() < 1) {
            throw new ReglaDominioException("RN-03: La fecha de salida debe ser posterior a la de entrada");
        }
        // 2. RN-04
        if (estancia.fechaEntrada().isBefore(hoy)) {
            throw new ReglaDominioException("RN-04: La fecha de entrada " + estancia.fechaEntrada()
                    + " es anterior a la fecha actual " + hoy);
        }
        // 3. Apartamento activo (las tarifas completas se exigen para activarlo, 7.4)
        if (!apartamento.estaVigenteParaVenta()) {
            throw new ReglaDominioException("7.5: El apartamento " + apartamento.getIdentificacion().codigo()
                    + " no está activo para la venta");
        }
        // 4. RN-02
        apartamento.validarCapacidadPara(ocupantes.size());

        // 3.2: el titular es siempre un ocupante facturable
        if (!titular.esTitularFacturable(ocupantes, estancia, umbral)) {
            throw new ReglaDominioException("3.2: El titular debe ser un ocupante facturable de la reserva");
        }
        // RN-05 / RN-22: la cotización congelada debe corresponder a la estancia
        if (cotizacion.totalNoches() != estancia.noches()) {
            throw new ReglaDominioException("La cotización no corresponde a las noches de la estancia");
        }
    }

    /** RN-19: una reserva externa llega siempre con el identificador propio del canal. */
    private static String validarIdentificadorExterno(CanalOrigen canal, String identificadorExterno) {
        boolean tieneIdentificador = identificadorExterno != null && !identificadorExterno.isBlank();
        if (canal.exigeIdentificadorExterno() && !tieneIdentificador) {
            throw new ReglaDominioException("RN-19: Una reserva del canal " + canal
                    + " debe traer el identificador externo del canal");
        }
        if (!canal.exigeIdentificadorExterno() && tieneIdentificador) {
            throw new ReglaDominioException("Solo las reservas externas llevan identificador de canal");
        }
        return tieneIdentificador ? identificadorExterno.trim() : null;
    }

    // ---------------------------------------------------------------- Getters

    public CodigoReserva getCodigo() { return codigo; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public Estancia getEstancia() { return estancia; }
    public Titular getTitular() { return titular; }
    public List<Ocupante> getOcupantes() { return Collections.unmodifiableList(ocupantes); }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public String getIdentificadorCanalExterno() { return identificadorCanalExterno; }
    public RegistroLlegadaNocturna getRegistroLlegada() { return registroLlegada; }
    public LocalTime getHoraEstimadaLlegada() { return registroLlegada == null ? null : registroLlegada.horaEstimada(); }
    public Cotizacion getCotizacion() { return cotizacion; }
    public PoliticaCancelacion getPoliticaCancelacion() { return politicaCancelacion; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public LocalDate getFechaSalidaEfectiva() { return fechaSalidaEfectiva; }
    public LocalDate getFechaCancelacion() { return fechaCancelacion; }
    public String getMotivoCancelacion() { return motivoCancelacion; }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva otra)) return false;
        return codigo.equals(otra.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
