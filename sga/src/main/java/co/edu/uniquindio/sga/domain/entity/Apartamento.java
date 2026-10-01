package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Unidad vendible: vivienda autónoma e identificada, entregada en exclusiva a un grupo (F-01).
 *
 * Reglas que valida: RN-02 (capacidad), RN-07 (bloqueos), RN-11 (solo recibe grupo si está PREPARADO),
 * transiciones de estado operativo (7.6), imágenes 1..10 con una principal (7.3),
 * tarifas completas para activarse (7.4), retiro de venta sin reservas activas ni futuras (7.3).
 *
 * RN-01 y RN-20 comparan varias reservas entre sí, por eso viven en DisponibilidadApartamentoService.
 */
public class Apartamento {

    private static final int MINIMO_IMAGENES = 1;
    private static final int MAXIMO_IMAGENES = 10;

    private final IdentificacionApartamento identificacion;
    private String nombre;
    private String descripcion;
    private int dormitorios;
    private Capacidad capacidad;
    private Dotacion dotacion;
    private final List<Imagen> imagenes;
    private EstadoOperativo estadoOperativo;
    private boolean activo;
    private boolean eliminado;
    private final List<Bloqueo> bloqueos;
    private final List<Tarifa> tarifas;

    private Apartamento(IdentificacionApartamento identificacion, String nombre, String descripcion, int dormitorios,
                        Capacidad capacidad, Dotacion dotacion, List<Imagen> imagenes,
                        EstadoOperativo estadoOperativo, boolean activo, boolean eliminado,
                        List<Bloqueo> bloqueos, List<Tarifa> tarifas) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
        this.dotacion = dotacion;
        this.imagenes = new ArrayList<>(imagenes);
        this.estadoOperativo = estadoOperativo;
        this.activo = activo;
        this.eliminado = eliminado;
        this.bloqueos = new ArrayList<>(bloqueos);
        this.tarifas = new ArrayList<>(tarifas);
    }

    /**
     * Crea un apartamento nuevo. Nace PREPARADO e inactivo: solo se activa cuando tiene tarifa
     * en todas las temporadas (7.4).
     */
    public static Apartamento crear(IdentificacionApartamento identificacion, String nombre, String descripcion,
                                    int dormitorios, Capacidad capacidad, Dotacion dotacion, List<Imagen> imagenes) {
        requerido(identificacion, "La identificación del apartamento es obligatoria (L-04)");
        textoRequerido(nombre, "El nombre del apartamento es obligatorio");
        validarDormitorios(dormitorios);
        requerido(capacidad, "La capacidad del apartamento es obligatoria");
        requerido(dotacion, "La dotación del apartamento es obligatoria (L-06)");
        validarImagenes(imagenes);
        return new Apartamento(identificacion, nombre.trim(), descripcion, dormitorios, capacidad, dotacion,
                imagenes, EstadoOperativo.PREPARADO, false, false, List.of(), List.of());
    }

    public static Apartamento reconstruir(IdentificacionApartamento identificacion, String nombre, String descripcion,
                                          int dormitorios, Capacidad capacidad, Dotacion dotacion,
                                          List<Imagen> imagenes, EstadoOperativo estadoOperativo, boolean activo,
                                          boolean eliminado, List<Bloqueo> bloqueos, List<Tarifa> tarifas) {
        return new Apartamento(identificacion, nombre, descripcion, dormitorios, capacidad, dotacion,
                imagenes == null ? List.of() : imagenes, estadoOperativo, activo, eliminado,
                bloqueos == null ? List.of() : bloqueos, tarifas == null ? List.of() : tarifas);
    }

    // ---------------------------------------------------------------- Capacidad (RN-02)

    /** RN-02: todos los ocupantes cuentan para la capacidad, sean facturables o no. */
    public boolean admite(int totalOcupantes) {
        return totalOcupantes > 0 && !capacidad.excede(totalOcupantes);
    }

    /** RN-02: la capacidad es un tope rígido, sin excepciones. */
    public void validarCapacidadPara(int totalOcupantes) {
        if (!admite(totalOcupantes)) {
            throw new ReglaDominioException("RN-02: El apartamento " + identificacion.codigo() + " admite máximo "
                    + capacidad.maximo() + " ocupantes y se solicitaron " + totalOcupantes);
        }
    }

    /** 7.3: cambiar la capacidad no afecta las reservas ya creadas; advertirlas es tarea del servicio. */
    public void cambiarCapacidad(Capacidad nuevaCapacidad) {
        requerido(nuevaCapacidad, "La capacidad es obligatoria");
        this.capacidad = nuevaCapacidad;
    }

    // ---------------------------------------------------------------- Estado operativo (RN-11, 7.6)

    /** RN-11: un apartamento solo puede recibir un grupo si su estado operativo es PREPARADO. */
    public boolean puedeRecibirGrupo() {
        return estadoOperativo.permiteRegistro();
    }

    /** Registro del grupo: PREPARADO → OCUPADO. */
    public void recibirGrupo() {
        if (!puedeRecibirGrupo()) {
            throw new ReglaDominioException("RN-11: El apartamento " + identificacion.codigo()
                    + " no está PREPARADO (estado actual: " + estadoOperativo + ")");
        }
        cambiarEstadoOperativo(EstadoOperativo.OCUPADO);
    }

    /** Salida del grupo: OCUPADO → PENDIENTE_PREPARACION. */
    public void liberarTrasSalida() {
        cambiarEstadoOperativo(EstadoOperativo.PENDIENTE_PREPARACION);
    }

    /** Personal de servicio: PENDIENTE_PREPARACION → EN_PREPARACION. */
    public void iniciarPreparacion() {
        cambiarEstadoOperativo(EstadoOperativo.EN_PREPARACION);
    }

    /** Personal de servicio: EN_PREPARACION → PREPARADO. */
    public void terminarPreparacion() {
        cambiarEstadoOperativo(EstadoOperativo.PREPARADO);
    }

    /**
     * Decisión administrativa (7.6): FUERA_DE_SERVICIO desde cualquier estado no ocupado.
     * Recordatorio 3.3: esto no bloquea la venta futura; para eso el administrador registra un Bloqueo.
     */
    public void ponerFueraDeServicio() {
        cambiarEstadoOperativo(EstadoOperativo.FUERA_DE_SERVICIO);
    }

    /** Al volver al servicio el apartamento debe prepararse antes de recibir un grupo. */
    public void restablecerServicio() {
        cambiarEstadoOperativo(EstadoOperativo.PENDIENTE_PREPARACION);
    }

    /** Aplica una transición de estado operativo, rechazando las no permitidas por 7.6. */
    public void cambiarEstadoOperativo(EstadoOperativo nuevoEstado) {
        requerido(nuevoEstado, "El nuevo estado operativo es obligatorio");
        if (!esTransicionPermitida(estadoOperativo, nuevoEstado)) {
            throw new ReglaDominioException("7.6: Transición de estado operativo no permitida: "
                    + estadoOperativo + " → " + nuevoEstado);
        }
        this.estadoOperativo = nuevoEstado;
    }

    private static boolean esTransicionPermitida(EstadoOperativo desde, EstadoOperativo hacia) {
        if (hacia == EstadoOperativo.FUERA_DE_SERVICIO) {
            return desde != EstadoOperativo.OCUPADO && desde != EstadoOperativo.FUERA_DE_SERVICIO;
        }
        return switch (desde) {
            case PREPARADO -> hacia == EstadoOperativo.OCUPADO;
            case OCUPADO -> hacia == EstadoOperativo.PENDIENTE_PREPARACION;
            case PENDIENTE_PREPARACION -> hacia == EstadoOperativo.EN_PREPARACION;
            case EN_PREPARACION -> hacia == EstadoOperativo.PREPARADO;
            case FUERA_DE_SERVICIO -> hacia == EstadoOperativo.PENDIENTE_PREPARACION;
        };
    }

    // ---------------------------------------------------------------- Bloqueos (RN-07, 7.3)

    /**
     * 7.3: no puede registrarse un bloqueo sobre noches que ya tengan reservas activas.
     * El servicio entrega las estancias de las reservas activas del apartamento.
     */
    public void registrarBloqueo(Bloqueo nuevoBloqueo, List<Estancia> estanciasReservadasActivas) {
        requerido(nuevoBloqueo, "El bloqueo es obligatorio");
        requerido(estanciasReservadasActivas, "Las estancias reservadas son obligatorias (puede ser una lista vacía)");
        if (!nuevoBloqueo.perteneceA(identificacion)) {
            throw new ReglaDominioException("El bloqueo no corresponde al apartamento " + identificacion.codigo());
        }
        boolean chocaConReserva = estanciasReservadasActivas.stream().anyMatch(nuevoBloqueo::seSolapaCon);
        if (chocaConReserva) {
            throw new ReglaDominioException("RN-01: No se puede bloquear el apartamento " + identificacion.codigo()
                    + ": hay reservas activas en esas noches");
        }
        bloqueos.add(nuevoBloqueo);
    }

    public void levantarBloqueo(String idBloqueo, LocalDate fecha) {
        Bloqueo bloqueo = bloqueos.stream()
                .filter(b -> b.getId().equals(idBloqueo))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("No existe el bloqueo " + idBloqueo));
        bloqueo.levantar(fecha);
    }

    /** RN-07: un bloqueo vigente sobre una noche la deja no disponible. */
    public boolean estaBloqueadoEn(LocalDate noche) {
        return bloqueos.stream().anyMatch(b -> b.afectaFecha(noche));
    }

    /** RN-07 aplicado a toda la estancia. */
    public boolean estaBloqueadoDurante(Estancia estancia) {
        return bloqueos.stream().anyMatch(b -> b.seSolapaCon(estancia));
    }

    /**
     * Parte de la disponibilidad (3.3) que el apartamento puede responder solo:
     * activo, capacidad suficiente y sin bloqueos. Las reservas solapadas (RN-01) y el tiempo de
     * preparación (RN-20) los agrega DisponibilidadApartamentoService.
     */
    public boolean puedeVenderse(Estancia estancia, int totalOcupantes) {
        return estaVigenteParaVenta() && admite(totalOcupantes) && !estaBloqueadoDurante(estancia);
    }

    // ---------------------------------------------------------------- Tarifas y activación (7.4)

    /** Define (o reemplaza) la tarifa del apartamento para una temporada. */
    public void definirTarifa(Tarifa tarifa) {
        requerido(tarifa, "La tarifa es obligatoria");
        tarifas.removeIf(t -> t.idTemporada().equals(tarifa.idTemporada()));
        tarifas.add(tarifa);
    }

    /** RN-05: tarifa vigente del apartamento en la temporada de esa noche. */
    public Tarifa tarifaPara(Temporada temporada) {
        requerido(temporada, "La temporada es obligatoria");
        return buscarTarifa(temporada)
                .orElseThrow(() -> new ReglaDominioException("7.4: El apartamento " + identificacion.codigo()
                        + " no tiene tarifa para la temporada " + temporada.getNombre()));
    }

    public boolean tieneTarifasCompletas(List<Temporada> temporadas) {
        requerido(temporadas, "Las temporadas son obligatorias");
        return !temporadas.isEmpty() && temporadas.stream().allMatch(t -> buscarTarifa(t).isPresent());
    }

    /** 7.4: el sistema impide activar un apartamento sin tarifa en todas las temporadas. */
    public void activar(List<Temporada> temporadasDelAlojamiento) {
        if (eliminado) {
            throw new ReglaDominioException("7.3: Un apartamento desactivado (eliminado lógicamente) no puede activarse");
        }
        if (!tieneTarifasCompletas(temporadasDelAlojamiento)) {
            throw new ReglaDominioException("7.4: El apartamento " + identificacion.codigo()
                    + " no tiene tarifa en todas las temporadas");
        }
        this.activo = true;
    }

    /**
     * 7.3: un apartamento puede retirarse de la venta solo si no tiene reservas activas ni futuras.
     * La eliminación es lógica (12.4) y el glosario la nombra "desactivar": queda marcado como eliminado,
     * los listados y búsquedas lo ignoran y las reservas históricas lo siguen referenciando.
     */
    public void desactivar(List<Estancia> estanciasActivasOFuturas) {
        validarSinReservasPendientes(estanciasActivasOFuturas);
        this.activo = false;
        this.eliminado = true;
    }

    public boolean estaVigenteParaVenta() {
        return activo && !eliminado;
    }

    // ---------------------------------------------------------------- Datos descriptivos e imágenes (7.3)

    public void actualizarDatos(String nombre, String descripcion, int dormitorios, Dotacion dotacion) {
        textoRequerido(nombre, "El nombre del apartamento es obligatorio");
        validarDormitorios(dormitorios);
        requerido(dotacion, "La dotación es obligatoria");
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.dormitorios = dormitorios;
        this.dotacion = dotacion;
    }

    public void agregarImagen(Imagen imagen) {
        requerido(imagen, "La imagen es obligatoria");
        List<Imagen> nuevas = new ArrayList<>(imagenes);
        nuevas.add(imagen);
        validarImagenes(nuevas);
        imagenes.add(imagen);
    }

    public void quitarImagen(String url) {
        List<Imagen> nuevas = new ArrayList<>(imagenes);
        if (!nuevas.removeIf(i -> i.url().equals(url))) {
            throw new ReglaDominioException("El apartamento no tiene la imagen " + url);
        }
        validarImagenes(nuevas);
        imagenes.clear();
        imagenes.addAll(nuevas);
    }

    /** Cambia cuál imagen es la destacada; siempre queda exactamente una principal. */
    public void destacarImagen(String url) {
        if (imagenes.stream().noneMatch(i -> i.url().equals(url))) {
            throw new ReglaDominioException("El apartamento no tiene la imagen " + url);
        }
        List<Imagen> nuevas = imagenes.stream()
                .map(i -> new Imagen(i.url(), i.url().equals(url)))
                .toList();
        imagenes.clear();
        imagenes.addAll(nuevas);
    }

    public Imagen imagenPrincipal() {
        return imagenes.stream().filter(Imagen::esPrincipal).findFirst()
                .orElseThrow(() -> new ReglaDominioException("El apartamento no tiene imagen principal"));
    }

    // ---------------------------------------------------------------- Getters

    public IdentificacionApartamento getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getDormitorios() { return dormitorios; }
    public Capacidad getCapacidad() { return capacidad; }
    public Dotacion getDotacion() { return dotacion; }
    public List<Imagen> getImagenes() { return Collections.unmodifiableList(imagenes); }
    public EstadoOperativo getEstadoOperativo() { return estadoOperativo; }
    public boolean isActivo() { return activo; }
    public boolean isEliminado() { return eliminado; }
    public List<Bloqueo> getBloqueos() { return Collections.unmodifiableList(bloqueos); }
    public List<Tarifa> getTarifas() { return Collections.unmodifiableList(tarifas); }

    // ---------------------------------------------------------------- Validaciones internas

    private Optional<Tarifa> buscarTarifa(Temporada temporada) {
        return tarifas.stream().filter(t -> t.idTemporada().equals(temporada.getId())).findFirst();
    }

    private void validarSinReservasPendientes(List<Estancia> estanciasActivasOFuturas) {
        requerido(estanciasActivasOFuturas, "Las estancias son obligatorias (puede ser una lista vacía)");
        if (!estanciasActivasOFuturas.isEmpty()) {
            throw new ReglaDominioException("7.3: El apartamento " + identificacion.codigo()
                    + " tiene reservas activas o futuras y no puede retirarse de la venta");
        }
    }

    private static void validarDormitorios(int dormitorios) {
        if (dormitorios < 1) {
            throw new ReglaDominioException("El apartamento debe tener al menos un dormitorio");
        }
    }

    /** 7.3: mínimo 1, máximo 10 imágenes, con exactamente una principal. */
    private static void validarImagenes(List<Imagen> imagenes) {
        if (imagenes == null || imagenes.size() < MINIMO_IMAGENES || imagenes.size() > MAXIMO_IMAGENES) {
            throw new ReglaDominioException("El apartamento debe tener entre " + MINIMO_IMAGENES + " y "
                    + MAXIMO_IMAGENES + " imágenes");
        }
        long principales = imagenes.stream().filter(Imagen::esPrincipal).count();
        if (principales != 1) {
            throw new ReglaDominioException("El apartamento debe tener exactamente una imagen principal");
        }
    }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Apartamento otro)) return false;
        return identificacion.equals(otro.identificacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identificacion);
    }
}
