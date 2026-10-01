package co.edu.uniquindio.sga.domain.valueobject;

/** Gravedad de una novedad (7.6), decisión del equipo; la IA puede sugerirla (L-21). */
public enum GravedadNovedad {
    BAJA,
    MEDIA,
    ALTA;

    public boolean requiereAtencionInmediata() {
        return this == ALTA;
    }
}
