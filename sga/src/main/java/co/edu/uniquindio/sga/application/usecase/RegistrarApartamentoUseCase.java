package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.service.AdministracionApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-22. POST /api/apartamentos
 * Registra un apartamento con sus datos, dotación, imágenes (1 a 10, una principal) y, opcionalmente, tarifas por
 * temporada. Nace INACTIVO (7.4): se activa con CU-25 cuando tenga tarifa en todas las temporadas.
 * La identificación única (L-04), el máximo de apartamentos (L-03) y las reglas del agregado las valida el dominio.
 * Actor: administrador.
 */
@Service
public class RegistrarApartamentoUseCase {

    private final AdministracionApartamentoService administracionService;
    private final Clock reloj;

    public RegistrarApartamentoUseCase(AdministracionApartamentoService administracionService, Clock reloj) {
        this.administracionService = Objects.requireNonNull(administracionService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion, String nombre, String descripcion,
                                int dormitorios, Capacidad capacidad, Dotacion dotacion, List<Imagen> imagenes,
                                List<Tarifa> tarifas) {
        Apartamento nuevo = Apartamento.crear(identificacion, nombre, descripcion, dormitorios, capacidad,
                dotacion, imagenes);
        return administracionService.registrar(nuevo, tarifas, LocalDate.now(reloj));
    }
}
