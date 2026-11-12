package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoConflictoCanal;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reserva externa rechazada por colisionar con una reserva vigente (glosario, 7.8).
 *
 * Reglas: RN-18 (la reserva externa en conflicto se rechaza y se registra; nunca sobrescribe la vigente).
 * Esta entidad es el "se registra": queda PENDIENTE hasta que el administrador la resuelve (7.1), dejando el
 * motivo y quién la resolvió. Resolverla no toca ninguna reserva: la vigente sigue igual.
 * Detectar el conflicto es tarea de ConciliacionCanalExternoService.
 */
public class ConflictoCanal {

    private final String id;
    private final CanalOrigen canalOrigen;
    private final String nombreCanal;
    private final String identificadorExterno;
    private final String detalle;
    private final LocalDate fecha;
    private boolean resuelto;
    private String motivoResolucion;
    private String resueltoPor;
    private LocalDate fechaResolucion;

    private ConflictoCanal(String id, CanalOrigen canalOrigen, String nombreCanal, String identificadorExterno,
                           String detalle, LocalDate fecha, boolean resuelto, String motivoResolucion,
                           String resueltoPor, LocalDate fechaResolucion) {
        this.id = id;
        this.canalOrigen = canalOrigen;
        this.nombreCanal = nombreCanal;
        this.identificadorExterno = identificadorExterno;
        this.detalle = detalle;
        this.fecha = fecha;
        this.resuelto = resuelto;
        this.motivoResolucion = motivoResolucion;
        this.resueltoPor = resueltoPor;
        this.fechaResolucion = fechaResolucion;
    }

    /** RN-18: registra el conflicto pendiente de revisión. Solo aplica a reservas de canal externo. */
    public static ConflictoCanal registrar(String id, CanalOrigen canalOrigen, String nombreCanal,
                                           String identificadorExterno, String detalle, LocalDate fecha) {
        textoRequerido(id, "El id del conflicto es obligatorio");
        requerido(canalOrigen, "El canal de origen es obligatorio");
        if (!canalOrigen.exigeIdentificadorExterno()) {
            throw new ReglaDominioException("RN-18: Solo una reserva de canal externo genera un conflicto de canal");
        }
        textoRequerido(nombreCanal, "El conflicto debe registrar el canal que envió la reserva");
        textoRequerido(identificadorExterno, "El conflicto debe registrar el identificador externo (RN-19)");
        textoRequerido(detalle, "El conflicto debe describir con qué colisionó");
        requerido(fecha, "La fecha del conflicto es obligatoria");
        return new ConflictoCanal(id, canalOrigen, nombreCanal.trim(), identificadorExterno.trim(), detalle.trim(),
                fecha, false, null, null, null);
    }

    public static ConflictoCanal reconstruir(String id, CanalOrigen canalOrigen, String nombreCanal,
                                             String identificadorExterno, String detalle, LocalDate fecha,
                                             boolean resuelto, String motivoResolucion, String resueltoPor,
                                             LocalDate fechaResolucion) {
        return new ConflictoCanal(id, canalOrigen, nombreCanal, identificadorExterno, detalle, fecha, resuelto,
                motivoResolucion, resueltoPor, fechaResolucion);
    }

    /**
     * 7.1 Administrador: resolver un conflicto registrado. Se resuelve una sola vez, con el motivo y quién lo
     * resolvió. Solo cambia el conflicto: la reserva vigente nunca se toca (RN-18).
     */
    public void resolver(String motivo, String administrador, LocalDate fechaResolucion) {
        textoRequerido(motivo, "Para resolver el conflicto hay que escribir el motivo");
        textoRequerido(administrador, "Hay que registrar quién resuelve el conflicto");
        requerido(fechaResolucion, "La fecha de resolución es obligatoria");
        if (resuelto) {
            throw new ReglaDominioException("RN-18: El conflicto " + id + " ya fue resuelto");
        }
        if (fechaResolucion.isBefore(fecha)) {
            throw new ReglaDominioException("La resolución no puede ser anterior al conflicto");
        }
        this.resuelto = true;
        this.motivoResolucion = motivo.trim();
        this.resueltoPor = administrador.trim();
        this.fechaResolucion = fechaResolucion;
    }

    public boolean estaPendiente() {
        return !resuelto;
    }

    public EstadoConflictoCanal estado() {
        return resuelto ? EstadoConflictoCanal.RESUELTO : EstadoConflictoCanal.PENDIENTE;
    }

    public String getId() { return id; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public String getNombreCanal() { return nombreCanal; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public String getDetalle() { return detalle; }
    public LocalDate getFecha() { return fecha; }
    public boolean isResuelto() { return resuelto; }
    public String getMotivoResolucion() { return motivoResolucion; }
    public String getResueltoPor() { return resueltoPor; }
    public LocalDate getFechaResolucion() { return fechaResolucion; }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConflictoCanal otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
