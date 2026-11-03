package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un tramo de la política de cancelación congelada en la reserva (RN-13, RN-22). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TramoCancelacionEmbedable {

    private int antelacionMinimaDias;

    private int porcentajeRetencion;
}
