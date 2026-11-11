package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalTime;
import java.util.List;

/**
 * CU-28. El alojamiento tal como lo ve el administrador: datos, ubicación, horario, normas, servicios adicionales y
 * parámetros vigentes (sección 6, solo lectura: se configuran en application.properties). Dinero viaja como pesos
 * enteros.
 */
public record AlojamientoResponse(
        String id,
        String nombre,
        String descripcion,
        String ciudad,
        String direccion,
        double latitud,
        double longitud,
        LocalTime horaEntrada,
        LocalTime horaSalida,
        String normaConvivencia,
        List<ServicioAdicionalResponse> serviciosAdicionales,
        ParametrosResponse parametros
) {

    /** L-17: servicio que ofrece el alojamiento; valor es 0 si no genera cargo. */
    public record ServicioAdicionalResponse(
            String id,
            String nombre,
            String descripcion,
            boolean generaCargo,
            long valor,
            boolean activo
    ) {
    }

    /**
     * Parámetros que rigen hoy (L-09, L-11, L-13 a L-16, RP-01, RP-02, RP-03). Un null indica que no está configurado.
     * depositoGarantia es el mismo depósito (RP-02) que se asienta en el folio de toda reserva nueva.
     */
    public record ParametrosResponse(
            Integer umbralEdadFacturable,
            Integer tiempoPreparacionHoras,
            Integer plazoConfirmacionHoras,
            LocalTime horaLimiteNoShow,
            LocalTime horaInicioLlegadaNocturna,
            Integer estanciaMinimaNoches,
            boolean estanciaMinimaActiva,
            boolean exigeAnticipo,
            int porcentajeAnticipo,
            boolean aceptaMascotas,
            long cargoAseoMascota,
            int maximoMascotas,
            int pesoMaximoMascotaKg,
            long depositoGarantia
    ) {
    }
}
