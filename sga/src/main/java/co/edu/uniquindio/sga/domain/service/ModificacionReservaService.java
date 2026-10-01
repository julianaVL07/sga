package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Cambia fechas, ocupantes o apartamento de una reserva que no ha iniciado (7.5).
 *
 * Reglas: RN-14 (se revalidan todas las condiciones de creación, sin contar la propia reserva, y la
 * diferencia de valor se asienta como ajuste en el folio), RN-22 (la política de cancelación congelada
 * no cambia; la cotización se reemplaza por una con las tarifas vigentes).
 */
public class ModificacionReservaService {

    /**
     * Cambios pedidos. Un campo en null significa "se conserva el valor actual".
     * (El catálogo lo nombra DatosModificacion; no está entre los objetos de valor, por eso vive aquí.)
     */
    public record DatosModificacion(IdentificacionApartamento nuevoApartamento, Estancia nuevaEstancia,
                                    List<Ocupante> nuevosOcupantes) {
        public DatosModificacion {
            if (nuevoApartamento == null && nuevaEstancia == null && nuevosOcupantes == null) {
                throw new ReglaDominioException("RN-14: La modificación debe cambiar al menos un dato");
            }
            nuevosOcupantes = nuevosOcupantes == null ? null : List.copyOf(nuevosOcupantes);
        }
    }

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final DisponibilidadApartamentoService disponibilidadService;
    private final CotizadorEstanciaService cotizadorService;

    public ModificacionReservaService(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                      ApartamentoRepository apartamentoRepository,
                                      AlojamientoRepository alojamientoRepository,
                                      DisponibilidadApartamentoService disponibilidadService,
                                      CotizadorEstanciaService cotizadorService) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.disponibilidadService = Objects.requireNonNull(disponibilidadService);
        this.cotizadorService = Objects.requireNonNull(cotizadorService);
    }

    /** @return la diferencia de valor asentada en el folio (positiva si el huésped debe más). */
    public Dinero modificar(CodigoReserva codigo, DatosModificacion cambios, LocalDateTime ahora) {
        if (cambios == null) throw new ReglaDominioException("RN-14: Los cambios son obligatorios");
        if (ahora == null) throw new ReglaDominioException("La fecha actual es obligatoria");
        Reserva reserva = buscarReserva(codigo);
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
        Folio folio = folioRepository.buscarPorReserva(reserva.getCodigo())
                .orElseThrow(() -> new ReglaDominioException("7.7: La reserva " + codigo.valor() + " no tiene folio"));

        IdentificacionApartamento aptoId = cambios.nuevoApartamento() != null
                ? cambios.nuevoApartamento() : reserva.getApartamento();
        Estancia estancia = cambios.nuevaEstancia() != null ? cambios.nuevaEstancia() : reserva.getEstancia();
        List<Ocupante> ocupantes = cambios.nuevosOcupantes() != null
                ? cambios.nuevosOcupantes() : reserva.getOcupantes();

        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(aptoId)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + aptoId.codigo()));

        // RN-14: condiciones 5 y 6 (y el resto de la disponibilidad) sin contar la propia reserva.
        disponibilidadService.validarDisponibilidad(apartamento, estancia, ocupantes.size(), reserva.getCodigo());
        Cotizacion nuevaCotizacion = cotizadorService.cotizar(apartamento, estancia, ocupantes,
                alojamiento.getUmbralEdadFacturable());

        Dinero diferencia = reserva.modificar(apartamento, estancia, ocupantes, nuevaCotizacion,
                alojamiento.getUmbralEdadFacturable(), ahora);
        folio.registrarAjustePorModificacion(diferencia,
                "Ajuste por modificación de la reserva " + codigo.valor(), ahora.toLocalDate());

        reservaRepository.guardar(reserva);
        folioRepository.guardar(folio);
        return diferencia;
    }

    private Reserva buscarReserva(CodigoReserva codigo) {
        if (codigo == null) throw new ReglaDominioException("El código de la reserva es obligatorio");
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaDominioException("No existe la reserva " + codigo.valor()));
    }
}
