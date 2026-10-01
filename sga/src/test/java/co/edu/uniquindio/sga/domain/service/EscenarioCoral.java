package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinima;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.domain.valueobject.Titular;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.AlojamientoRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ApartamentoRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.BloqueoRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.FolioRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ReservaRepositoryInMemory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos del Anexo A que comparten las pruebas de servicios: alojamiento Coral con temporada base
 * ($310.000 por ocupante) y temporada alta del 5 al 14 de octubre de 2026 ($620.000), apartamento APT-101
 * de capacidad 3 y parámetros de la sección 6 (umbral 6 años, preparación 3 h, confirmación 12 h,
 * no-show 22:00, anticipo 50 %). Los repositorios son los InMemory de infraestructura.
 */
final class EscenarioCoral {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 9, 0);
    static final IdentificacionApartamento APT_101 = new IdentificacionApartamento("APT-101");

    final AlojamientoRepositoryInMemory alojamientos = new AlojamientoRepositoryInMemory();
    final ApartamentoRepositoryInMemory apartamentos = new ApartamentoRepositoryInMemory();
    final ReservaRepositoryInMemory reservas = new ReservaRepositoryInMemory();
    final FolioRepositoryInMemory folios = new FolioRepositoryInMemory();
    final BloqueoRepositoryInMemory bloqueos = new BloqueoRepositoryInMemory();

    final Alojamiento alojamiento;
    final Apartamento apartamento;
    final DocumentoIdentidad documentoTitular = new DocumentoIdentidad("CC", "1094000000");
    final Titular titular = new Titular("Ana Gómez", documentoTitular, "ana@correo.co", "3001234567");
    final Ocupante ana = new Ocupante("Ana Gómez", documentoTitular, LocalDate.of(1990, 5, 20));
    final PoliticaCancelacion politica = new PoliticaCancelacion(1, LocalDate.of(2026, 1, 1),
            "Retención del 50 % del total", List.of(new TramoCancelacion(16, 0), new TramoCancelacion(7, 25),
            new TramoCancelacion(0, 50)));

    EscenarioCoral() {
        alojamiento = Alojamiento.crear("ALO-1", "Coral", "Apartamentos turísticos", "Cartagena",
                "Bocagrande", new Coordenada(10.40, -75.55), LocalTime.of(15, 0), LocalTime.of(11, 0),
                "Políticas generales del alojamiento");
        alojamiento.configurarParametros(new UmbralEdadFacturable(6), new TiempoPreparacion(3),
                new PlazoConfirmacion(12), LocalTime.of(22, 0), LocalTime.of(20, 0), new EstanciaMinima(2, true),
                true, 50);
        alojamiento.agregarTemporada(Temporada.crearBase("T-BASE", "Base"));
        alojamiento.agregarTemporada(Temporada.crear("T-ALTA", "Alta", TipoTemporada.ALTA,
                List.of(new RangoFechas(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 15)))));
        alojamientos.guardar(alojamiento);

        apartamento = Apartamento.crear(APT_101, "Apartamento 101", "Vista al mar", 1, new Capacidad(3),
                new Dotacion(List.of("Aire acondicionado")), List.of(new Imagen("https://coral.co/apt-101.jpg", true)));
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(310_000)));
        apartamento.definirTarifa(new Tarifa("T-ALTA", Dinero.de(620_000)));
        apartamento.activar(alojamiento.getTemporadas());
        apartamentos.guardar(apartamento);
    }

    /** Reserva PENDIENTE del titular solo, con la cotización calculada por el cotizador. */
    Reserva reservaPendiente(int secuencia, Estancia estancia, LocalTime horaEstimadaLlegada) {
        CotizadorEstanciaService cotizador = new CotizadorEstanciaService(apartamentos, alojamientos);
        Reserva reserva = Reserva.crear(CodigoReserva.generar(2026, secuencia), apartamento, estancia, titular,
                List.of(ana), CanalOrigen.PORTAL, null, horaEstimadaLlegada,
                cotizador.cotizar(apartamento, estancia, List.of(ana), null), politica,
                alojamiento.getUmbralEdadFacturable(), AHORA);
        reservas.guardar(reserva);
        return reserva;
    }
}
