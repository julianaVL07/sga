package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.List;

/**
 * Política de cancelación vigente del Apartahotel Coral (Anexo A, A.4) leída de application.properties
 * (sga.politica-cancelacion.*). L-13: parámetro del alojamiento, nunca una constante en el código.
 * Solo transporta los valores: las reglas (L-10, RN-13) las valida PoliticaCancelacion al construirse.
 *
 * @param version            versión de la política que se congela en cada reserva nueva (RN-22).
 * @param vigenteDesde       fecha desde la que rige la versión (F-07).
 * @param consecuenciaNoShow qué ocurre ante un no-show (7.4).
 * @param tramos             retención según los días de antelación (L-10).
 */
@ConfigurationProperties(prefix = "sga.politica-cancelacion")
public record PoliticaCancelacionProperties(int version, LocalDate vigenteDesde, String consecuenciaNoShow,
                                            List<Tramo> tramos) {

    /** Un tramo: desde cuántos días de antelación se retiene qué porcentaje del valor total (L-10, RN-13). */
    public record Tramo(int diasAntelacion, int porcentajeRetencion) {
    }

    /** Construye el objeto de valor del dominio; si los valores violan L-10 el arranque falla con ReglaDominioException. */
    public PoliticaCancelacion aPolitica() {
        List<TramoCancelacion> tramosDominio = tramos == null ? null : tramos.stream()
                .map(t -> new TramoCancelacion(t.diasAntelacion(), t.porcentajeRetencion()))
                .toList();
        return new PoliticaCancelacion(version, vigenteDesde, consecuenciaNoShow, tramosDominio);
    }
}
