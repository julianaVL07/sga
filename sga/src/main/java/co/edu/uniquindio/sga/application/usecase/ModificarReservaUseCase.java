package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService.DatosModificacion;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Mascotas;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * CU-11. PUT /api/reservas/{codigo}
 * Cambia fechas, ocupantes, apartamento o mascotas de una reserva no iniciada. Revalida las seis condiciones de creación
 * con las tarifas vigentes y registra la diferencia como ajuste en el folio (RN-14, RN-22).
 * Incluye CU-01 y CU-02. Actores: recepcionista o administrador.
 *
 * Un dato en null se conserva: para cambiar solo las fechas, apartamento y ocupantes van en null. Las mascotas
 * (L-16) siguen la misma idea: si solo llega la cantidad o el peso, el otro dato se toma de la reserva.
 */

@Service
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
    @Transactional
    public Dinero ejecutar(CodigoReserva codigo, IdentificacionApartamento nuevoApartamento, LocalDate nuevaFechaEntrada,
                           LocalDate nuevaFechaSalida, List<Ocupante> nuevosOcupantes,
                           Integer nuevaCantidadMascotas, Integer nuevoPesoMayorMascotaKg) {
        verificarQueExiste(codigo);
        Reserva actual = reservaRepository.buscarPorCodigo(codigo).orElseThrow();
        Estancia nuevaEstancia = null;
        if (nuevaFechaEntrada != null || nuevaFechaSalida != null) {
            Estancia estancia = actual.getEstancia();
            nuevaEstancia = new Estancia(nuevaFechaEntrada != null ? nuevaFechaEntrada : estancia.fechaEntrada(),
                    nuevaFechaSalida != null ? nuevaFechaSalida : estancia.fechaSalida());
        }
        Mascotas nuevasMascotas = null;
        if (nuevaCantidadMascotas != null || nuevoPesoMayorMascotaKg != null) {
            Mascotas mascotas = actual.getMascotas();
            int cantidad = nuevaCantidadMascotas != null ? nuevaCantidadMascotas : mascotas.cantidad();
            // Quitar las mascotas (cantidad 0) sin enviar el peso deja también el peso en 0.
            int peso = nuevoPesoMayorMascotaKg != null ? nuevoPesoMayorMascotaKg
                    : (cantidad == 0 ? 0 : mascotas.pesoMayorKg());
            nuevasMascotas = new Mascotas(cantidad, peso);
        }
        return modificacionService.modificar(codigo,
                new DatosModificacion(nuevoApartamento, nuevaEstancia, nuevosOcupantes, nuevasMascotas),
                LocalDateTime.now(reloj));
    }

    private void verificarQueExiste(CodigoReserva codigo) {
        if (codigo == null || reservaRepository.buscarPorCodigo(codigo).isEmpty()) {
            throw new ReservaNoEncontradaException(codigo);
        }
    }
}
