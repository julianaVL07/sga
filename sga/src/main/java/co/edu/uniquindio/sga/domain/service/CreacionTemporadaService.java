package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CU-29 (7.4): crea una temporada y asigna su tarifa a los apartamentos en una sola operación.
 *
 * Regla 7.4: todo apartamento activo debe tener tarifa en todas las temporadas, así que una temporada nueva debe
 * traer la tarifa de cada apartamento activo; si falta alguna no se crea la temporada ni se asigna ninguna tarifa.
 * Que la temporada no se solape con otra y que no haya una segunda base lo valida Alojamiento.agregarTemporada;
 * que el valor sea mayor que cero, Tarifa. Las tarifas quedan en el histórico de cada apartamento (CU-24).
 */
public class CreacionTemporadaService {

    /** Tarifa por ocupante facturable y por noche de un apartamento en la temporada que se crea. */
    public record TarifaApartamento(IdentificacionApartamento apartamento, Dinero valorPorOcupante) {
    }

    private final AlojamientoRepository alojamientoRepository;
    private final ApartamentoRepository apartamentoRepository;

    public CreacionTemporadaService(AlojamientoRepository alojamientoRepository,
                                    ApartamentoRepository apartamentoRepository) {
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
    }

    /**
     * Valida todo antes de cambiar nada: la temporada (Temporada), las tarifas (7.4) y su lugar en el calendario
     * (Alojamiento: 7.4 sin solapes, F-05 una sola base). Solo entonces asigna las tarifas y guarda.
     *
     * @param rangos  rangos de fechas con la fecha final excluida; la temporada base no lleva rangos (F-05).
     * @param tarifas tarifa de cada apartamento activo; puede incluir apartamentos inactivos no retirados.
     * @param hoy     fecha desde la que rigen las tarifas en el histórico de cada apartamento (CU-24).
     */
    public Temporada crear(String id, String nombre, TipoTemporada tipo, List<RangoFechas> rangos,
                           List<TarifaApartamento> tarifas, LocalDate hoy) {
        if (hoy == null) throw new ReglaDominioException("La fecha actual es obligatoria");
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));

        Temporada temporada = construir(id, nombre, tipo, rangos == null ? List.of() : rangos);
        Map<Apartamento, Tarifa> tarifasPorApartamento = validarTarifas(temporada,
                tarifas == null ? List.of() : tarifas);
        alojamiento.agregarTemporada(temporada);

        tarifasPorApartamento.forEach((apartamento, tarifa) -> apartamento.definirTarifa(tarifa, hoy));
        alojamientoRepository.guardar(alojamiento);
        tarifasPorApartamento.keySet().forEach(apartamentoRepository::guardar);
        return temporada;
    }

    /** F-05: la base se crea sin rangos; si los trae, Temporada.agregarRango la rechaza. */
    private static Temporada construir(String id, String nombre, TipoTemporada tipo, List<RangoFechas> rangos) {
        if (tipo != TipoTemporada.BASE) {
            return Temporada.crear(id, nombre, tipo, rangos);
        }
        Temporada base = Temporada.crearBase(id, nombre);
        rangos.forEach(base::agregarRango);
        return base;
    }

    /**
     * 7.4: cada apartamento aparece una sola vez, existe y no está retirado de la venta, y ningún apartamento activo
     * queda sin tarifa en la temporada nueva. Devuelve cada apartamento con su tarifa, sin modificarlo todavía.
     */
    private Map<Apartamento, Tarifa> validarTarifas(Temporada temporada, List<TarifaApartamento> tarifas) {
        Map<Apartamento, Tarifa> porApartamento = new LinkedHashMap<>();
        for (TarifaApartamento tarifa : tarifas) {
            if (tarifa == null || tarifa.apartamento() == null) {
                throw new ReglaDominioException("7.4: Cada tarifa debe indicar el apartamento");
            }
            Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(tarifa.apartamento())
                    .filter(a -> !a.isEliminado())
                    .orElseThrow(() -> new ReglaDominioException("7.4: El apartamento "
                            + tarifa.apartamento().codigo() + " no existe o fue retirado de la venta"));
            if (porApartamento.containsKey(apartamento)) {
                throw new ReglaDominioException("7.4: El apartamento " + apartamento.getIdentificacion().codigo()
                        + " aparece más de una vez");
            }
            porApartamento.put(apartamento, new Tarifa(temporada.getId(), tarifa.valorPorOcupante()));
        }

        List<String> sinTarifa = new ArrayList<>();
        for (Apartamento activo : apartamentoRepository.listarVigentesParaVenta()) {
            if (!porApartamento.containsKey(activo)) {
                sinTarifa.add(activo.getIdentificacion().codigo());
            }
        }
        if (!sinTarifa.isEmpty()) {
            throw new ReglaDominioException("7.4: Todo apartamento activo debe tener tarifa en todas las temporadas; "
                    + "falta la tarifa de la temporada " + temporada.getNombre() + " para " + String.join(", ", sinTarifa));
        }
        return porApartamento;
    }
}
