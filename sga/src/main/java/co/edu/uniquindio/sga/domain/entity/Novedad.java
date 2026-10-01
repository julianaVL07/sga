package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reporte de un daño, faltante o situación en un apartamento (glosario, 7.6).
 * Se registra con fecha, autor, descripción y gravedad. La gravedad puede venir sugerida por el modelo
 * de IA (L-21) o elegirse manualmente; en ambos casos se puede reclasificar.
 */
public class Novedad {

    private final String id;
    private final IdentificacionApartamento apartamento;
    private final LocalDate fecha;
    private final String autor;
    private final String descripcion;
    private GravedadNovedad gravedad;
    private boolean atendida;
    private LocalDate fechaAtencion;

    private Novedad(String id, IdentificacionApartamento apartamento, LocalDate fecha, String autor,
                    String descripcion, GravedadNovedad gravedad, boolean atendida, LocalDate fechaAtencion) {
        this.id = id;
        this.apartamento = apartamento;
        this.fecha = fecha;
        this.autor = autor;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
        this.atendida = atendida;
        this.fechaAtencion = fechaAtencion;
    }

    /** 7.6: registrar una novedad con fecha, autor, descripción y gravedad. */
    public static Novedad registrar(String id, IdentificacionApartamento apartamento, String autor, String descripcion,
                                    GravedadNovedad gravedad, LocalDate fecha) {
        textoRequerido(id, "El id de la novedad es obligatorio");
        requerido(apartamento, "La novedad debe indicar el apartamento");
        textoRequerido(autor, "La novedad debe indicar su autor");
        textoRequerido(descripcion, "La novedad debe tener una descripción");
        requerido(gravedad, "La novedad debe tener una gravedad");
        requerido(fecha, "La novedad debe tener fecha");
        return new Novedad(id, apartamento, fecha, autor.trim(), descripcion.trim(), gravedad, false, null);
    }

    public static Novedad reconstruir(String id, IdentificacionApartamento apartamento, LocalDate fecha, String autor,
                                      String descripcion, GravedadNovedad gravedad, boolean atendida,
                                      LocalDate fechaAtencion) {
        return new Novedad(id, apartamento, fecha, autor, descripcion, gravedad, atendida, fechaAtencion);
    }

    /** Una novedad de gravedad ALTA sin atender requiere atención inmediata (y puede ameritar un bloqueo). */
    public boolean requiereAtencionInmediata() {
        return !atendida && gravedad.requiereAtencionInmediata();
    }

    /** Permite corregir la gravedad sugerida por la IA o elegida manualmente. */
    public void reclasificarGravedad(GravedadNovedad nuevaGravedad) {
        requerido(nuevaGravedad, "La gravedad es obligatoria");
        validarPendiente();
        this.gravedad = nuevaGravedad;
    }

    public void marcarComoAtendida(LocalDate fecha) {
        requerido(fecha, "La fecha de atención es obligatoria");
        validarPendiente();
        if (fecha.isBefore(this.fecha)) {
            throw new ReglaDominioException("La atención no puede ser anterior al registro de la novedad");
        }
        this.atendida = true;
        this.fechaAtencion = fecha;
    }

    public boolean perteneceA(IdentificacionApartamento identificacion) {
        return apartamento.equals(identificacion);
    }

    private void validarPendiente() {
        if (atendida) {
            throw new ReglaDominioException("7.6: La novedad " + id + " ya fue atendida");
        }
    }

    public String getId() { return id; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public LocalDate getFecha() { return fecha; }
    public String getAutor() { return autor; }
    public String getDescripcion() { return descripcion; }
    public GravedadNovedad getGravedad() { return gravedad; }
    public boolean isAtendida() { return atendida; }
    public LocalDate getFechaAtencion() { return fechaAtencion; }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Novedad otra)) return false;
        return id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
