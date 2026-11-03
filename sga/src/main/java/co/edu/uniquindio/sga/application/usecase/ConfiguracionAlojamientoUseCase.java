package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Objects;

/**
 * CU-28. GET /api/alojamiento y PUT /api/alojamiento
 * Consulta el alojamiento (datos, ubicación, horario, normas, servicios adicionales y parámetros vigentes) y
 * actualiza sus datos descriptivos, su ubicación, sus normas y, si se indica, su horario (L-12). Los parámetros de la
 * sección 6 no se cambian por API: se configuran en application.properties. Las reglas las aplica el agregado
 * Alojamiento (409).
 * Actor: administrador.
 */
@Service
public class ConfiguracionAlojamientoUseCase {

    /** Datos que el administrador puede cambiar. Si las dos horas vienen en null se conserva el horario vigente. */
    public record DatosAlojamiento(String nombre, String descripcion, String ciudad, String direccion,
                                   Coordenada ubicacion, String normaConvivencia, LocalTime horaEntrada,
                                   LocalTime horaSalida) {
    }

    private final AlojamientoRepository alojamientoRepository;

    public ConfiguracionAlojamientoUseCase(AlojamientoRepository alojamientoRepository) {
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
    }

    @Transactional(readOnly = true)
    public Alojamiento consultar() {
        return obtenerAlojamiento();
    }

    @Transactional
    public Alojamiento actualizar(DatosAlojamiento datos) {
        if (datos == null) throw new ReglaDominioException("Los datos del alojamiento son obligatorios");
        Alojamiento alojamiento = obtenerAlojamiento();
        alojamiento.actualizarDatos(datos.nombre(), datos.descripcion(), datos.ciudad(), datos.direccion(),
                datos.ubicacion(), datos.normaConvivencia());
        if (datos.horaEntrada() != null || datos.horaSalida() != null) {
            alojamiento.definirHorario(datos.horaEntrada(), datos.horaSalida());
        }
        return alojamientoRepository.guardar(alojamiento);
    }

    private Alojamiento obtenerAlojamiento() {
        return alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
    }
}
