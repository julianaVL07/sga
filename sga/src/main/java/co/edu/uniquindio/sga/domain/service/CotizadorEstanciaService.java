package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DetalleNoche;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Calcula el valor de una estancia noche por noche (3.5, 7.4).
 *
 * Reglas: RN-05 (cada noche se cobra con la tarifa de su temporada), RN-06 (solo cuentan los ocupantes
 * facturables según el umbral de edad).
 */
public class CotizadorEstanciaService {

    private final ApartamentoRepository apartamentoRepository;
    private final AlojamientoRepository alojamientoRepository;

    public CotizadorEstanciaService(ApartamentoRepository apartamentoRepository,
                                    AlojamientoRepository alojamientoRepository) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
    }

    /** @param umbral si es null se usa el umbral configurado en el alojamiento (L-09). */
    public Cotizacion cotizar(IdentificacionApartamento aptoId, Estancia estancia, List<Ocupante> grupo,
                              UmbralEdadFacturable umbral) {
        requerido(aptoId, "El apartamento es obligatorio");
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(aptoId)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + aptoId.codigo()));
        return cotizar(apartamento, estancia, grupo, umbral);
    }

    public Cotizacion cotizar(Apartamento apartamento, Estancia estancia, List<Ocupante> grupo,
                              UmbralEdadFacturable umbral) {
        requerido(apartamento, "El apartamento es obligatorio");
        requerido(estancia, "La estancia es obligatoria");
        if (grupo == null || grupo.isEmpty()) {
            throw new ReglaDominioException("RN-06: El grupo debe tener al menos un ocupante");
        }
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
        UmbralEdadFacturable umbralAplicado = umbral != null ? umbral : alojamiento.getUmbralEdadFacturable();
        requerido(umbralAplicado, "L-09: El umbral de edad facturable no está configurado");

        int facturables = (int) grupo.stream().filter(o -> o.esFacturableEn(estancia, umbralAplicado)).count();

        List<DetalleNoche> detalles = estancia.fechasDeNoches().stream()
                .map(noche -> detallar(alojamiento, apartamento, noche, facturables))
                .toList();
        return Cotizacion.desde(detalles);
    }

    private static DetalleNoche detallar(Alojamiento alojamiento, Apartamento apartamento, LocalDate noche,
                                         int facturables) {
        Temporada temporada = alojamiento.obtenerTemporadaPara(noche);
        return DetalleNoche.calcular(noche, temporada.getNombre(), apartamento.tarifaPara(temporada), facturables);
    }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }
}
