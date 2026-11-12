package co.edu.uniquindio.sga.infrastructure.notificaciones;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Textos en español de los correos al titular (7.8). Los usan todos los adaptadores del puerto NotificadorTitular,
 * así el adaptador local guarda exactamente lo mismo que enviaría el SMTP.
 * Todo correo de una reserva lleva el código, el apartamento, las fechas y el valor.
 */
public class PlantillasCorreo {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_CO);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final String nombreAlojamiento;
    private final LocalTime horaEntrada;
    private final LocalTime horaSalida;

    /**
     * @param nombreAlojamiento firma de los correos, por ejemplo "Apartahotel Coral".
     * @param horaEntrada       desde qué hora se recibe a los huéspedes (para el recordatorio de llegada).
     * @param horaSalida        hasta qué hora se entrega el apartamento (para el recordatorio de salida).
     */
    public PlantillasCorreo(String nombreAlojamiento, LocalTime horaEntrada, LocalTime horaSalida) {
        this.nombreAlojamiento = Objects.requireNonNull(nombreAlojamiento);
        this.horaEntrada = Objects.requireNonNull(horaEntrada);
        this.horaSalida = Objects.requireNonNull(horaSalida);
    }

    public MensajeCorreo reservaCreada(Reserva reserva) {
        String cuerpo = saludo(reserva)
                + "Recibimos tu reserva en el " + nombreAlojamiento + ". Está PENDIENTE de confirmación: "
                + "se confirma cuando recepción registre el pago del anticipo.\n\n"
                + detalle(reserva)
                + despedida();
        return new MensajeCorreo("RESERVA_CREADA", destinatario(reserva),
                "Recibimos tu reserva " + reserva.getCodigo().valor(), cuerpo);
    }

    public MensajeCorreo reservaConfirmada(Reserva reserva) {
        String llegada = reserva.getHoraEstimadaLlegada() == null ? ""
                : "Hora estimada de llegada: " + reserva.getHoraEstimadaLlegada().format(HORA) + "\n";
        String cuerpo = saludo(reserva)
                + "¡Tu reserva está CONFIRMADA! Te esperamos.\n\n"
                + detalle(reserva)
                + llegada
                + "\nLa entrada es desde las " + horaEntrada.format(HORA) + " y la salida hasta las "
                + horaSalida.format(HORA) + ".\n"
                + despedida();
        return new MensajeCorreo("RESERVA_CONFIRMADA", destinatario(reserva),
                "Tu reserva " + reserva.getCodigo().valor() + " está confirmada", cuerpo);
    }

    public MensajeCorreo reservaCancelada(Reserva reserva, Dinero retencion) {
        Dinero retenido = retencion == null ? Dinero.cero() : retencion;
        String motivo = reserva.getMotivoCancelacion() == null || reserva.getMotivoCancelacion().isBlank() ? ""
                : "Motivo: " + reserva.getMotivoCancelacion() + "\n";
        String retencionTexto = retenido.esCero()
                ? "Según la política de cancelación de tu reserva no se retiene ningún valor."
                : "Según la política de cancelación de tu reserva se retienen " + pesos(retenido) + ".";
        String cuerpo = saludo(reserva)
                + "Tu reserva fue CANCELADA y sus noches quedaron libres.\n\n"
                + detalle(reserva)
                + motivo
                + "Retención: " + pesos(retenido) + "\n\n"
                + retencionTexto + " Si pagaste más que eso, el saldo a tu favor se devuelve por recepción.\n"
                + despedida();
        return new MensajeCorreo("RESERVA_CANCELADA", destinatario(reserva),
                "Tu reserva " + reserva.getCodigo().valor() + " fue cancelada", cuerpo);
    }

    public MensajeCorreo recordatorio(Reserva reserva, MotivoRecordatorio motivo) {
        boolean llegada = motivo == MotivoRecordatorio.LLEGADA;
        LocalDate fecha = llegada ? reserva.getEstancia().fechaEntrada() : reserva.getEstancia().fechaSalida();
        String aviso = llegada
                ? "Te recordamos que mañana, " + fecha(fecha) + ", es tu llegada. La entrada es desde las "
                + horaEntrada.format(HORA) + ". Si cambia tu hora de llegada, avísale a recepción."
                : "Te recordamos que mañana, " + fecha(fecha) + ", termina tu estancia. La salida es hasta las "
                + horaSalida.format(HORA) + " y el folio debe quedar a paz y salvo antes de entregar el apartamento.";
        String cuerpo = saludo(reserva)
                + aviso + "\n\n"
                + detalle(reserva)
                + despedida();
        return new MensajeCorreo(llegada ? "RECORDATORIO_LLEGADA" : "RECORDATORIO_SALIDA", destinatario(reserva),
                (llegada ? "Mañana es tu llegada" : "Mañana termina tu estancia") + " · "
                        + reserva.getCodigo().valor(), cuerpo);
    }

    public MensajeCorreo codigoRecuperacion(String correo, String nombre, String codigo, long minutosVigencia) {
        String cuerpo = "Hola" + (nombre == null || nombre.isBlank() ? "" : ", " + nombre) + ":\n\n"
                + "Tu código para restablecer la contraseña es:\n\n"
                + "    " + codigo + "\n\n"
                + "Vence en " + minutosVigencia + " minutos y sirve una sola vez.\n"
                + "Si no pediste cambiar tu contraseña, ignora este mensaje: tu cuenta sigue igual.\n"
                + despedida();
        return new MensajeCorreo("CODIGO_RECUPERACION", correo, "Código para restablecer tu contraseña", cuerpo);
    }

    // ---------- Partes comunes ----------

    private static String destinatario(Reserva reserva) {
        return reserva.getTitular() == null ? null : reserva.getTitular().email();
    }

    private static String saludo(Reserva reserva) {
        String nombre = reserva.getTitular() == null ? "" : ", " + reserva.getTitular().nombre();
        return "Hola" + nombre + ":\n\n";
    }

    /** Código, apartamento, fechas, noches y valor: lo que lleva todo correo de una reserva. */
    private static String detalle(Reserva reserva) {
        return "Código de reserva: " + reserva.getCodigo().valor() + "\n"
                + "Apartamento: " + reserva.getApartamento().codigo() + "\n"
                + "Llegada: " + fecha(reserva.getEstancia().fechaEntrada()) + "\n"
                + "Salida: " + fecha(reserva.getEstancia().fechaSalida()) + "\n"
                + "Noches: " + reserva.getEstancia().noches() + "\n"
                + "Valor de la estancia: " + pesos(reserva.valorTotal()) + "\n";
    }

    private String despedida() {
        return "\nGracias por elegirnos.\n" + nombreAlojamiento + "\n";
    }

    private static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    /** Pesos colombianos sin decimales: $1.250.000 (3.4). */
    static String pesos(Dinero dinero) {
        BigDecimal monto = dinero.monto();
        return (monto.signum() < 0 ? "-$" : "$") + String.format(ES_CO, "%,d", monto.abs().toBigInteger());
    }
}
