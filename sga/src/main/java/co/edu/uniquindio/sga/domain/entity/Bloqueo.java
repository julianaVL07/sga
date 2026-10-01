package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Registro administrativo que impide vender un apartamento en un rango de fechas (glosario, 3.3).
 * Un bloqueo no cambia el estado operativo del apartamento, ni al revés.
 *
 * Reglas: RN-07 (un apartamento con bloqueo vigente sobre una noche no está disponible para esa noche).
 * La regla "no se puede bloquear sobre noches con reservas activas" (7.3) se valida en
 * Apartamento.registrarBloqueo, porque necesita las estancias reservadas.
 */
public class Bloqueo {

    private final String id;
    private final IdentificacionApartamento apartamento;
    private final RangoFechas rango;
    private final String motivo;
    private boolean vigente;
    private LocalDate fechaLevantamiento;

    private Bloqueo(String id, IdentificacionApartamento apartamento, RangoFechas rango, String motivo,
                    boolean vigente, LocalDate fechaLevantamiento) {
        this.id = id;
        this.apartamento = apartamento;
        this.rango = rango;
        this.motivo = motivo;
        this.vigente = vigente;
        this.fechaLevantamiento = fechaLevantamiento;
    }

    /** Registra un bloqueo nuevo y vigente. */
    public static Bloqueo registrar(String id, IdentificacionApartamento apartamento, RangoFechas rango, String motivo) {
        textoRequerido(id, "El id del bloqueo es obligatorio");
        requerido(apartamento, "El bloqueo debe indicar el apartamento");
        requerido(rango, "El bloqueo debe tener un rango de fechas");
        textoRequerido(motivo, "El bloqueo debe registrar un motivo");
        if (rango.noches() < 1) {
            throw new ReglaDominioException("El rango del bloqueo debe cubrir al menos una noche");
        }
        return new Bloqueo(id, apartamento, rango, motivo.trim(), true, null);
    }

    /** Reconstruye un bloqueo desde persistencia, sin reaplicar reglas de creación. */
    public static Bloqueo reconstruir(String id, IdentificacionApartamento apartamento, RangoFechas rango, String motivo,
                                      boolean vigente, LocalDate fechaLevantamiento) {
        return new Bloqueo(id, apartamento, rango, motivo, vigente, fechaLevantamiento);
    }

    /** RN-07: un bloqueo vigente hace no disponible la noche indicada. */
    public boolean afectaFecha(LocalDate noche) {
        requerido(noche, "La fecha es obligatoria");
        return vigente && rango.contiene(noche);
    }

    /** RN-07: indica si alguna noche de la estancia cae dentro del bloqueo vigente. */
    public boolean seSolapaCon(Estancia estancia) {
        requerido(estancia, "La estancia es obligatoria");
        return vigente && estancia.fechaEntrada()
                .datesUntil(estancia.fechaSalida())
                .anyMatch(rango::contiene);
    }

    public boolean perteneceA(IdentificacionApartamento identificacion) {
        return apartamento.equals(identificacion);
    }

    /** Eliminación lógica: el bloqueo deja de afectar la disponibilidad pero se conserva su historia. */
    public void levantar(LocalDate fecha) {
        requerido(fecha, "La fecha de levantamiento es obligatoria");
        if (!vigente) {
            throw new ReglaDominioException("RN-07: El bloqueo " + id + " ya fue levantado");
        }
        this.vigente = false;
        this.fechaLevantamiento = fecha;
    }

    public String getId() { return id; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public RangoFechas getRango() { return rango; }
    public String getMotivo() { return motivo; }
    public boolean isVigente() { return vigente; }
    public LocalDate getFechaLevantamiento() { return fechaLevantamiento; }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bloqueo otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
