package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService.DatosModificacion;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * CU-11. PUT /api/reservas/{codigo}
 * Cambia fechas, ocupantes o apartamento de una reserva no iniciada. Revalida las seis condiciones de creación
 * con las tarifas vigentes y registra la diferencia como ajuste en el folio (RN-14, RN-22).
 * Incluye CU-01 y CU-02. Actores: recepcionista o administrador.
 *
 * Un dato en null se conserva: para cambiar solo las fechas, apartamento y ocupantes van en null.
 */
public class ModificarReservaUseCase {

    private final ReservaRepository reservaRepository;
    private final ModificacionReservaService modificacionService;
    private final Clock reloj;

    public ModificarReservaUseCase(ReservaRepository reservaRepository, ModificacionReservaService modificacionService,
                                   Clock reloj) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.modificacionService = Objects.requireNonNull(modificacionService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** @return la diferencia asentada en el folio (negativa si baja el valor). */
    public Dinero ejecutar(CodigoReserva codigo, IdentificacionApartamento nuevoApartamento, LocalDate nuevaFechaEntrada,
                           LocalDate nuevaFechaSalida, List<Ocupante> nuevosOcupantes) {
        verificarQueExiste(codigo);
        Estancia nuevaEstancia = null;
        if (nuevaFechaEntrada != null || nuevaFechaSalida != null) {
            Estancia actual = reservaRepository.buscarPorCodigo(codigo).orElseThrow().getEstancia();
            nuevaEstancia = new Estancia(nuevaFechaEntrada != null ? nuevaFechaEntrada : actual.fechaEntrada(),
                    nuevaFechaSalida != null ? nuevaFechaSalida : actual.fechaSalida());
        }
        return modificacionService.modificar(codigo,
                new DatosModificacion(nuevoApartamento, nuevaEstancia, nuevosOcupantes), LocalDateTime.now(reloj));
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
