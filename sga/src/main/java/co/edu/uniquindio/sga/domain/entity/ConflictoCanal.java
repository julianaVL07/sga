package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reserva externa rechazada por colisionar con una reserva vigente (glosario, 7.8).
 *
 * Reglas: RN-18 (la reserva externa en conflicto se rechaza y se registra; nunca sobrescribe la vigente).
 * Esta entidad es el "se registra": queda pendiente hasta que el administrador la revisa.
 * Detectar el conflicto es tarea de ConciliacionCanalExternoService.
 */
public class ConflictoCanal {

    private final String id;
    private final CanalOrigen canalOrigen;
    private final String identificadorExterno;
    private final String detalle;
    private final LocalDate fecha;
    private boolean revisado;
    private String notasAdministrador;
    private LocalDate fechaRevision;

    private ConflictoCanal(String id, CanalOrigen canalOrigen, String identificadorExterno, String detalle,
                           LocalDate fecha, boolean revisado, String notasAdministrador, LocalDate fechaRevision) {
        this.id = id;
        this.canalOrigen = canalOrigen;
        this.identificadorExterno = identificadorExterno;
        this.detalle = detalle;
        this.fecha = fecha;
        this.revisado = revisado;
        this.notasAdministrador = notasAdministrador;
        this.fechaRevision = fechaRevision;
    }

    /** RN-18: registra el conflicto pendiente de revisión. Solo aplica a reservas de canal externo. */
    public static ConflictoCanal registrar(String id, CanalOrigen canalOrigen, String identificadorExterno,
                                           String detalle, LocalDate fecha) {
        textoRequerido(id, "El id del conflicto es obligatorio");
        requerido(canalOrigen, "El canal de origen es obligatorio");
        if (!canalOrigen.exigeIdentificadorExterno()) {
            throw new ReglaDominioException("RN-18: Solo una reserva de canal externo genera un conflicto de canal");
        }
        textoRequerido(identificadorExterno, "El conflicto debe registrar el identificador externo (RN-19)");
        textoRequerido(detalle, "El conflicto debe describir con qué colisionó");
        requerido(fecha, "La fecha del conflicto es obligatoria");
        return new ConflictoCanal(id, canalOrigen, identificadorExterno.trim(), detalle.trim(), fecha,
                false, null, null);
    }

    public static ConflictoCanal reconstruir(String id, CanalOrigen canalOrigen, String identificadorExterno,
                                             String detalle, LocalDate fecha, boolean revisado,
                                             String notasAdministrador, LocalDate fechaRevision) {
        return new ConflictoCanal(id, canalOrigen, identificadorExterno, detalle, fecha, revisado,
                notasAdministrador, fechaRevision);
    }

    /** 7.1 Administrador: resolver los conflictos registrados. Un conflicto se revisa una sola vez. */
    public void marcarComoRevisado(String notasAdministrador, LocalDate fechaRevision) {
        textoRequerido(notasAdministrador, "La revisión debe registrar las notas del administrador");
        requerido(fechaRevision, "La fecha de revisión es obligatoria");
        if (revisado) {
            throw new ReglaDominioException("RN-18: El conflicto " + id + " ya fue revisado");
        }
        if (fechaRevision.isBefore(fecha)) {
            throw new ReglaDominioException("La revisión no puede ser anterior al conflicto");
        }
        this.revisado = true;
        this.notasAdministrador = notasAdministrador.trim();
        this.fechaRevision = fechaRevision;
    }

    public boolean estaPendiente() {
        return !revisado;
    }

    public String getId() { return id; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public String getDetalle() { return detalle; }
    public LocalDate getFecha() { return fecha; }
    public boolean isRevisado() { return revisado; }
    public String getNotasAdministrador() { return notasAdministrador; }
    public LocalDate getFechaRevision() { return fechaRevision; }

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
