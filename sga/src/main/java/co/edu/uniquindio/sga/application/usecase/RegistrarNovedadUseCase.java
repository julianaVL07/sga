package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * CU-17. POST /api/apartamentos/{id}/novedades
 * Registra una novedad (daño, faltante o situación) sobre un apartamento con fecha, autor, descripción y
 * gravedad (7.6). La gravedad la elige quien reporta; la sugerencia del modelo de IA (L-21) es opcional.
 * Actores: personal de servicio y recepcionista.
 */
public class RegistrarNovedadUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final NovedadRepository novedadRepository;
    private final Clock reloj;

    public RegistrarNovedadUseCase(ApartamentoRepository apartamentoRepository, NovedadRepository novedadRepository,
                                   Clock reloj) {
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.novedadRepository = Objects.requireNonNull(novedadRepository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Novedad ejecutar(IdentificacionApartamento apartamento, String autor, String descripcion,
                            GravedadNovedad gravedad) {
        if (apartamento == null || !apartamentoRepository.existe(apartamento)) {
            throw new ReglaDominioException("No existe el apartamento " + (apartamento == null ? "" : apartamento.codigo()));
        }
        Novedad novedad = Novedad.registrar("NOV-" + UUID.randomUUID(), apartamento, autor, descripcion, gravedad,
                LocalDate.now(reloj));
        return novedadRepository.guardar(novedad);
    }
}
