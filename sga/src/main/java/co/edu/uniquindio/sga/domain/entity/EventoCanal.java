package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entrada de la bitácora de eventos intercambiados con los canales (7.8).
 * Una vez auditado el evento no cambia: la bitácora es de solo escritura.
 *
 * Reglas: RN-19 (canal + identificador externo es único: el mismo mensaje recibido dos veces no crea
 * dos reservas). ConciliacionCanalExternoService consulta los eventos con esDuplicado(...) antes de procesar.
 */
public class EventoCanal {

    private final String id;
    private final TipoEventoCanal tipoEvento;
    private final CanalOrigen canalOrigen;
    private final String identificadorExterno;
    private final ResultadoEventoCanal resultado;
    private final LocalDateTime fecha;

    private EventoCanal(String id, TipoEventoCanal tipoEvento, CanalOrigen canalOrigen, String identificadorExterno,
                        ResultadoEventoCanal resultado, LocalDateTime fecha) {
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.canalOrigen = canalOrigen;
        this.identificadorExterno = identificadorExterno;
        this.resultado = resultado;
        this.fecha = fecha;
    }

    /** Registra en la bitácora un evento recibido de un canal y su resultado. */
    public static EventoCanal auditar(String id, TipoEventoCanal tipoEvento, CanalOrigen canalOrigen,
                                      String identificadorExterno, ResultadoEventoCanal resultado,
                                      LocalDateTime fecha) {
        textoRequerido(id, "El id del evento es obligatorio");
        requerido(tipoEvento, "El tipo de evento es obligatorio");
        requerido(canalOrigen, "El canal de origen es obligatorio");
        requerido(resultado, "El resultado del evento es obligatorio");
        requerido(fecha, "La fecha del evento es obligatoria");
        boolean requiereIdentificador = tipoEvento.requiereIdentificadorExterno();
        if (requiereIdentificador && (identificadorExterno == null || identificadorExterno.isBlank())) {
            throw new ReglaDominioException("RN-19: los eventos de creación y cancelación llevan identificador externo");
        }
        String idExterno = identificadorExterno == null ? null : identificadorExterno.trim();
        return new EventoCanal(id, tipoEvento, canalOrigen, idExterno, resultado, fecha);
    }

    public static EventoCanal reconstruir(String id, TipoEventoCanal tipoEvento, CanalOrigen canalOrigen,
                                          String identificadorExterno, ResultadoEventoCanal resultado,
                                          LocalDateTime fecha) {
        return new EventoCanal(id, tipoEvento, canalOrigen, identificadorExterno, resultado, fecha);
    }

    /**
     * RN-19: un mensaje nuevo es duplicado de este evento si trae el mismo canal, identificador externo
     * y tipo, y este evento ya fue aceptado. Así una cancelación posterior de la misma reserva no se
     * confunde con una creación repetida.
     */
    public boolean esDuplicado(CanalOrigen canal, String idExterno, TipoEventoCanal tipo) {
        return resultado == ResultadoEventoCanal.ACEPTADO
                && canalOrigen == canal
                && tipoEvento == tipo
                && identificadorExterno != null
                && identificadorExterno.equals(idExterno);
    }

    public String getId() { return id; }
    public TipoEventoCanal getTipoEvento() { return tipoEvento; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public ResultadoEventoCanal getResultado() { return resultado; }
    public LocalDateTime getFecha() { return fecha; }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }

    private static void textoRequerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new ReglaDominioException(mensaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoCanal otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
