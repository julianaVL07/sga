package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Periodo del calendario con tarifas propias (glosario).
 * La temporada base cubre todas las fechas no asignadas a otra temporada y es obligatoria (F-05, 7.4).
 *
 * Reglas: RN-05 (define qué tarifa aplica a cada noche), RP-01 (su tipo indica si exige estancia mínima
 * de fin de semana). Que las temporadas no se solapen entre sí se valida en Alojamiento.agregarTemporada.
 */
public class Temporada {

    private final String id;
    private String nombre;
    private final TipoTemporada tipo;
    private final List<RangoFechas> rangos;

    private Temporada(String id, String nombre, TipoTemporada tipo, List<RangoFechas> rangos) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.rangos = new ArrayList<>(rangos);
    }

    /** Crea una temporada con fechas propias (MEDIA o ALTA). */
    public static Temporada crear(String id, String nombre, TipoTemporada tipo, List<RangoFechas> rangos) {
        textoRequerido(id, "El id de la temporada es obligatorio");
        textoRequerido(nombre, "El nombre de la temporada es obligatorio");
        requerido(tipo, "El tipo de temporada es obligatorio");
        if (tipo == TipoTemporada.BASE) {
            throw new ReglaDominioException("La temporada base se crea con crearBase: cubre las fechas no asignadas");
        }
        if (rangos == null || rangos.isEmpty()) {
            throw new ReglaDominioException("Una temporada no base debe tener al menos un rango de fechas");
        }
        Temporada temporada = new Temporada(id, nombre.trim(), tipo, List.of());
        rangos.forEach(temporada::agregarRango);
        return temporada;
    }

    /** Crea la temporada base obligatoria: no tiene rangos porque cubre el resto del calendario. */
    public static Temporada crearBase(String id, String nombre) {
        textoRequerido(id, "El id de la temporada es obligatorio");
        textoRequerido(nombre, "El nombre de la temporada es obligatorio");
        return new Temporada(id, nombre.trim(), TipoTemporada.BASE, List.of());
    }

    public static Temporada reconstruir(String id, String nombre, TipoTemporada tipo, List<RangoFechas> rangos) {
        return new Temporada(id, nombre, tipo, rangos == null ? List.of() : rangos);
    }

    public boolean esBase() {
        return tipo == TipoTemporada.BASE;
    }

    /**
     * Indica si la fecha pertenece a la temporada. La base incluye cualquier fecha; la prioridad de las
     * temporadas con fechas propias sobre la base la resuelve Alojamiento.obtenerTemporadaPara.
     */
    public boolean incluye(LocalDate fecha) {
        requerido(fecha, "La fecha es obligatoria");
        return esBase() || rangos.stream().anyMatch(r -> r.contiene(fecha));
    }

    /** 7.4: las temporadas no pueden solaparse entre sí. La base nunca se solapa porque solo cubre huecos. */
    public boolean seSolapaCon(Temporada otra) {
        requerido(otra, "La temporada a comparar es obligatoria");
        if (this.esBase() || otra.esBase()) {
            return false;
        }
        return rangos.stream().anyMatch(propio -> otra.rangos.stream().anyMatch(propio::seSolapaCon));
    }

    /** RP-01: solo las temporadas media y alta exigen estancia mínima de fin de semana. */
    public boolean exigeEstanciaMinimaFinDeSemana() {
        return tipo.exigeEstanciaMinimaFinDeSemana();
    }

    public void agregarRango(RangoFechas rango) {
        requerido(rango, "El rango es obligatorio");
        if (esBase()) {
            throw new ReglaDominioException("F-05: La temporada base no tiene rangos: cubre las fechas no asignadas");
        }
        if (rango.noches() < 1) {
            throw new ReglaDominioException("El rango de la temporada debe cubrir al menos una noche");
        }
        if (rangos.stream().anyMatch(r -> r.seSolapaCon(rango))) {
            throw new ReglaDominioException("7.4: El rango se solapa con otro rango de la temporada " + nombre);
        }
        rangos.add(rango);
    }

    public void renombrar(String nuevoNombre) {
        textoRequerido(nuevoNombre, "El nombre de la temporada es obligatorio");
        this.nombre = nuevoNombre.trim();
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoTemporada getTipo() { return tipo; }
    public List<RangoFechas> getRangos() { return Collections.unmodifiableList(rangos); }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Temporada otra)) return false;
        return id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
