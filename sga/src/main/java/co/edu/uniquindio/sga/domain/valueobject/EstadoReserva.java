package co.edu.uniquindio.sga.domain.valueobject;

public enum EstadoReserva {
    PENDIENTE(true),
    CONFIRMADA(true),
    EN_CURSO(true),
    FINALIZADA(false),
    CANCELADA(false),
    NO_SHOW(false);

    private final boolean activa;

    EstadoReserva(boolean activa) {
        this.activa = activa;
    }

    //Consulta estado activo PENDIENTE CONFIRMADA EN_CURSO
    public boolean esActiva() {
        return activa;
    }

    //Consulta estado terminal FINALIZADA CANCELADA NO_SHOW
    public boolean esTerminal(){
        return !activa;
    }

    public boolean retieneDisponibilidad(){
        return activa;
    }
}
