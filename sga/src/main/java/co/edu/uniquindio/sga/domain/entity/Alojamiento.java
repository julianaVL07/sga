package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinima;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroLlegadaNocturna;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El negocio completo: conjunto de apartamentos bajo una misma administración (F-13: es único).
 * Guarda los parámetros de la sección 6 para que ningún valor quede quemado en el código.
 *
 * Reglas: F-05 y 7.4 (temporada base obligatoria, temporadas sin solaparse), RP-01 (estancia mínima
 * de fin de semana en temporada media o alta), RP-03 (qué es una llegada nocturna),
 * 3.3 y RN-20 (si el tiempo de preparación permite entrada el mismo día de una salida),
 * L-03 y L-04 (cantidad e identificación única de apartamentos).
 */
public class Alojamiento {

    private static final int MINIMO_APARTAMENTOS = 5;
    private static final int MAXIMO_APARTAMENTOS = 15;

    private final String id;
    private String nombre;
    private String descripcion;
    private String ciudad;
    private String direccion;
    private Coordenada ubicacion;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private String normaConvivencia;

    // Parámetros configurables (sección 6 y 7.3)
    private UmbralEdadFacturable umbralEdadFacturable;
    private TiempoPreparacion tiempoPreparacion;
    private PlazoConfirmacion plazoConfirmacion;
    private LocalTime horaLimiteNoShow;
    private LocalTime horaInicioLlegadaNocturna;
    private EstanciaMinima estanciaMinima;
    private boolean exigeAnticipo;
    private int porcentajeAnticipo;
    private CargoAseoMascota politicaMascotas;

    /** Apartamento es otro agregado: aquí solo se guarda su identificación (L-03, L-04). */
    private final List<IdentificacionApartamento> apartamentos;
    private final List<Temporada> temporadas;
    private final List<ServicioAdicional> serviciosAdicionales;

    private Alojamiento(String id, String nombre, String descripcion, String ciudad, String direccion,
                        Coordenada ubicacion, LocalTime horaEntrada, LocalTime horaSalida, String normaConvivencia,
                        UmbralEdadFacturable umbralEdadFacturable, TiempoPreparacion tiempoPreparacion,
                        PlazoConfirmacion plazoConfirmacion, LocalTime horaLimiteNoShow,
                        LocalTime horaInicioLlegadaNocturna, EstanciaMinima estanciaMinima, boolean exigeAnticipo,
                        int porcentajeAnticipo, CargoAseoMascota politicaMascotas, List<IdentificacionApartamento> apartamentos,
                        List<Temporada> temporadas,
                        List<ServicioAdicional> serviciosAdicionales) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.normaConvivencia = normaConvivencia;
        this.umbralEdadFacturable = umbralEdadFacturable;
        this.tiempoPreparacion = tiempoPreparacion;
        this.plazoConfirmacion = plazoConfirmacion;
        this.horaLimiteNoShow = horaLimiteNoShow;
        this.horaInicioLlegadaNocturna = horaInicioLlegadaNocturna;
        this.estanciaMinima = estanciaMinima;
        this.exigeAnticipo = exigeAnticipo;
        this.porcentajeAnticipo = porcentajeAnticipo;
        this.politicaMascotas = politicaMascotas;
        this.apartamentos = new ArrayList<>(apartamentos);
        this.temporadas = new ArrayList<>(temporadas);
        this.serviciosAdicionales = new ArrayList<>(serviciosAdicionales);
    }

    /** Crea el alojamiento con sus datos de identidad (A.1) y horario (L-12). Los parámetros se configuran después. */
    public static Alojamiento crear(String id, String nombre, String descripcion, String ciudad, String direccion,
                                    Coordenada ubicacion, LocalTime horaEntrada, LocalTime horaSalida,
                                    String normaConvivencia) {
        textoRequerido(id, "El id del alojamiento es obligatorio");
        textoRequerido(nombre, "El nombre del alojamiento es obligatorio (L-01)");
        textoRequerido(ciudad, "La ciudad del alojamiento es obligatoria (L-02)");
        requerido(ubicacion, "La ubicación exacta (latitud y longitud) es obligatoria (7.3)");
        validarHorario(horaEntrada, horaSalida);
        return new Alojamiento(id, nombre.trim(), descripcion, ciudad.trim(), direccion, ubicacion, horaEntrada,
                horaSalida, normaConvivencia, null, null, null, null, null, null, false, 0,
                CargoAseoMascota.noSeAceptanMascotas(), List.of(), List.of(), List.of());
    }

    public static Alojamiento reconstruir(String id, String nombre, String descripcion, String ciudad,
                                          String direccion, Coordenada ubicacion, LocalTime horaEntrada,
                                          LocalTime horaSalida, String normaConvivencia,
                                          UmbralEdadFacturable umbralEdadFacturable,
                                          TiempoPreparacion tiempoPreparacion, PlazoConfirmacion plazoConfirmacion,
                                          LocalTime horaLimiteNoShow, LocalTime horaInicioLlegadaNocturna,
                                          EstanciaMinima estanciaMinima, boolean exigeAnticipo,
                                          int porcentajeAnticipo, CargoAseoMascota politicaMascotas,
                                          List<IdentificacionApartamento> apartamentos,
                                          List<Temporada> temporadas, List<ServicioAdicional> serviciosAdicionales) {
        return new Alojamiento(id, nombre, descripcion, ciudad, direccion, ubicacion, horaEntrada, horaSalida,
                normaConvivencia, umbralEdadFacturable, tiempoPreparacion, plazoConfirmacion, horaLimiteNoShow,
                horaInicioLlegadaNocturna, estanciaMinima, exigeAnticipo, porcentajeAnticipo,
                politicaMascotas == null ? CargoAseoMascota.noSeAceptanMascotas() : politicaMascotas,
                apartamentos == null ? List.of() : apartamentos,
                temporadas == null ? List.of() : temporadas,
                serviciosAdicionales == null ? List.of() : serviciosAdicionales);
    }

    // ---------------------------------------------------------------- Datos y configuración

    public void actualizarDatos(String nombre, String descripcion, String ciudad, String direccion,
                                Coordenada ubicacion, String normaConvivencia) {
        textoRequerido(nombre, "El nombre del alojamiento es obligatorio");
        textoRequerido(ciudad, "La ciudad del alojamiento es obligatoria");
        requerido(ubicacion, "La ubicación es obligatoria");
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.ciudad = ciudad.trim();
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.normaConvivencia = normaConvivencia;
    }

    /** L-12: las horas de entrada y salida deben existir. */
    public void definirHorario(LocalTime horaEntrada, LocalTime horaSalida) {
        validarHorario(horaEntrada, horaSalida);
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
    }

    /**
     * Configura los parámetros de la sección 6 (L-09, L-11, L-13, L-14, L-15, RP-01, RP-03).
     * Cambiarlos no exige recompilar: se guardan como datos del alojamiento.
     */
    public void configurarParametros(UmbralEdadFacturable umbralEdadFacturable, TiempoPreparacion tiempoPreparacion,
                                     PlazoConfirmacion plazoConfirmacion, LocalTime horaLimiteNoShow,
                                     LocalTime horaInicioLlegadaNocturna, EstanciaMinima estanciaMinima,
                                     boolean exigeAnticipo, int porcentajeAnticipo) {
        requerido(umbralEdadFacturable, "El umbral de edad facturable es obligatorio (L-09)");
        requerido(tiempoPreparacion, "El tiempo de preparación es obligatorio (L-13)");
        requerido(plazoConfirmacion, "El plazo de confirmación es obligatorio (L-14)");
        requerido(horaLimiteNoShow, "La hora límite de no-show es obligatoria (L-15)");
        requerido(horaInicioLlegadaNocturna, "La hora desde la que una llegada es nocturna es obligatoria (RP-03)");
        requerido(estanciaMinima, "La estancia mínima de fin de semana es obligatoria (RP-01)");
        if (exigeAnticipo && (porcentajeAnticipo <= 0 || porcentajeAnticipo > 100)) {
            throw new ReglaDominioException("Si se exige anticipo, su porcentaje debe estar entre 1 y 100 (L-11)");
        }
        this.umbralEdadFacturable = umbralEdadFacturable;
        this.tiempoPreparacion = tiempoPreparacion;
        this.plazoConfirmacion = plazoConfirmacion;
        this.horaLimiteNoShow = horaLimiteNoShow;
        this.horaInicioLlegadaNocturna = horaInicioLlegadaNocturna;
        this.estanciaMinima = estanciaMinima;
        this.exigeAnticipo = exigeAnticipo;
        this.porcentajeAnticipo = exigeAnticipo ? porcentajeAnticipo : 0;
    }

    /** L-16: define si se aceptan mascotas, cuántas, de qué peso y el cargo de aseo por estancia. */
    public void definirPoliticaMascotas(CargoAseoMascota politica) {
        requerido(politica, "L-16: la política de mascotas es obligatoria");
        this.politicaMascotas = politica;
    }

    /** Anexo A: mínimo 5 apartamentos, temporada base, al menos 2 temporadas adicionales y parámetros definidos. */
    public boolean estaConfiguradoCompletamente() {
        boolean parametrosDefinidos = umbralEdadFacturable != null && tiempoPreparacion != null
                && plazoConfirmacion != null && horaLimiteNoShow != null && horaInicioLlegadaNocturna != null
                && estanciaMinima != null;
        long temporadasNoBase = temporadas.stream().filter(t -> !t.esBase()).count();
        return parametrosDefinidos
                && horaEntrada != null && horaSalida != null
                && apartamentos.size() >= MINIMO_APARTAMENTOS
                && buscarTemporadaBase().isPresent()
                && temporadasNoBase >= 2;
    }

    // ---------------------------------------------------------------- Apartamentos (L-03, L-04)

    public void agregarApartamento(Apartamento apartamento) {
        requerido(apartamento, "El apartamento es obligatorio");
        if (apartamentos.size() >= MAXIMO_APARTAMENTOS) {
            throw new ReglaDominioException("L-03: El alojamiento admite máximo " + MAXIMO_APARTAMENTOS
                    + " apartamentos");
        }
        if (contieneApartamento(apartamento.getIdentificacion())) {
            throw new ReglaDominioException("L-04: Ya existe un apartamento con la identificación "
                    + apartamento.getIdentificacion().codigo());
        }
        apartamentos.add(apartamento.getIdentificacion());
    }

    public boolean contieneApartamento(IdentificacionApartamento identificacion) {
        return apartamentos.contains(identificacion);
    }

    // ---------------------------------------------------------------- Temporadas (F-05, 7.4, RN-05)

    /** 7.4: solo una temporada base y las demás no pueden solaparse entre sí. */
    public void agregarTemporada(Temporada nuevaTemporada) {
        requerido(nuevaTemporada, "La temporada es obligatoria");
        if (temporadas.contains(nuevaTemporada)) {
            throw new ReglaDominioException("La temporada " + nuevaTemporada.getId() + " ya está registrada");
        }
        if (nuevaTemporada.esBase() && buscarTemporadaBase().isPresent()) {
            throw new ReglaDominioException("F-05: El alojamiento ya tiene una temporada base");
        }
        temporadas.stream()
                .filter(nuevaTemporada::seSolapaCon)
                .findFirst()
                .ifPresent(existente -> {
                    throw new ReglaDominioException("7.4: La temporada " + nuevaTemporada.getNombre()
                            + " se solapa con " + existente.getNombre());
                });
        temporadas.add(nuevaTemporada);
    }

    /**
     * RN-05: temporada que aplica a una noche. Una temporada con fechas propias tiene prioridad;
     * si ninguna la cubre, aplica la base (7.4: ninguna fecha reservable queda sin temporada).
     */
    public Temporada obtenerTemporadaPara(LocalDate fecha) {
        requerido(fecha, "La fecha es obligatoria");
        return temporadas.stream()
                .filter(t -> !t.esBase() && t.incluye(fecha))
                .findFirst()
                .or(this::buscarTemporadaBase)
                .orElseThrow(() -> new ReglaDominioException("F-05: El alojamiento no tiene temporada base configurada"));
    }

    /**
     * RP-01: toda reserva que toque al menos una noche de viernes o sábado en temporada media o alta
     * debe tener la estancia mínima configurada (2 noches en el Anexo A).
     */
    public void validarEstanciaMinima(Estancia estancia) {
        requerido(estancia, "La estancia es obligatoria");
        requerido(estanciaMinima, "RP-01: la estancia mínima no está configurada");
        boolean tocaFinDeSemanaAltaOMedia = estancia.fechaEntrada()
                .datesUntil(estancia.fechaSalida())
                .anyMatch(noche -> esViernesOSabado(noche) && obtenerTemporadaPara(noche).exigeEstanciaMinimaFinDeSemana());
        if (!estanciaMinima.cumpleRegla(estancia.noches(), tocaFinDeSemanaAltaOMedia)) {
            throw new ReglaDominioException("RP-01: Una estancia que incluye viernes o sábado en temporada media "
                    + "o alta debe tener mínimo " + estanciaMinima.nochesMinimas() + " noches");
        }
    }

    private static boolean esViernesOSabado(LocalDate noche) {
        DayOfWeek dia = noche.getDayOfWeek();
        return dia == DayOfWeek.FRIDAY || dia == DayOfWeek.SATURDAY;
    }

    // ---------------------------------------------------------------- Operación (RP-03, RN-20)

    /** RP-03: una llegada es nocturna si la hora estimada es igual o posterior a la hora configurada (20:00). */
    public boolean esLlegadaNocturna(LocalTime horaEstimada) {
        requerido(horaEstimada, "La hora estimada de llegada es obligatoria");
        return new RegistroLlegadaNocturna(horaEstimada).generaRecargo(horaInicioLlegadaNocturna);
    }

    /**
     * 3.3 y RN-20: si el tiempo de preparación excede la ventana entre la hora de salida y la de entrada,
     * el apartamento no puede recibir una entrada el mismo día de una salida.
     */
    public boolean permiteEntradaElMismoDiaDeUnaSalida() {
        requerido(tiempoPreparacion, "RN-20: el tiempo de preparación no está configurado");
        return !tiempoPreparacion.excedeHora(horaSalida, horaEntrada);
    }

    // ---------------------------------------------------------------- Servicios adicionales (L-17)

    public void agregarServicioAdicional(ServicioAdicional servicio) {
        requerido(servicio, "El servicio adicional es obligatorio");
        if (serviciosAdicionales.contains(servicio)) {
            throw new ReglaDominioException("El servicio " + servicio.getId() + " ya está registrado");
        }
        serviciosAdicionales.add(servicio);
    }

    public Optional<ServicioAdicional> buscarServicioAdicional(String idServicio) {
        return serviciosAdicionales.stream().filter(s -> s.getId().equals(idServicio)).findFirst();
    }

    // ---------------------------------------------------------------- Getters

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCiudad() { return ciudad; }
    public String getDireccion() { return direccion; }
    public Coordenada getUbicacion() { return ubicacion; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public String getNormaConvivencia() { return normaConvivencia; }
    public UmbralEdadFacturable getUmbralEdadFacturable() { return umbralEdadFacturable; }
    public TiempoPreparacion getTiempoPreparacion() { return tiempoPreparacion; }
    public PlazoConfirmacion getPlazoConfirmacion() { return plazoConfirmacion; }
    public LocalTime getHoraLimiteNoShow() { return horaLimiteNoShow; }
    public LocalTime getHoraInicioLlegadaNocturna() { return horaInicioLlegadaNocturna; }
    public EstanciaMinima getEstanciaMinima() { return estanciaMinima; }
    public boolean isExigeAnticipo() { return exigeAnticipo; }
    public int getPorcentajeAnticipo() { return porcentajeAnticipo; }
    public CargoAseoMascota getPoliticaMascotas() { return politicaMascotas; }
    public List<IdentificacionApartamento> getApartamentos() { return Collections.unmodifiableList(apartamentos); }
    public List<Temporada> getTemporadas() { return Collections.unmodifiableList(temporadas); }
    public List<ServicioAdicional> getServiciosAdicionales() { return Collections.unmodifiableList(serviciosAdicionales); }

    private Optional<Temporada> buscarTemporadaBase() {
        return temporadas.stream().filter(Temporada::esBase).findFirst();
    }

    private static void validarHorario(LocalTime horaEntrada, LocalTime horaSalida) {
        requerido(horaEntrada, "La hora de entrada es obligatoria (L-12)");
        requerido(horaSalida, "La hora de salida es obligatoria (L-12)");
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
        if (!(o instanceof Alojamiento otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
