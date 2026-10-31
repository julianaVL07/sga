package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.RegistrarBloqueoService;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinima;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos iniciales del alojamiento Coral (Anexo A). H2 en memoria arranca vacía en cada ejecución y
 * sin alojamiento, temporadas, tarifas y servicios ningún caso de uso funciona (F-13).
 * La carga usa las fábricas del dominio, así que pasa por las mismas reglas (F-05, 7.4, L-03, L-04).
 * Los parámetros de A.4 y los valores de RP-02/RP-03 se leen de application.properties (L-13: configuración,
 * nunca constante). El catálogo (identidad, temporadas, tarifas, inventario y demás servicios) es dato inicial.
 * Las temporadas se registran con la fecha final excluida (nota de A.3).
 *
 * Datos de prueba (12.3): APT-202 queda FUERA_DE_SERVICIO con un bloqueo de hoy+3 a hoy+15 (3.3: el estado no
 * retira la venta futura, el bloqueo sí) y APT-301 tiene un bloqueo vigente de hoy+20 a hoy+25. No se tocan
 * APT-101, APT-102 ni APT-201, que son los de las reservas de prueba. Corre con @Order(1), antes que cualquier
 * otro cargador de datos.
 */
@Configuration
public class DatosInicialesCoralConfig {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesCoralConfig.class);

    private static final String BASE = "T-BASE";
    private static final String MEDIA = "T-MEDIA";
    private static final String ALTA = "T-ALTA";

    @Bean
    @Order(1)
    public CommandLineRunner cargarDatosCoral(
            AlojamientoRepository alojamientoRepository,
            ApartamentoRepository apartamentoRepository,
            RegistrarBloqueoService registrarBloqueoService,
            Clock reloj,
            @Value("${sga.alojamiento.umbral-edad-facturable}") int umbralEdad,
            @Value("${sga.alojamiento.hora-entrada}") LocalTime horaEntrada,
            @Value("${sga.alojamiento.hora-salida}") LocalTime horaSalida,
            @Value("${sga.alojamiento.tiempo-preparacion-horas}") int horasPreparacion,
            @Value("${sga.alojamiento.plazo-confirmacion-horas}") int horasConfirmacion,
            @Value("${sga.alojamiento.hora-limite-no-show}") LocalTime horaLimiteNoShow,
            @Value("${sga.alojamiento.hora-inicio-llegada-nocturna}") LocalTime horaLlegadaNocturna,
            @Value("${sga.alojamiento.estancia-minima-noches}") int nochesEstanciaMinima,
            @Value("${sga.alojamiento.estancia-minima-activa}") boolean estanciaMinimaActiva,
            @Value("${sga.alojamiento.exige-anticipo}") boolean exigeAnticipo,
            @Value("${sga.alojamiento.porcentaje-anticipo}") int porcentajeAnticipo,
            @Value("${sga.alojamiento.mascotas.aceptadas}") boolean aceptaMascotas,
            @Value("${sga.alojamiento.mascotas.cargo-aseo}") long cargoAseoMascota,
            @Value("${sga.alojamiento.mascotas.maximo}") int maximoMascotas,
            @Value("${sga.alojamiento.mascotas.peso-maximo-kg}") int pesoMaximoMascotaKg,
            @Value("${sga.alojamiento.id-servicio-recepcion-nocturna}") String idRecepcionNocturna,
            @Value("${sga.alojamiento.valor-recepcion-nocturna}") long valorRecepcionNocturna) {
        return args -> {
            if (alojamientoRepository.obtener().isPresent()) {
                return;                                          // no duplicar si ya hay datos
            }

            // A.1 Identidad. La coordenada es la de Bocagrande (aproximada: el Anexo A no la trae).
            Alojamiento coral = Alojamiento.crear("ALO-CORAL", "Coral",
                    "Conjunto residencial exclusivo de apartamentos independientes de alta gama, con balcones "
                            + "panorámicos frente al mar, hidromasaje privado y acabados en piedra coralina.",
                    "Cartagena de Indias",
                    "Sector Bocagrande, Avenida del Malecón, frente a las playas de Castillo Grande",
                    new Coordenada(10.3975, -75.5590), horaEntrada, horaSalida,
                    "Silencio estricto en balcones entre las 23:00 y las 07:00; prohibido fumar dentro de los "
                            + "apartamentos; uso respetuoso de las áreas comunes no vendibles.");

            // A.4 Parámetros y políticas. RP-01: noches mínimas si incluye viernes o sábado en Media o Alta.
            coral.configurarParametros(new UmbralEdadFacturable(umbralEdad), new TiempoPreparacion(horasPreparacion),
                    new PlazoConfirmacion(horasConfirmacion), horaLimiteNoShow, horaLlegadaNocturna,
                    new EstanciaMinima(nochesEstanciaMinima, estanciaMinimaActiva), exigeAnticipo, porcentajeAnticipo);
            // L-16: política de mascotas y cargo único de aseo por estancia
            coral.definirPoliticaMascotas(aceptaMascotas
                    ? new CargoAseoMascota(true, Dinero.de(cargoAseoMascota), maximoMascotas, pesoMaximoMascotaKg)
                    : CargoAseoMascota.noSeAceptanMascotas());

            // A.3 Temporadas
            coral.agregarTemporada(Temporada.crearBase(BASE, "Base"));
            coral.agregarTemporada(Temporada.crear(MEDIA, "Media", TipoTemporada.MEDIA, List.of(
                    rango(2026, 1, 21, 2026, 2, 1),          // regreso de vacaciones
                    rango(2026, 3, 20, 2026, 3, 24),         // puente de San José
                    rango(2026, 6, 1, 2026, 6, 15),          // antesala de vacaciones
                    rango(2026, 7, 21, 2026, 8, 1),          // cierre de vacaciones
                    rango(2026, 10, 30, 2026, 11, 3),        // puente de Todos los Santos
                    rango(2026, 11, 6, 2026, 11, 17),        // Fiestas de Independencia de Cartagena
                    rango(2026, 12, 1, 2026, 12, 10))));     // antesala de fin de año
            coral.agregarTemporada(Temporada.crear(ALTA, "Alta", TipoTemporada.ALTA, List.of(
                    rango(2026, 1, 1, 2026, 1, 21),          // cierre de la temporada de fin de año 2025
                    rango(2026, 3, 27, 2026, 4, 6),          // Semana Santa
                    rango(2026, 6, 15, 2026, 7, 21),         // vacaciones de mitad de año
                    rango(2026, 10, 5, 2026, 10, 13),        // receso escolar
                    rango(2026, 12, 10, 2027, 1, 21))));     // fin de año

            // A.5 Servicios adicionales (L-17). RP-03: la recepción nocturna genera el cargo configurado.
            coral.agregarServicioAdicional(ServicioAdicional.crearConCargo(idRecepcionNocturna, "Recepción nocturna",
                    "Recepción para llegadas desde las " + horaLlegadaNocturna + " (RP-03)",
                    Dinero.de(valorRecepcionNocturna)));
            coral.agregarServicioAdicional(ServicioAdicional.crearConCargo("SRV-PASEO-BAHIA",
                    "Paseo privado por la bahía", "Recorrido privado por la bahía de Cartagena", Dinero.de(420_000)));
            coral.agregarServicioAdicional(ServicioAdicional.crearConCargo("SRV-CENA-BALCON",
                    "Cena en balcón por cocinero privado", "Cena para 2 personas en el balcón", Dinero.de(260_000)));

            // A.2 Inventario y tarifas de A.3 (Base, Media, Alta por ocupante facturable)
            List<Apartamento> inventario = List.of(
                    apartamento("APT-101", "Coral Luminoso", "Pensado para parejas o viajes de descanso individual.",
                            1, 2, List.of("Cama extragrande con lencería de algodón egipcio",
                                    "Baño privado en mármol con doble ducha", "Cocina integral abierta",
                                    "Balcón con tina de hidromasaje privada", "Vista frontal al mar Caribe"),
                            310_000, 430_000, 620_000),
                    apartamento("APT-102", "Arrecife Rosado", "Orientado a parejas jóvenes o profesionales.",
                            1, 2, List.of("Cama extragrande", "Baño de lujo", "Cocina equipada con barra de cuarzo",
                                    "Cava refrigerada de vinos", "Terraza privada con cama balinesa y vista a la bahía"),
                            330_000, 460_000, 660_000),
                    apartamento("APT-201", "Marea Dorada", "Diseñado para familias o pequeños grupos.",
                            2, 4, List.of("Dormitorio principal con cama extragrande",
                                    "Dormitorio auxiliar con dos camas semidobles", "2 baños completos",
                                    "Salón comedor", "Cocina completa", "Terraza con hidromasaje exterior"),
                            270_000, 380_000, 560_000),
                    apartamento("APT-202", "Bahía Cristal", "Distribución funcional para grupos familiares.",
                            2, 4, List.of("Dormitorio principal extragrande", "Dormitorio con dos camas individuales",
                                    "2 baños privados", "Cocina integral", "Comedor en balcón panorámico",
                                    "Hamaca artesanal de San Jacinto"),
                            280_000, 390_000, 580_000),
                    apartamento("APT-301", "Gran Coral Imperial", "La unidad insignia de máxima capacidad.",
                            3, 6, List.of("Dormitorio principal con vestier y baño privado", "2 dormitorios dobles",
                                    "3 baños completos", "Sala", "Cocina amplia",
                                    "Terraza privada con piscina de inmersión y vista a las murallas y al mar"),
                            250_000, 350_000, 520_000));

            for (Apartamento apartamento : inventario) {
                apartamento.activar(coral.getTemporadas());          // 7.4: exige tarifa en todas las temporadas
                coral.agregarApartamento(apartamento);
                apartamentoRepository.guardar(apartamento);
            }
            alojamientoRepository.guardar(coral);

            // 12.3: APT-202 fuera de servicio por decisión administrativa (7.6) y bloqueado para la venta (3.3)
            LocalDate hoy = LocalDate.now(reloj);
            IdentificacionApartamento apt202 = new IdentificacionApartamento("APT-202");
            Apartamento fueraDeServicio = apartamentoRepository.buscarPorIdentificacion(apt202).orElseThrow();
            fueraDeServicio.ponerFueraDeServicio();
            apartamentoRepository.guardar(fueraDeServicio);
            registrarBloqueoService.registrarBloqueo(apt202, new RangoFechas(hoy.plusDays(3), hoy.plusDays(15)),
                    "Fuera de servicio: reparación de la cocina integral");

            // 12.3: bloqueo vigente sobre APT-301 (RN-07)
            registrarBloqueoService.registrarBloqueo(new IdentificacionApartamento("APT-301"),
                    new RangoFechas(hoy.plusDays(20), hoy.plusDays(25)), "Mantenimiento de la piscina de inmersión");

            log.info("Datos iniciales del alojamiento Coral cargados: {} apartamentos", inventario.size());
        };
    }

    private static Apartamento apartamento(String id, String nombre, String descripcion, int dormitorios,
                                           int capacidad, List<String> dotacion,
                                           long tarifaBase, long tarifaMedia, long tarifaAlta) {
        Apartamento apartamento = Apartamento.crear(new IdentificacionApartamento(id), nombre, descripcion,
                dormitorios, new Capacidad(capacidad), new Dotacion(dotacion),
                List.of(new Imagen("https://coral.co/img/" + id.toLowerCase() + ".jpg", true)));
        apartamento.definirTarifa(new Tarifa(BASE, Dinero.de(tarifaBase)));
        apartamento.definirTarifa(new Tarifa(MEDIA, Dinero.de(tarifaMedia)));
        apartamento.definirTarifa(new Tarifa(ALTA, Dinero.de(tarifaAlta)));
        return apartamento;
    }

    private static RangoFechas rango(int anioInicio, int mesInicio, int diaInicio, int anioFin, int mesFin, int diaFin) {
        return new RangoFechas(LocalDate.of(anioInicio, mesInicio, diaInicio), LocalDate.of(anioFin, mesFin, diaFin));
    }
}