package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.service.CanalVentaService;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoAutenticacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Autenticación de un canal externo con su credencial (10.3.1), antes de cualquiera de las tres operaciones del
 * canal. La usa el filtro de seguridad del canal; si el intento se rechaza, queda en la bitácora (7.8).
 * Actor: canal externo.
 */
@Service
public class AutenticarCanalUseCase {

    private final CanalVentaService canalVentaService;
    private final Clock reloj;

    public AutenticarCanalUseCase(CanalVentaService canalVentaService, Clock reloj) {
        this.canalVentaService = Objects.requireNonNull(canalVentaService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Transactional
    public ResultadoAutenticacionCanal autenticar(String nombreCanal, String credencial, TipoEventoCanal operacion,
                                                  String identificadorExterno) {
        return canalVentaService.autenticar(nombreCanal, credencial, operacion, identificadorExterno,
                LocalDateTime.now(reloj));
    }
}
